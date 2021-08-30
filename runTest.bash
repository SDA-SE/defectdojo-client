#!/bin/bash
set -e
source env.bash

if [ "$DD_URL" == "" ]; then
  echo "DD_URL not set"
  exit;
fi

if [ "$DD_TOKEN" == "" ]; then
  echo "DD_TOKEN not set"
  exit;
fi

if [ "${DD_USER}" == "" ]; then
  export DD_USER="cluster-scan"
fi
groovy defectdojo.groovy

