#!/bin/bash
#set -e


#export DD_URL="https://defectdojo-test.tools.sda-se.io/"
#export DD_URL=http://localhost:8081/

#export DD_USER="admin"
export DD_PRODUCT_NAME="production-cluster | mordor | quay.io/sdase/ring"
#export DD_PRODUCT_DESCRIPTION="Test defectdojo by tpagel"
export DD_BRANCH_NAME="2.0.0"
#export DD_BUILD_ID=1
export DD_SOURCE_CODE_MANAGEMENT_URI="" # https://github.com/XYZ
export DD_DEDUPLICATION_ON_ENGAGEMENT="true"
export DD_REPORT_PATH="./test/dependency-check-report-5.xml"
export DD_PRODUCT_TAGS="cluster/production"
export DD_TEAM="fellowship-of-the-ring-t3"
export IS_CREATE_GROUPS="true"
./gradlew run

export DD_REPORT_PATH="./test/dependency-check-report-10.xml"
./gradlew run

export DD_REPORT_PATH="./test/findings.csv"
export DD_REPORT_TYPE="Generic Findings Import"
./gradlew run
./gradlew run

export DD_REPORT_TYPE="Dependency Check Scan"
export DD_BRANCH_NAME="2.0.1"
export DD_REPORT_PATH="./test/dependency-check-report-10.xml"
./gradlew run
exit

export DD_REPORT_PATH="./test/dependency-check-report-5.xml"
./gradlew run

export DD_REPORT_PATH="./test/findings.csv"
export DD_REPORT_TYPE="Generic Findings Import"
./gradlew run


