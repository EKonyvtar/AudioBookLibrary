export FILE_PATH='./mobile/google-services.json'

if [ ! -f $FILE_PATH ]; then
    echo "$FILE_PATH does not exist"
    echo $FILE_GOOGLE_SERVICES_JSON | base64 --decode > $FILE_PATH
else
    echo "$FILE_PATH already exists. Skipping"
fi
