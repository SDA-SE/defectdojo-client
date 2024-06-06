#!/bin/bash
./gradlew build
docker run --memory 1024m -v $(pwd)/build/classes/groovy/main/:/app/classes/:ro --network=host    -e "DEFECTDOJO_URL=${DEFECTDOJO_URL}"   -e "DEFECTDOJO_APIKEY=${DEFECTDOJO_API_KEY}"   -e "DEFECTDOJO_USERNAME=${DEFECTDOJO_USERNAME}"    quay.io/sdase/defectdojo-statistic-client:4
