"""
Run Flask API: flask --app app.py --debug run
"""

import os, sys
from flask import Flask, request, Response, render_template
import zipfile
from datetime import date
import cv2
import matplotlib.pyplot as plt
import time
import numpy
from Face_Detection import detect_and_crop_faces

UPLOAD_FOLDER = os.path.dirname(os.path.realpath(__file__))
app = Flask(__name__)
INFERENCE_FOLDER = "./Uploads/Inference"


# """ Upload Zip Folder for Training - DataSet """
@app.route("/upload_zip", methods=["POST"])
def upload():
    file = request.files["data_zip_file"]

    # Check if the uploaded file is a zip file
    if file.filename.endswith(".zip"):
        fileName = file.filename.split(".")[0]

        file_like_object = file.stream._file
        zipfile_ob = zipfile.ZipFile(file_like_object)

        my_dir = "./Uploads/Training"
        text_file_path = "./Uploads/Training/Upload.txt"
        subFolderName = f"./Uploads/Training/{fileName}"

        for files in zipfile_ob.namelist():
            zipfile_ob.extract(files, my_dir)

        zipfile_ob.close()
        data = os.listdir(subFolderName)

        print(data)

        with open(text_file_path, "a") as text_file:
            text_file.write(fileName + "\n")

        with open(text_file_path, "a") as text_file:
            for i, data in enumerate(data):
                print(i)
                text_file.write("\t" + " - " + data + "\n")

        return {"message": "success"}
    else:
        return {"error": "Uploaded file is not a zip file"}


"""
Find the inference from an image. 
Step 1: Select multiple images [✅]
Step 2: Upload those images into Uploads Folder (Filter with year, sem, date) [✅]
Step 3: Crop the Face from the images and Store (Alter make a list and append those face encoding)
Step 4: FR model run and find the Enrollment of the student 
Step 5: Return Json Response and Store into DataBase
"""


# """ Above functionality... """
@app.route("/inference_upload", methods=["POST"])
def Upload_Inference():
    """
    Collection of Variable
    """
    files_input = request.files.getlist("file")
    semester = str(request.form.get("semester"))
    division = str(request.form.get("division"))
    department = str(request.form.get("department"))
    slot = str(request.form.get("slot"))

    Today_date = str(date.today())
    slot_list = [str("Slot-" + str(i)) for i in range(1, 7)]
    department_list = ["IT", "CE"]
    semester_list = [str("SEM-" + str(i)) for i in range(1, 9)]
    division_list = ["I", "J"]

    #

    print("Today-Date", Today_date)
    print("Slot", slot)
    print("Department", department)
    print("Semester", semester)
    print("Division", division)

    #

    """
    If current date folder didn't exist then create new Folder
    """
    if Today_date not in os.listdir(INFERENCE_FOLDER):
        os.mkdir(os.path.join(INFERENCE_FOLDER, Today_date))
        for slot in slot_list:
            os.mkdir(os.path.join(INFERENCE_FOLDER, Today_date, slot))
            for dept in department_list:
                os.mkdir(os.path.join(INFERENCE_FOLDER, Today_date, slot, dept))
                for i in semester_list:
                    os.mkdir(os.path.join(INFERENCE_FOLDER, Today_date, slot, dept, i))
                    for div in division_list:
                        os.mkdir(
                            os.path.join(
                                INFERENCE_FOLDER, Today_date, slot, dept, i, div
                            )
                        )
        # # Introduce a delay after creating folders
        # time.sleep(1)

    slot = "SLOT-"+slot
    semester = "SEM-"+semester
    Inference_Image_Folder_location = os.path.join(
        INFERENCE_FOLDER, Today_date, slot, department, semester, division
    )
    cropped_face_list = {}
    
    if not os.path.exists(Inference_Image_Folder_location):
        print("-" * 50)
        print("Creating folders...")
        # Create folders recursively, ensuring all levels exist
        os.makedirs(Inference_Image_Folder_location, exist_ok=True)
        print("-" * 50)

    if os.path.exists(Inference_Image_Folder_location):
        print("-" * 50)
        print("Move images into Folder")
        for i in files_input:
            i.save(
                os.path.join(
                    INFERENCE_FOLDER,
                    Today_date,
                    slot,
                    department,
                    semester,
                    division,
                    i.filename,
                )
            )
        print("-" * 50)

        list_of_images = os.listdir(Inference_Image_Folder_location)

        for i in list_of_images:
            path = os.path.join(Inference_Image_Folder_location, i)

            cropped_face = detect_and_crop_faces(path)
            cropped_face_list[path] = cropped_face

        print(cropped_face_list)

    else:
        return {"message": "Path Not Exist"}

    return {"message": "Success"}


if __name__ == "__main__":
    app.run(debug=True, port=5001)
