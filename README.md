# File Uploader and Archive App

### Dependencies
* jdk 11 
* docker 19

### How to Build (OPTIONAL: as the image's been pushed to docker hub)
* Build docker image via gradle  \
`./gradlew bootBuildImage --imageName=sbilobrov/correvate`

### How to Run
* Run the built image (take the name of image from the build stage) \ 
`docker run -p 8080:8080 -t sbilobrov/correvate`

* OPTIONAL: we can pass Java Arg or spring configuration
    - Run in debug mode \
    `docker run -e "JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=n" -p 8080:8080 -p 5005:5005 -t sbilobrov/correvate`
     - Run with increasing size of uploaded files  \
     `docker run -e "SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=50MB" -e "SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=60MB" -p 8080:8080 -t sbilobrov/correvate`

### How to Use

* Adjust curl request by changing paths to files and execute
```bash
curl --location --request POST 'http://localhost:8080/multi-upload' --header 'Content-Type: multipart/form-data' --form 'files=@example.csv' --form 'files=@patients-seed.sql' --output file.zip
```
* Extract the zip file and check the files inside. There should be files that we sent by curl

### Possible Improvements
* Uploading by chunks (Ability to stop and resume uploading)
* Mount an external volume, or even better to use S3 service to keep data for chunks logic or to keep data for user 
* Use Async Requests for processing big files 
    - provide http links to big files to avoid connection problems from user's side
    - in this case user checks the readiness of zip file
    - notify user with email, once it is ready to download an archive
* Scheduled file cleanup if we use file system to process data (saving, archiving, downloading)
* Persisting historical data about uploads
* Add more load tests
* Pass level and method of compression as request params
* Move logic to service layer if needed. Now there is no many code lines to be extracted

### Limitation
* File's size is limited by configuration `spring.servlet.multipart`
* We may catch OutOfMemoryError if uploading files too big to process within Java Heap