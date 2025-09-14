from sqlalchemy import Column, Integer, String, Boolean, Text, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.sql import func

Base = declarative_base()


class StudentRecognitionLog(Base):
    __tablename__ = "student_recognition_logs"  # This becomes your table name

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=True)
    detection_confidence = Column(String, nullable=True)
    recognition_confidence = Column(String, nullable=True)
    location_xyxy = Column(Text, nullable=True)  # Store coordinates as JSON string
    status = Column(Boolean, default=True)
    timestamp = Column(
        DateTime(timezone=True), server_default=func.now()
    )  # Automatically captures insert time
