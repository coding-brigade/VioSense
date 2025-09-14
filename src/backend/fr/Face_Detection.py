import cv2
import matplotlib.pyplot as plt


def detect_and_crop_faces(image_path):
    # Load the image
    image = cv2.imread(image_path)

    # Convert the image to grayscale
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Load the pre-trained Haar cascade face detector
    face_cascade = cv2.CascadeClassifier(
        cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
    )

    # Detect faces in the image
    faces = face_cascade.detectMultiScale(
        gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30)
    )

    # Initialize a list to store cropped faces
    cropped_faces = []

    # Crop and append each detected face to the list
    for x, y, w, h in faces:
        cropped_faces.append(image[y : y + h, x : x + w])

    return cropped_faces


# # Replace 'image.jpg' with the path to your image
# cropped_faces = detect_and_crop_faces("1.jpg")

# # # Display each cropped face using plt.imshow()
# # for face in cropped_faces:
# #     plt.imshow(cv2.cvtColor(face, cv2.COLOR_BGR2RGB))
# #     plt.show()
