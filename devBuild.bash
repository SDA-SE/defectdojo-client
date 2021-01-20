#!/bin/bash

ENGINE_PATH="/home/tpagel/git/securecodebox/defectdojo-client-java"

CURRENT_PATH=$(pwd)
cd $ENGINE_PATH
./gradlew build
exitcode=$?
cd $CURRENT_PATH
exit $exitcode
