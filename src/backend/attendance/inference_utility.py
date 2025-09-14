"""
This code infers the image and generates the output of the attendance. It uses a
two-stage pipeline: a YOLOv8 model for face detection and a custom ResNet-based
model for face recognition.
"""

import torch
import torch.nn.functional as F
from torchvision import transforms
import datasets  # This import seems unused in the provided code snippet
from PIL import Image
import importlib.util
import os
from ultralytics import YOLO  # The key import for YOLOv8
from torchvision import datasets as torchvision_datasets

# Get the directory of the current script
SCRIPT_DIR = os.path.dirname(__file__)
# Calculate the project root directory (one level up from the script directory)
PROJECT_ROOT = os.path.abspath(
    os.path.join(
        SCRIPT_DIR,
    )
)

# --- CONFIGURATION ---
# Make paths relative to the PROJECT ROOT
DATASET_PATH = os.path.join(PROJECT_ROOT, "Full_VFD_Dataset")
# Stage 2: Recognition Model
RECOGNITION_MODEL_PATH = os.path.join(PROJECT_ROOT, "best.pt")
RECOGNITION_MODEL_CONFIG_PATH = os.path.join(PROJECT_ROOT, "model_architecture.py")
# Stage 1: Detection Model
FACE_DETECTION_MODEL_PATH = os.path.join(PROJECT_ROOT, "yolov8n.pt")

# !!! IMPORTANT: Replace this with the actual class name from your model file !!!
# For example, it might be 'ResNet18WithPatchGridShuffle' or similar.
YOUR_RECOGNITION_MODEL_CLASS_NAME = "ResNet18WithUPSCA"

# --- HELPER FUNCTIONS ---


def load_recognizer_from_file(file_path, class_name, num_classes):
    """Dynamically loads a custom recognition model class from a Python file."""
    spec = importlib.util.spec_from_file_location(
        name="model_definition", location=file_path
    )
    model_module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(model_module)

    ModelClass = getattr(model_module, class_name)
    model = ModelClass(num_classes=num_classes)
    return model


def setup_pipeline():
    """
    Loads both models (detector and recognizer), class names, and sets up the device.
    This should be called once to avoid reloading everything on each inference.
    """
    print("--- Setting up the inference pipeline ---")

    # 1. Determine device
    device = torch.device(
        "cuda"
        if torch.cuda.is_available()
        else "mps" if torch.backends.mps.is_available() else "cpu"
    )
    print(f"Using device: {device}")

    # 2. Load the YOLOv8 face detector model
    print(f"Loading face detector model from: {FACE_DETECTION_MODEL_PATH}")
    # Check if the file exists before loading
    if not os.path.exists(FACE_DETECTION_MODEL_PATH):
        raise FileNotFoundError(
            f"Face detection model not found at: {FACE_DETECTION_MODEL_PATH}"
        )
    detector_model = YOLO(FACE_DETECTION_MODEL_PATH)

    # 3. Load class names for the recognizer from the ImageFolder directory
    #    This is the corrected section.
    print(f"Loading class names from ImageFolder: {DATASET_PATH}...")
    try:
        # We use torchvision.datasets.ImageFolder, just like in your training script.
        # We don't need to load the images, just initialize the object to get the class names.
        # Check if the directory exists before initializing ImageFolder
        if not os.path.isdir(DATASET_PATH):
            raise FileNotFoundError(f"Dataset directory not found at: {DATASET_PATH}")

        full_dataset = torchvision_datasets.ImageFolder(root=DATASET_PATH)
        class_names = full_dataset.classes
        num_classes = len(class_names)
        print(f"Found {num_classes} classes for recognition: {class_names}")
    except FileNotFoundError as e:
        print(f"ERROR: {e}. Please check the DATASET_PATH configuration.")
        raise
    except Exception as e:
        print(f"An error occurred while reading the dataset directory: {e}")
        raise

    # 4. Load the custom face recognizer model architecture
    print(f"Loading recognizer architecture from: {RECOGNITION_MODEL_CONFIG_PATH}")
    # Check if the file exists before loading
    if not os.path.exists(RECOGNITION_MODEL_CONFIG_PATH):
        raise FileNotFoundError(
            f"Recognizer architecture file not found at: {RECOGNITION_MODEL_CONFIG_PATH}"
        )
    recognizer_model = load_recognizer_from_file(
        RECOGNITION_MODEL_CONFIG_PATH, YOUR_RECOGNITION_MODEL_CLASS_NAME, num_classes
    )

    # 5. Load the trained weights into the recognizer
    print(f"Loading recognizer weights from: {RECOGNITION_MODEL_PATH}")
    # Check if the file exists before loading
    if not os.path.exists(RECOGNITION_MODEL_PATH):
        raise FileNotFoundError(
            f"Recognizer weights file not found at: {RECOGNITION_MODEL_PATH}"
        )
    recognizer_model.load_state_dict(
        torch.load(RECOGNITION_MODEL_PATH, map_location=device)
    )
    recognizer_model.to(device)

    # 6. Set recognizer to evaluation mode (VERY IMPORTANT!)
    recognizer_model.eval()
    print("--- Pipeline setup complete. Ready for inference. ---")

    return detector_model, recognizer_model, class_names, device


