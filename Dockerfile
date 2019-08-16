FROM docker.io/securecodebox/engine:feature-reImport AS scb
RUN cd /scb-engine/ ; unzip /scb-engine/app.jar

FROM groovy:3.0-jdk12
LABEL org.opencontainers.image.version=0.3.0

COPY --from=scb /scb-engine/BOOT-INF/lib/sdk-0.0.1-SNAPSHOT.jar /home/groovy/.groovy/grapes/io.securecodebox.core/sdk/jars/sdk-0.0.1-SNAPSHOT.jar
COPY --from=scb /scb-engine/BOOT-INF/lib/ /home/groovy/.groovy/lib/
COPY --from=scb /scb-engine/lib/defectdojo-persistenceprovider-0.0.1-SNAPSHOT-jar-with-dependencies.jar /home/groovy/.groovy/grapes/io.securecodebox.persistenceproviders/defectdojo-persistenceprovider/jars/defectdojo-persistenceprovider-0.0.1-SNAPSHOT.jar

COPY defectdojo.groovy /home/groovy/defectdojo.groovy
COPY importToDefectDojo.groovy /home/groovy/importToDefectDojo.groovy

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