#!/bin/bash
#set -e


docker run  -v $(pwd)/test:/test -e DD_REPORT_PATH="/test/dependency-check-report-logstash.xml" -e DD_PRODUCT_TAGS="cluster/production" -e DD_PRODUCT_TAGS="cluster/production" -e  DD_TEAM="fellowship-of-the-ring-t3" \
  -e IS_CREATE_GROUPS="true" \
  -e DD_BRANCH_NAME="2.0.0" \
  -e DD_PRODUCT_NAME="production-cluster | mordor | quay.io/sdase/ring" \
  -e DEFECTDOJO_URL=$DEFECTDOJO_URL \
  -e DEFECTDOJO_APIKEY=$DEFECTDOJO_APIKEY \
  -e DEFECTDOJO_USERNAME=$DEFECTDOJO_USERNAME \
  -e DEFECTDOJO_MAX_PAGE_COUNT_FOR_GETS=1000 \
  -e DD_DEDUPLICATION_ON_ENGAGEMENT=true \
  quay.io/sdase/defectdojo-client:3
