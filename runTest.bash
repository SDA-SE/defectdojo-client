#!/bin/bash
#set -e
ENGINE_PATH="/home/tpagel/git/securecodebox/defectdojo-client-java"

if [ "$DD_TOKEN" == "" ]; then
  echo "DD_TOKEN not set"
  exit;
fi

export DD_URL="https://defectdojo-test.tools.sda-se.io/"
export DD_USER="clusterscanner"
export DD_PRODUCT_NAME="test-2021-03-22-dedup-on-engagement-level-engagement-per-image"
export DD_PRODUCT_DESCRIPTION="Test defectdojo by tpagel"
export DD_BRANCH_NAME="image:2.0.0"
#export DD_BUILD_ID=1
export DD_SOURCE_CODE_MANAGEMENT_URI="" # https://github.com/XYZ
export DD_DEDUPLICATION_ON_ENGAGEMENT="true"
export DD_REPORT_PATH="./test/dependency-check-report-5.xml"
groovy defectdojo.groovy

export DD_REPORT_PATH="./test/dependency-check-report-10.xml"
groovy defectdojo.groovy

export DD_REPORT_PATH="./test/findings.csv"
export DD_REPORT_TYPE="Generic Findings Import"
groovy defectdojo.groovy


export DD_REPORT_TYPE="Dependency Check Scan"
export DD_BRANCH_NAME="image:2.0.1"
export DD_REPORT_PATH="./test/dependency-check-report-5.xml"
groovy defectdojo.groovy

export DD_REPORT_PATH="./test/dependency-check-report-10.xml"
groovy defectdojo.groovy

export DD_REPORT_PATH="./test/findings.csv"
export DD_REPORT_TYPE="Generic Findings Import"
groovy defectdojo.groovy


