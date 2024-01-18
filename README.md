This projects contains the DefectDojo upload client and statistics client. It is for example used within the ClusterImageScanner.

# Entrypoints
The default entrypoint for the upload-client image is  
```bash
"java", "-cp", "@/app/jib-classpath-file", "org.sdase.Main".
```
To use the upload validation, use
```bash
"java", "-cp", "@/app/jib-classpath-file", "org.sdase.uploadValidation.MainValidation".
```
To delete test products, use
```bash
"java", "-cp", "@/app/jib-classpath-file", "org.sdase.deleteTestProduct.MainDeleteProduct".
```
# Run as image
```bash
docker run -ti \
  -e "PRODUCT_NAME_TO_DELETE=xxx" \
  -e "DEFECTDOJO_URL=xxx" \
  -e "DEFECTDOJO_API_KEY=xxx" \
  -e "DEFECTDOJO_USERNAME=xxx" \
  -v $(pwd)/delete.groovy:/delete.groovy --entrypoint="java -cp @/app/jib-classpath-file org.sdase.DeleteProducts" quay.io/sdase/defectdojo-client:4
```