#!/bin/bash
set -e

if [ "$DEFECTDOJO_URL" == "" ]; then
  echo "DEFECTDOJO_URL not set"
  exit;
fi

if [ "$DEFECTDOJO_APIKEY" == "" ]; then
  echo "DEFECTDOJO_APIKEY not set"
  exit;
fi

if [ "${DEFECTDOJO_USERNAME}" == "" ]; then
  export DEFECTDOJO_USERNAME="cluster-scan"
fi
export STATISTIC_FILE_PATH=/tmp/team-statistics.csv
groovy defectdojo.groovy
