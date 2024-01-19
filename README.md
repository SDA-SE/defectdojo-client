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
export PRODUCT_NAME_TO_DELETE=String_In_Product_Name
export DEFECTDOJO_URL=xx
export DEFECTDOJO_APIKEY=xx
export DEFECTDOJO_USERNAME=xx

docker run -ti \
  -e "PRODUCT_NAME_TO_DELETE=${PRODUCT_NAME_TO_DELETE}" \
  -e "DEFECTDOJO_URL=${DEFECTDOJO_URL}" \
  -e "DEFECTDOJO_API_KEY=${DEFECTDOJO_APIKEY}" \
  -e "DEFECTDOJO_USERNAME=${DEFECTDOJO_USERNAME}" \
 --entrypoint="java" quay.io/sdase/defectdojo-client:4 -cp @/app/jib-classpath-file org.sdase.deleteProduct.DeleteProduct
```