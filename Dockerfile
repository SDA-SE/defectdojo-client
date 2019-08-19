FROM docker.io/securecodebox/engine@sha256:ad9dca1a8f992fdd6bb1250810d73628608098f684585a90a8a89e351bae751d AS scb
RUN \
  cd /scb-engine/ && \
  unzip /scb-engine/app.jar

FROM quay.io/sdase/openjdk-development:12-openj9
LABEL org.opencontainers.image.version=0.3.10

USER root
RUN \
  cd /usr && \
  mkdir groovy && \  
  cd groovy && \  
  curl -L https://dl.bintray.com/groovy/maven/apache-groovy-binary-2.5.8.zip  --output apache-groovy-binary.zip && ls -la && \
  unzip /usr/groovy/apache-groovy-binary.zip && \
  rm *.zip 

RUN \
  mkdir -p /.groovy/grapes/io.securecodebox.persistenceproviders/defectdojo-persistenceprovider/jars/ && \
  mkdir -p /.groovy/grapes/io.securecodebox.core/sdk/jars/ && \
  mkdir -p /.groovy/lib/ && \
  chown -R 999:999 /.groovy/

USER 999
COPY --chown=999:999 --from=scb /scb-engine/BOOT-INF/lib/sdk-0.0.1-SNAPSHOT.jar /.groovy/grapes/io.securecodebox.core/sdk/jars/sdk-0.0.1-SNAPSHOT.jar
COPY --chown=999:999 --from=scb /scb-engine/BOOT-INF/lib/ /.groovy/lib/
COPY --chown=999:999 --from=scb /scb-engine/lib/defectdojo-persistenceprovider-0.0.1-SNAPSHOT-jar-with-dependencies.jar /.groovy/grapes/io.securecodebox.persistenceproviders/defectdojo-persistenceprovider/jars/defectdojo-persistenceprovider-0.0.1-SNAPSHOT.jar

COPY defectdojo.groovy /code/defectdojo.groovy
COPY importToDefectDojo.groovy /code/importToDefectDojo.groovy

ENV \
  DD_USER="tpagel" \
  DD_TOKEN="" \
  DD_PRODUCT_NAME="" \
  DD_USER="" \
  DD_URL="http://localhost:8080" \
  DD_REPORT_PATH="/dependency-check-report.xml" \
  DD_IMPORT_TYPE="import" \
  DD_BRANCH_NAME="" \
  DD_LEAD=1 \
  DD_BUILD_ID="1" \
  DD_SOURCE_CODE_MANAGEMENT_URI=""  \
  DD_BRANCHES_TO_KEEP=""

CMD ["groovy","/code/defectdojo.groovy"]