def preprocess_face_image(image: Image.Image):
    """Applies the necessary transformations to a cropped face image for the recognizer."""
    # These transforms should match what you used during training the recognizer
    transform = transforms.Compose(
        [
            transforms.Resize((224, 224)),  # ResNet models usually take 224x224 inputs
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ]
    )
    return transform(image).unsqueeze(0)  # Add batch dimension


# --- INFERENCE DRIVER CODE ---


# Modify the infer function to accept a PIL Image directly
def infer(image_pil: Image.Image, detector, recognizer, class_names: list, device):
    """
    Takes a PIL Image, detects faces using YOLOv8, and performs recognition
    on each face using the custom ResNet model.

    Args:
        image_pil (Image.Image): The input image as a PIL Image object.
        detector (YOLO): The loaded YOLOv8 model object.
        recognizer (torch.nn.Module): The loaded custom PyTorch recognition model.
        class_names (list): A list of class names for mapping predictions.
        device (torch.device): The device to run inference on ('cpu', 'cuda', etc.).

    Returns:
        list: A list of dictionaries, where each dictionary contains details
        about a recognized face (name, confidence, and bounding box).
    """
    # --- Stage 1: Face Detection with YOLOv8 ---
    print("Step 1: Detecting faces with YOLOv8...")
    # Pass the PIL Image directly to the detector
    detection_results = detector(
        image_pil, verbose=False
    )  # verbose=False cleans up the output

    # The result is a list, get the Boxes object for the first image
    boxes = detection_results[0].boxes
    if len(boxes) == 0:
        print("No faces detected in the image.")
        return []

    print(f"Found {len(boxes)} face(s).")

    # --- Stage 2: Face Recognition for each detected face ---
    predictions = []
    print("\nStep 2: Recognizing each face...")
    with torch.no_grad():  # Disable gradient calculation for efficiency
        for i, box in enumerate(boxes):
            # Get bounding box coordinates
            # .xyxy gets the tensor, [0] selects the first box's coords, .int().tolist() converts to a clean list
            x1, y1, x2, y2 = box.xyxy[0].int().tolist()

            # Crop the face from the original image
            face_image = image_pil.crop((x1, y1, x2, y2))

            # Preprocess the cropped face for the recognition model
            input_tensor = preprocess_face_image(face_image).to(device)

            # Get recognition model output (logits)
            output = recognizer(input_tensor)

            # Convert logits to probabilities
            probabilities = F.softmax(output, dim=1)

            # Get the top prediction
            confidence, predicted_idx = torch.max(probabilities, 1)

            predicted_class = class_names[predicted_idx.item()]
            confidence_score = confidence.item()
                        
            result = {
                "name": predicted_class,
                "recognition_confidence": f"{confidence_score:.2%}",
                "detection_confidence": f"{box.conf.item():.2%}",
                "location_xyxy": (x1, y1, x2, y2),
            }
            predictions.append(result)

            print(
                f" -> Face #{i+1}: Prediction: {result['name']} (Confidence: {result['recognition_confidence']})"
            )

    return predictions


if __name__ == "__main__":
    # --- This is an example of how to use the functions ---

    # 1. Set up the entire pipeline (loads both models, classes, etc.)
    # This is done only once at the start of your application.
    try:
        detector, recognizer, class_names, device = setup_pipeline()
    except FileNotFoundError as e:
        print(f"Could not set up pipeline: {e}")
        sys.exit(1)  # Exit if pipeline setup fails

    # 2. Specify the path to your test image.
    # Replace this with the actual path to an image you want to test.
    # Example using a path relative to the script:
    test_image_path = os.path.join(PROJECT_ROOT, "test_images", "test_image.jpg")
    # Example using an absolute path:
    # test_image_path = '/Users/dhruv/Pictures/Dhruv Pics/photo.png'

    # 3. Run inference on the image
    if os.path.exists(test_image_path):
        print(f"\n--- Running Full Pipeline on '{test_image_path}' ---")
        try:
            image_pil = Image.open(test_image_path).convert("RGB")
            results = infer(image_pil, detector, recognizer, class_names, device)

            if results:
                print("\n--- Inference Summary ---")
                print(f"Processed image and found {len(results)} person/people.")
                for i, res in enumerate(results):
                    print(f"\nResult #{i+1}:")
                    print(f"  - Name: {res['name']}")
                    print(
                        f"  - Recognition Confidence: {res['recognition_confidence']}"
                    )
                    print(f"  - Detection Confidence: {res['detection_confidence']}")
                    print(f"  - Bounding Box (x1, y1, x2, y2): {res['location_xyxy']}")
            else:
                print("\n--- Inference Summary ---")
                print("Pipeline finished, but no one was recognized.")

        except FileNotFoundError:
            print(f"Error: Test image file not found at {test_image_path}")
        except Exception as e:
            print(f"An error occurred during test inference: {e}")

    else:
        print(f"\n--- SKIPPING INFERENCE ---")
        print(f"Test image not found at: '{test_image_path}'")
        print(
            "Please update the 'test_image_path' variable in the main block to run an example."
        )
