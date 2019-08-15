#!/bin/bash

image="defectdojo-client"
buildah bud --tag "${image}"

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
build_dir="${dir}/build"

if [ -n "${BUILD_EXPORT_OCI_ARCHIVES}" ]
then
    mkdir --parent "${build_dir}"
    buildah push --quiet \
    "localhost/${image}" \
    "oci-archive:${build_dir}/${image//:/-}.tar"
    buildah rmi "${image}"
fi
