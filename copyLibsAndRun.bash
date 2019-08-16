#!/bin/bash

ENGINE_PATH="../engine"

cp $ENGINE_PATH/scb-sdk/target/sdk-0.0.1-SNAPSHOT.jar lib/ ; cp $ENGINE_PATH/scb-persistenceproviders/defectdojo-persistenceprovider/target/defectdojo-persistenceprovider-0.0.1-SNAPSHOT-jar-with-dependencies.jar lib/ ; rm -Rf $HOME/.groovy/grapes/io.securecodebo*; rm -Rf $HOME./repository/io/securecodebox


export DD_TOKEN="8319a3b466240ef6e162f5d26eafc336e693f840"
export DD_PRODUCT_NAME="test6"
export DD_REPORT_PATH="/home/tpagel/dependency-check-report-valid.xml"
export DD_BRANCH_NAME="master"
export DD_BUILD_ID=1
export DD_SOURCE_CODE_MANAGEMENT_URI="https://github.com/XYZ"
export DD_IMPORT_TYPE="reimport" # reimport or import

groovy defectdojo.groovy
