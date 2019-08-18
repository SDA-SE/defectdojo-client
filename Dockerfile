FROM docker.io/securecodebox/engine:feature-reImport AS scb
RUN \
  cd /scb-engine/ && \
  unzip /scb-engine/app.jar && \
  chown -R 1000:1000 /scb-engine/

FROM quay.io/sdase/openjdk-development:12-openj9
LABEL org.opencontainers.image.version=0.3.3

USER root
RUN \
  cd /usr && \
  mkdir groovy && \  
  cd groovy && \  
  curl -L https://dl.bintray.com/groovy/maven/apache-groovy-binary-2.5.8.zip  --output apache-groovy-binary.zip && ls -la && \
  unzip /usr/groovy/apache-groovy-binary.zip && \
  rm *.zip

USER 999
RUN \
  mkdir -p /code/.groovy/grapes/io.securecodebox.persistenceproviders/defectdojo-persistenceprovider/jars/ && \
  mkdir -p /code/.groovy/grapes/io.securecodebox.core/sdk/jars/ && \
  mkdir -p /code/.groovy/lib/

COPY --chown=999:1000 --from=scb /scb-engine/BOOT-INF/lib/sdk-0.0.1-SNAPSHOT.jar /code/.groovy/grapes/io.securecodebox.core/sdk/jars/sdk-0.0.1-SNAPSHOT.jar
COPY --chown=999:1000 --from=scb /scb-engine/BOOT-INF/lib/ /code/.groovy/lib/
COPY --chown=999:1000 --from=scb /scb-engine/lib/defectdojo-persistenceprovider-0.0.1-SNAPSHOT-jar-with-dependencies.jar /code/.groovy/grapes/io.securecodebox.persistenceproviders/defectdojo-persistenceprovider/jars/defectdojo-persistenceprovider-0.0.1-SNAPSHOT.jar

COPY defectdojo.groovy /code/defectdojo.groovy
COPY importToDefectDojo.groovy /code/importToDefectDojo.groovy

# COPY with chown is not working for buildah in pipeline
USER root
RUN chown -R 999:1000 /code/.groovy/
USER 999

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
  DD_SOURCE_CODE_MANAGEMENT_URI=""

CMD ["groovy","/code/defectdojo.groovy"]
