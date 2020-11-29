import os
import appgallery
from appgallery import utils

os.environ['HUAWEI_CREDENTIALS_PATH'] = 'path/to/credentials.json'
client = appgallery.Client()
