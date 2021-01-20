#!/bin/bash

ENGINE_PATH="/home/tpagel/git/securecodebox/defectdojo-client-java"

#VERSION=0.0.3-SNAPSHOT
#mkdir -p ~/.groovy/grapes/io.securecodebox/defectdojo-client/jars/ || true
#cp $ENGINE_PATH/build/libs/defectdojo-client-$VERSION.jar ~/.groovy/grapes/io.securecodebox/defectdojo-client/jars/defectdojo-client-${VERSION}.jar

export DD_URL="https://defectdojo-test.tools.sda-se.io/"
DD_USER="clusterscanner"
export DD_TOKEN="4d34ecc02ddf71d8a80f89569f25b9f09e9f7da9"
export DD_PRODUCT_NAME="test8"
export DD_PRODUCT_DESCRIPTION="Test defectdojo by tpagel"
export DD_BRANCH_NAME="master"
export DD_BUILD_ID=1
export DD_SOURCE_CODE_MANAGEMENT_URI="" # https://github.com/XYZ
export DD_DEDUPLICATION_ON_ENGAGEMENT="false"
export DD_REPORT_PATH="./test/dependency-check-report-5.xml"
groovy defectdojo.groovy

export DD_REPORT_PATH="./test/dependency-check-report-10.xml"
groovy defectdojo.groovy
