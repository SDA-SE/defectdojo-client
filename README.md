This projects contains the DefectDojo upload client and statistics client. It is for example used within the ClusterImageScanner.

# Entrypoints
The default entrypoint for the upload-client image is  
```
"java", "-cp", "@/app/jib-classpath-file", "org.sdase.Main".
```
To use the upload validation, use
```
"java", "-cp", "@/app/jib-classpath-file", "org.sdase.uploadValidation.MainValidation".
```
