#!/usr/bin/env groovy

File sourceFile = new File("importToDefectDojo.groovy");
Class groovyClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(sourceFile);
GroovyObject importToDefectDojo = (GroovyObject) groovyClass.newInstance();

String token = System.getenv("DD_TOKEN")
if(!token) {
  println "Error: No token"
  return
}
String productName = System.getenv("DD_PRODUCT_NAME")
if(!productName) {
  println "Error: No productName"
  return
}

String user = System.getenv("DD_USER") ?: "admin"
String dojoUrl = System.getenv("DD_URL") ?: "http://localhost:8080"

String reportPath = System.getenv("DD_REPORT_PATH") ?: "/dependency-check-report.xml"
String importType = System.getenv("DD_IMPORT_TYPE") ?: "import" // reimport
String branchName = System.getenv("DD_BRANCH_NAME")
if(!branchName) {
  println "Error: No branchName"
  return
}
String leadTemp = System.getenv("DD_LEAD")
if(!leadTemp) {
  leadTemp = "1"
}
long lead = Long.valueOf(leadTemp)
String buildId = System.getenv("DD_BUILD_ID")
String sourceCodeManagementUri = System.getenv("DD_SOURCE_CODE_MANAGEMENT_URI")

String branchesToKeepFromEnv =  System.getenv("DD_BRANCHES_TO_KEEP")

String isMarkedAsActive = System.getenv("DD_IS_MARKED_AS_ACTIVE") ?: false
if(isMarkedAsActive) {
  isMarkedAsActive = isMarkedAsActive.toBoolean()
}
List<String> branchesToKeep;
if(!branchesToKeepFromEnv) {
  println "Error: No DD_BRANCHES_TO_KEEP"
  return
}else {
  branchesToKeep = branchesToKeepFromEnv.replace('"', '').split(' ')
}

importToDefectDojo token: token, 
  user: user,
  dojoUrl: dojoUrl,
  product: productName,
  reportPath: reportPath,
  branchName: branchName,
  lead: lead,
  buildId: buildId,
  sourceCodeManagementUri: sourceCodeManagementUri,
  importType: importType,
  branchesToKeep: branchesToKeep,
  isMarkedAsActive: isMarkedAsActive
