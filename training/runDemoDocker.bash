#!/bin/bash
#set -e

participants=0 #-1

mkdir tmp || true
touch tmp/defectDojoTestLink.txt
chmod -R 777 tmp || true

for i in $(seq 0 $participants); do

  docker run  -v $(pwd)/test:/test -e DD_REPORT_PATH="/test/dependency-check-report-10.xml" \
    -v $(pwd)/tmp:/code \
    -e DD_PRODUCT_TAGS="cluster/production" -e DD_PRODUCT_TAGS="cluster/production" -e  DD_TEAM="fellowship-of-the-ring-$i" \
    -e IS_CREATE_GROUPS="true" \
    -e DD_BRANCH_NAME="2.0.0" \
    -e DD_PRODUCT_NAME="production-cluster-$i | mordor | quay.io/sdase/ring" \
    -e DEFECTDOJO_URL=$DEFECTDOJO_URL \
    -e DEFECTDOJO_APIKEY=$DEFECTDOJO_APIKEY \
    -e DEFECTDOJO_USERNAME=$DEFECTDOJO_USERNAME \
    -e DEFECTDOJO_MAX_PAGE_COUNT_FOR_GETS=1000 \
    -e DD_DEDUPLICATION_ON_ENGAGEMENT=true \
    quay.io/sdase/defectdojo-client:3 2> /dev/null

  docker run  -v $(pwd)/test:/test -e DD_REPORT_PATH="/test/findings.csv" \
    -v $(pwd)/tmp:/code \
    -e DD_PRODUCT_TAGS="cluster/production" -e DD_PRODUCT_TAGS="cluster/production" -e  DD_TEAM="fellowship-of-the-ring-$i" \
    -e IS_CREATE_GROUPS="true" \
    -e DD_REPORT_TYPE="Generic Findings Import" \
    -e DD_BRANCH_NAME="2.0.0" \
    -e DD_PRODUCT_NAME="production-cluster-$i | mordor | quay.io/sdase/ring" \
    -e DEFECTDOJO_URL=$DEFECTDOJO_URL \
    -e DEFECTDOJO_APIKEY=$DEFECTDOJO_APIKEY \
    -e DEFECTDOJO_USERNAME=$DEFECTDOJO_USERNAME \
    -e DEFECTDOJO_MAX_PAGE_COUNT_FOR_GETS=1000 \
    -e DD_DEDUPLICATION_ON_ENGAGEMENT=true \
    quay.io/sdase/defectdojo-client:3 2> /dev/null
done