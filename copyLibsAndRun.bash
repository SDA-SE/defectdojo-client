#!/bin/bash

ENGINE_PATH="/home/tpagel/git/securecodebox/defectdojo-client-java"

VERSION=0.0.1-SNAPSHOT
#mkdir -p ~/.groovy/grapes/io.securecodebox/defectdojo-client/jars/ || true
#cp $ENGINE_PATH/build/libs/defectdojo-client-1.0-SNAPSHOT.jar ~/.groovy/grapes/io.securecodebox/defectdojo-client/jars/defectdojo-client-${VERSION}.jar

export DD_TOKEN="8319a3b466240ef6e162f5d26eafc336e693f840"
export DD_PRODUCT_NAME="test8"
export DD_REPORT_PATH="/home/tpagel/dependency-check-report.xml"
export DD_BRANCH_NAME="master"
export DD_BUILD_ID=1
export DD_SOURCE_CODE_MANAGEMENT_URI="" # https://github.com/XYZ
export DD_IMPORT_TYPE="import" # reimport or import
export DD_BRANCHES_TO_KEEP="master feature/3"
export DD_PRODUCT_TYPE=1
export DD_DEDUPLICATION_ON_ENGAGEMENT="false"
groovy defectdojo.groovy
