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
  test -n "${ctr_tools}" && buildah rm "${ctr_tools}" || true
  test -n "${defectdojo_container}" && buildah rm "${defectdojo_container}" || true
}

_base_image="quay.io/sdase/defectdojo-client:3"
defectdojo_container="$(buildah from $_base_image)"
defectdojo_mnt="$(buildah mount "${defectdojo_container}")"

cp defectdojo.groovy "${defectdojo_mnt}/code/defectdojo.groovy"

# Get a bill of materials
bill_of_materials="$(buildah run --volume "${mnt}":/mnt "${ctr_tools}" -- /usr/bin/rpm \
  --query \
  --all \
  --queryformat "%{NAME} %{VERSION} %{RELEASE} %{ARCH}" \
  --dbpath="/mnt/var/lib/rpm" \
  | sort )"
echo "bill_of_materials: ${bill_of_materials}";
# Get bill of materials hash – the content
# of this script is included in hash, too.
bill_of_materials_hash="$( ( cat "${0}";
  echo "${bill_of_materials}"; \
  cat ./*;
  ) | sha256sum | awk '{ print $1 }' )"

oci_prefix="org.opencontainers.image"
buildah config \
  --label "${oci_prefix}.authors=SDA SE Engineers <engineers@sda-se.io>" \
  --label "${oci_prefix}.url=https://quay.io/sdase/defectdojo-client" \
  --label "${oci_prefix}.source=https://github.com/SDA-SE/defectdojo-client" \
  --label "${oci_prefix}.version=${VERSION}" \
  --label "${oci_prefix}.revision=$( git rev-parse HEAD )" \
  --label "${oci_prefix}.vendor=SDA SE Open Industry Solutions" \
  --label "${oci_prefix}.licenses=MIT" \
  --label "${oci_prefix}.title=OWASP DefectDojo Statistic generation" \
  --label "${oci_prefix}.description=OWASP DefectDojo Java Client to create statistics for teams made with OWASP SecureCodeBox Java Client" \
  --label "io.sda-se.image.bill-of-materials-hash=${bill_of_materials_hash}" \
  --env "DD_USER=clusterscanner" \
  --env 'DD_TOKEN=' \
  --env 'STATISTIC_FILE_=/tmp/team-response-statistics.csv' \
  --env 'DD_URL="http://localhost:8080"' \
  --env 'HOME="/code"' \
  --env 'WORKING_DIR="/code"' \
  --user 1001 \
  --entrypoint '' \
  --cmd '/usr/bin/groovy /code/defectdojo.groovy' \
  "${defectdojo_container}"

buildah commit --quiet "${defectdojo_container}" "${IMAGE_NAME}:${VERSION}" && defectdojo_container=

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
