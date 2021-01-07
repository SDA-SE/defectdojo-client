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

_base_image="quay.io/sdase/openjdk-runtime:15-hotspot-distroless"
defectdojo_container="$(buildah from $_base_image)"
defectdojo_mnt="$(buildah mount "${defectdojo_container}")"

mkdir "${defectdojo_mnt}/code"
mkdir -p "${defectdojo_mnt}/usr/bin"
cp defectdojo.groovy "${defectdojo_mnt}/code/defectdojo.groovy"
cp importToDefectDojo.groovy "${defectdojo_mnt}/code/importToDefectDojo.groovy"
cp addDependenciesToDescription.groovy "${defectdojo_mnt}/code/addDependenciesToDescription.groovy"

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
chown -R 999:999 "${defectdojo_mnt}/code/.groovy"

echo "defectdojo:x:999:999:OWASP DefectDojo,,,:/code:/usr/sbin/nologin" >> ${defectdojo_mnt}/etc/passwd

version=2.0.21
oci_prefix="org.opencontainers.image"
buildah config \
  --label "${oci_prefix}.authors=SDA SE Engineers <engineers@sda-se.io>" \
  --label "${oci_prefix}.url=https://quay.io/sdase/defectdojo-client" \
  --label "${oci_prefix}.source=https://github.com/SDA-SE/defectdojo-client" \
  --label "${oci_prefix}.version=${version}" \
  --label "${oci_prefix}.revision=$( git rev-parse HEAD )" \
  --label "${oci_prefix}.vendor=SDA SE Open Industry Solutions" \
  --label "${oci_prefix}.licenses=Apache-2.0" \
  --label "${oci_prefix}.title=OWASP DefectDojo Client" \
  --label "${oci_prefix}.description=OWASP DefectDojo Client" \
  --label "io.sda-se.image.bill-of-materials-hash=${version}" \
  --env "DD_USER=admin" \
  --env 'DD_TOKEN=""' \
  --env 'DD_PRODUCT_NAME=""' \
  --env 'DD_URL="http://localhost:8080"' \
  --env 'DD_REPORT_PATH="/dependency-check-report.xml"' \
  --env 'DD_IMPORT_TYPE="import"' \
  --env 'DD_BRANCH_NAME=""' \
  --env 'DD_LEAD=1' \
  --env 'DD_PRODUCT_TYPE=1' \
  --env 'DD_BUILD_ID="1"' \
  --env 'DD_SOURCE_CODE_MANAGEMENT_URI=""' \
  --env 'DD_BRANCHES_TO_KEEP=""' \
  --env 'DD_PRODUCT_TAGS=""' \
  --env 'HOME="/code"' \
  --user 999 \
  --cmd "/usr/bin/groovy /code/defectdojo.groovy" \
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
