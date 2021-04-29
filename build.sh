#!/bin/bash
set -xe

if [ $# -ne 7 ]; then
  echo "Parameters are not set correctly"
  exit 1
fi

REGISTRY=$1
ORGANIZATION=$2
IMAGE_NAME=$3
VERSION=$4
REGISTRY_USER=$5
REGISTRY_TOKEN=$6
BUILD_EXPORT_OCI_ARCHIVES=$7

MAJOR=$(echo "${VERSION}" | tr  '.' "\n" | sed -n 1p)
MINOR=$(echo "${VERSION}" | tr  '.' "\n" | sed -n 2p)

echo "Building ${IMAGE_NAME}"

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
build_dir="${dir}/build"

trap cleanup INT EXIT
cleanup() {
  test -n "${scb_container}" && buildah rm "${scb_container}" || true
  test -n "${defectdojo_container}" && buildah rm "${defectdojo_container}" || true
  test -d "${scb_dir_tmp}" && rm -rf "${scb_dir_tmp}" || true
}

image="defectdojo-client"

_base_image="quay.io/sdase/openjdk-runtime:15-hotspot" # -distroless not possible, it is used in jenkins which starts "cat"
defectdojo_container="$(buildah from $_base_image)"
defectdojo_mnt="$(buildah mount "${defectdojo_container}")"

base_image="registry.access.redhat.com/ubi8/ubi-minimal"
ctr_tools="$( buildah from --pull --quiet ${base_image} )"
mnt_tools="$( buildah mount "${ctr_tools}" )"

mkdir "${defectdojo_mnt}/code"
mkdir -p "${defectdojo_mnt}/usr/bin"
mkdir -p "${defectdojo_mnt}/bin"
cp defectdojo.groovy "${defectdojo_mnt}/code/defectdojo.groovy"
cp importToDefectDojo.groovy "${defectdojo_mnt}/code/importToDefectDojo.groovy"
cp addDependenciesToDescription.groovy "${defectdojo_mnt}/code/addDependenciesToDescription.groovy"

#cp "${mnt_tools}/bin/cat" "${defectdojo_mnt}/bin/cat" # needed for jenkins pipeline which starts a container with cat

GROOVY_VERSION=3.0.7
mkdir -p "${defectdojo_mnt}/usr/groovy"
pushd "${defectdojo_mnt}/usr/groovy"
curl -L https://dl.bintray.com/groovy/maven/apache-groovy-binary-$GROOVY_VERSION.zip  --output apache-groovy-binary.zip
unzip apache-groovy-binary.zip
rm apache-groovy-binary.zip
ln -s  /usr/groovy/groovy-$GROOVY_VERSION/bin/groovy ${defectdojo_mnt}/usr/bin/groovy
popd

echo "################################# the following error is not expected, but it still works!"
${defectdojo_mnt}/usr/groovy/groovy-$GROOVY_VERSION/bin/groovy -Dgrape.root=${defectdojo_mnt}/code/.groovy/ importToDefectDojo.groovy || true # download needed libs
chown -R 1001:1001 "${defectdojo_mnt}/code/.groovy"
chmod -R 755 "${defectdojo_mnt}/code/.groovy"

echo "defectdojo:x:1001:1001:OWASP DefectDojo,,,:/code:/usr/sbin/nologin" >> ${defectdojo_mnt}/etc/passwd

touch "${defectdojo_mnt}/code/defectDojoTestLink.txt"
chown 1001:1001 "${defectdojo_mnt}/code/defectDojoTestLink.txt"

touch "${defectdojo_mnt}/code/isFinding"
chown 1001:1001 "${defectdojo_mnt}/code/isFinding"
echo "{}" >> "${defectdojo_mnt}/code/findings.json"
chown 1001:1001 "${defectdojo_mnt}/code/findings.json"


bill_of_materials_hash="$(find ${defectdojo_mnt} -type f -exec md5sum "{}" +  | md5sum | awk "{print $1}")"
version=3.0.1
oci_prefix="org.opencontainers.image"
buildah config \
  --label "${oci_prefix}.authors=SDA SE Engineers <engineers@sda-se.io>" \
  --label "${oci_prefix}.url=https://quay.io/sdase/defectdojo-client" \
  --label "${oci_prefix}.source=https://github.com/SDA-SE/defectdojo-client" \
  --label "${oci_prefix}.version=${version}" \
  --label "${oci_prefix}.revision=$( git rev-parse HEAD )" \
  --label "${oci_prefix}.vendor=SDA SE Open Industry Solutions" \
  --label "${oci_prefix}.licenses=Apache-2.0" \
  --label "${oci_prefix}.title=OWASP DefectDojo Java Client" \
  --label "${oci_prefix}.description=OWASP DefectDojo Java Client made with OWASP SecureCodeBox Java Client" \
  --label "io.sda-se.image.bill-of-materials-hash=${bill_of_materials_hash}" \
  --env "DD_USER=admin" \
  --env 'DD_TOKEN=TODO' \
  --env 'DD_PRODUCT_NAME=test' \
  --env 'DD_PRODUCT_DESCRIPTION=test product' \
  --env 'DD_URL="http://localhost:8080"' \
  --env 'DD_REPORT_PATH="/dependency-check-report.xml"' \
  --env 'DD_REPORT_TYPE="Dependency Check Scan"' \
  --env 'DD_IMPORT_TYPE=import' \
  --env 'DD_BRANCH_NAME="myimage:1.0.0"' \
  --env 'DD_LEAD=1' \
  --env 'DD_TEAM=nobody' \
  --env 'DD_SOURCE_CODE_MANAGEMENT_URI=""' \
  --env 'DD_BRANCHES_TO_KEEP=""' \
  --env 'DD_PRODUCT_TAGS=""' \
  --env 'HOME="/code"' \
  --env 'WORKING_DIR="/code"' \
  --env 'EXIT_CODE_ON_FINDING=10' \
  --user 1001 \
  --entrypoint '' \
  --cmd '/usr/bin/groovy /code/defectdojo.groovy' \
  "${defectdojo_container}"

buildah commit --quiet "${ctr}" "${IMAGE_NAME}:${VERSION}" && ctr=

if [ -n "${BUILD_EXPORT_OCI_ARCHIVES}" ]
then
  mkdir -p "${build_dir}"
  image="docker://${REGISTRY}/${ORGANIZATION}/${IMAGE_NAME}:${VERSION}"
  buildah push --quiet --creds "${REGISTRY_USER}:${REGISTRY_TOKEN}" "${IMAGE_NAME}:${VERSION}" "${image}"

  image="docker://${REGISTRY}/${ORGANIZATION}/${IMAGE_NAME}:${MAJOR}.${MINOR}"
  buildah push --quiet --creds "${REGISTRY_USER}:${REGISTRY_TOKEN}" "${IMAGE_NAME}:${VERSION}" "${image}"

  image="docker://${REGISTRY}/${ORGANIZATION}/${IMAGE_NAME}:${MAJOR}"
  buildah push --quiet --creds "${REGISTRY_USER}:${REGISTRY_TOKEN}" "${IMAGE_NAME}:${VERSION}" "${image}"

  buildah rmi "${IMAGE_NAME}:${VERSION}"
fi

cleanup
