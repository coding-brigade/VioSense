from fastapi import FastAPI, UploadFile, File, Form, Depends
from fastapi.responses import JSONResponse
from typing import List
from PIL import Image
import io
from sqlalchemy.orm import Session
import os
import sys
import json
from models.model import *
from pydantic import BaseModel
from database.database import *
from typing import List, Optional


# Add the project root to sys.path
# This ensures imports work regardless of where the script is run from (though running from root is still best)
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

# Import the inference utility
from inference_utility import setup_pipeline, infer

# Initialize FastAPI app
app = FastAPI(title="Face Recognition API")

# --- Global variables to hold the loaded pipeline ---
# Using a dictionary is slightly cleaner for managing state
pipeline = {"detector": None, "recognizer": None, "class_names": None, "device": None}


# Storing Present Student Information
class StudentDetailsItem(BaseModel):
    detection_confidence: Optional[str]
    location_xyxy: Optional[List[float]]
    name: Optional[str]
    recognition_confidence: Optional[str]
    status: Optional[bool] = True


@app.on_event("startup")
async def startup_event():
    """Load the inference pipeline when the server starts."""
    print("INFO:     Loading inference pipeline...")
    try:
        (
            pipeline["detector"],
            pipeline["recognizer"],
            pipeline["class_names"],
            pipeline["device"],
        ) = setup_pipeline()
        print("INFO:     Inference pipeline loaded successfully.")
    except Exception as e:
        print(f"FATAL:    Failed to load pipeline on startup: {e}")
        # In a real app, you might want to exit or handle this more gracefully
        # For now, we'll let it fail loudly.


# Dependency to get DB session
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@app.post("/submit_students")
async def submit_students(
    data: List[StudentDetailsItem], db: Session = Depends(get_db)
):
    for item in data:
        log = StudentRecognitionLog(
            name=item.name,
            detection_confidence=item.detection_confidence,
            recognition_confidence=item.recognition_confidence,
            location_xyxy=json.dumps(item.location_xyxy),  # Convert to string
            status=item.status,
        )
        db.add(log)
    db.commit()

    return {"message": "Student list saved", "count": len(data)}


@app.post("/infer", summary="Recognize   faces in an uploaded image")
async def run_inference(
    files: List[UploadFile] = File(..., description="List of image files to process."),
    studentInfo: str = Form(...),  # Receive raw JSON string
):

    try:
        student_data = json.loads(studentInfo)
        print("Received Student Data:", student_data)
        # Example access
        print("Batch:", student_data.get("studentBatch"))
        print("Department:", student_data.get("studentDepartment"))

    except json.JSONDecodeError as e:
        return JSONResponse(
            status_code=400, content={"message": "Invalid JSON in studentInfo"}
        )

    """
    Receives an image, performs face detection and recognition, and returns the results.
    """
    if not pipeline["detector"]:
        return JSONResponse(
            status_code=503,  # Service Unavailable
            content={
                "message": "Inference pipeline is not available. Check server startup logs.",
                "data": [],
                "statusCode": 503,
            },
        )

    if not files[0].content_type.startswith("image/"):
        return JSONResponse(
            status_code=400,
            content={
                "message": "Invalid file type. Please upload an image.",
                "data": [],
                "statusCode": 400,
            },
        )

    try:
        # Read image data from the upload
        image_data = await files[0].read()
        # Open it as a PIL Image object
        image = Image.open(io.BytesIO(image_data)).convert("RGB")

        # Perform inference by passing the PIL Image object directly
        results = infer(
            image_pil=image,
            detector=pipeline["detector"],
            recognizer=pipeline["recognizer"],
            class_names=pipeline["class_names"],
            device=pipeline["device"],
        )

        if not results:
            return JSONResponse(
                content={
                    "message": "No faces were recognized in the image.",
                    "data": [],
                    "statusCode": 400,
                }
            )

        return JSONResponse(
            content={"message": "Success", "data": results, "statusCode": 200}
        )

    except Exception as e:
        print(f"ERROR:    An error occurred during inference: {e}")
        return JSONResponse(
            status_code=500,
            content={
                "message": "An internal server error occurred during inference.",
                "data": [],
                "statusCode": 400,
            },
        )


# To run this server:
# 1. Ensure you have an empty `__init__.py` in the `backend` directory.
# 2. Open your terminal in the PROJECT ROOT directory (the one containing 'backend').
# 3. Run: uvicorn server:app --reload
