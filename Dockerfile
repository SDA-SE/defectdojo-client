FROM docker.io/securecodebox/engine:feature-reImport AS scb
RUN \
  cd /scb-engine/ && \
  unzip /scb-engine/app.jar && \
  chown -R 1000:1000 scb-engine/

FROM groovy:3.0-jdk12
LABEL org.opencontainers.image.version=0.3.2

RUN \
  mkdir -p /home/groovy/.groovy/grapes/io.securecodebox.persistenceproviders/defectdojo-persistenceprovider/jars/ && \
  mkdir -p /home/groovy/.groovy/grapes/io.securecodebox.core/sdk/jars/ && \
  mkdir -p /home/groovy/.groovy/lib/

COPY --chown=1000:1000 --from=scb /scb-engine/BOOT-INF/lib/sdk-0.0.1-SNAPSHOT.jar /home/groovy/.groovy/grapes/io.securecodebox.core/sdk/jars/sdk-0.0.1-SNAPSHOT.jar
COPY --chown=1000:1000 --from=scb /scb-engine/BOOT-INF/lib/ /home/groovy/.groovy/lib/
COPY --chown=1000:1000 --from=scb /scb-engine/lib/defectdojo-persistenceprovider-0.0.1-SNAPSHOT-jar-with-dependencies.jar /home/groovy/.groovy/grapes/io.securecodebox.persistenceproviders/defectdojo-persistenceprovider/jars/defectdojo-persistenceprovider-0.0.1-SNAPSHOT.jar

COPY defectdojo.groovy /home/groovy/defectdojo.groovy
COPY importToDefectDojo.groovy /home/groovy/importToDefectDojo.groovy

# COPY with chown is not working for buildah in pipeline
USER root
RUN chown -R 1000:1000 /home/groovy/.groovy/
USER 1000

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

CMD ["groovy","/home/groovy/defectdojo.groovy"]
