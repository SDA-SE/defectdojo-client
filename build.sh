#!/bin/bash
set -xe

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
build_dir="${dir}/build"

trap cleanup INT EXIT
cleanup() {
  test -n "${scb_container}" && buildah rm "${scb_container}" || true
  test -n "${defectdojo_container}" && buildah rm "${defectdojo_container}" || true
  test -d "${scb_dir_tmp}" && rm -rf "${scb_dir_tmp}" || true
}

image="defectdojo-client"

scb_container="$(buildah from docker.io/securecodebox/engine:master)"
scb_mnt="$(buildah mount "${scb_container}")"

defectdojo_container="$(buildah from quay.io/sdase/openjdk-development:12-openj9)"
defectdojo_mnt="$(buildah mount "${scb_container}")"

mkdir -p "${defectdojo_mnt}/.groovy/grapes/io.securecodebox.core/sdk/jars/"
mkdir -p "${defectdojo_mnt}/.groovy/lib/"
mkdir -p "${defectdojo_mnt}/.groovy/grapes/io.securecodebox.persistenceproviders/defectdojo-persistenceprovider/jars/"
mkdir ${defectdojo_mnt}/code

scb_dir_tmp="$(mktemp -d)"
pushd "${scb_dir_tmp}"
cp ${scb_mnt}/scb-engine/lib/defectdojo-persistenceprovider-0.0.1-SNAPSHOT-jar-with-dependencies.jar "${defectdojo_mnt}/.groovy/grapes/io.securecodebox.persistenceproviders/defectdojo-persistenceprovider/jars/defectdojo-persistenceprovider-0.0.1-SNAPSHOT.jar"
unzip "${scb_mnt}/scb-engine/app.jar"
cp ./BOOT-INF/lib/sdk-0.0.1-SNAPSHOT.jar "${defectdojo_mnt}/.groovy/grapes/io.securecodebox.core/sdk/jars/sdk-0.0.1-SNAPSHOT.jar"
cp -r ./BOOT-INF/lib/ "${defectdojo_mnt}/.groovy/lib/"
popd
cp defectdojo.groovy "${defectdojo_mnt}/code/defectdojo.groovy"
cp importToDefectDojo.groovy "${defectdojo_mnt}/code/importToDefectDojo.groovy"
cp addDependenciesToDescription.groovy "${defectdojo_mnt}/code/addDependenciesToDescription.groovy"

pushd "${defectdojo_mnt}/usr"
mkdir groovy
pushd groovy
curl -L https://dl.bintray.com/groovy/maven/apache-groovy-binary-2.5.8.zip  --output apache-groovy-binary.zip
unzip apache-groovy-binary.zip 
rm apache-groovy-binary.zip
popd

oci_prefix="org.opencontainers.image"
buildah config \
  --label "${oci_prefix}.authors=SDA SE Engineers <engineers@sda-se.io>" \
  --label "${oci_prefix}.url=https://quay.io/sdase/centos" \
  --label "${oci_prefix}.source=https://github.com/SDA-SE/centos" \
  --label "${oci_prefix}.version=0.3.22" \
  --label "${oci_prefix}.revision=$( git rev-parse HEAD )" \
  --label "${oci_prefix}.vendor=SDA SE Open Industry Solutions" \
  --label "${oci_prefix}.licenses=AGPL-3.0" \
  --label "${oci_prefix}.title=CentOS" \
  --label "${oci_prefix}.description=CentOS base image" \
  --env "DD_USER=tpagel" \
  --env 'DD_TOKEN=""' \
  --env 'DD_PRODUCT_NAME=""' \
  --env 'DD_URL="http://localhost:8080"' \
  --env 'DD_REPORT_PATH="/dependency-check-report.xml"' \
  --env 'DD_IMPORT_TYPE="import"' \
  --env 'DD_BRANCH_NAME=""' \
  --env 'DD_LEAD=1' \
  --env 'DD_BUILD_ID="1"' \
  --env 'DD_SOURCE_CODE_MANAGEMENT_URI=""' \
  --env 'DD_BRANCHES_TO_KEEP=""' \
  --cmd "groovy /code/defectdojo.groovy" \
  "${defectdojo_container}"

# Create image
buildah commit --quiet --rm "${defectdojo_container}" "${image}" && defectdojo_container=

if [ -n "${BUILD_EXPORT_OCI_ARCHIVES}" ]
then
    mkdir --parent "${build_dir}"
    buildah push --quiet \
    "localhost/${image}" \
    "oci-archive:${build_dir}/${image//:/-}.tar"
    buildah rmi "${image}"
fi

