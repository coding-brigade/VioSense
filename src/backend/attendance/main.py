# main.py or wherever you initialize
from database.database import engine
from models.model import Base

Base.metadata.create_all(bind=engine)