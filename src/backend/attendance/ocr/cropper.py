from ultralytics import YOLO
import cv2
import uuid

model=YOLO('best.pt')


'''
@def: the function will crop the image and return the name of the cropped image
@params: image: the image to be cropped. String type, the name of the image
@returns: filename: the name of the cropped image
'''
def cropper(image):
    image = cv2.imread(image)
    
    x = model.predict(image) # Get the predictions
    x = x[0].boxes # extract the boxes
    
    # convert into the bounding boxes
    bbox_raw = x.xyxy[0]
    bbox= [] # empty list to store the bounding boxes
    for bound in bbox_raw:
        bbox.append(int(bound.item()))
    
    cropped_image = image[bbox[1]:bbox[3], bbox[0]:bbox[2]] # crop the image
    
    filename=str(uuid.uuid4()) + '.jpg' # generate a unique name for the image
    cv2.imwrite(filename, cropped_image)
    
    return filename # return the name of the cropped image

# test the function
# cropped_image = cropper('photo1.jpg')
