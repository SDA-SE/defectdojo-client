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

String branchName = System.getenv("DD_BRANCH_NAME")
if(!branchName) {
  println "Error: No branchName"
  return
}

String productDescription = System.getenv("DD_PRODUCT_DESCRIPTION") ?: productName

String dojoUser = System.getenv("DD_USER") ?: "clusterscanner"
String dojoUrl = System.getenv("DD_URL") ?: "https://localhost:8080/"

String reportPath = System.getenv("DD_REPORT_PATH") ?: "/dependency-check-report-10.xml"
String scanType = System.getenv("DD_REPORT_TYPE") ?: "Dependency Check Scan"

String sourceCodeManagementUri = System.getenv("DD_SOURCE_CODE_MANAGEMENT_URI")

String branchesToKeepFromEnv =  System.getenv("DD_BRANCHES_TO_KEEP") ?: "*"
List<String> branchesToKeep = branchesToKeepFromEnv.replace('"', '').split(' ')

String tagsAsString =  System.getenv("DD_PRODUCT_TAGS")
List<String> productTags;
if(tagsAsString) {
  productTags = tagsAsString.split(' ')
} else {
  productTags = java.util.Collections.emptyList();
}

String deduplicationOnEngagement = "true"
if(System.getenv("DD_DEDUPLICATION_ON_ENGAGEMENT")) {
  deduplicationOnEngagement = System.getenv("DD_DEDUPLICATION_ON_ENGAGEMENT")
}

String productType = "unset-team"
if(System.getenv("DD_TEAM") && !System.getenv("DD_TEAM").isEmpty()) {
  productType = System.getenv("DD_TEAM")
  productTags.add(System.getenv("DD_TEAM"))
}

String leadUsername = System.getenv("DD_LEAD_USERNAME") ?: dojoUser
String testDescription = System.getenv("DD_TEST_DESCRIPTION") ?: ""


importToDefectDojo dojoToken: token,
  dojoUser: dojoUser,
  dojoUrl: dojoUrl,
  productName: productName,
  productDescription: productDescription,
  reportPath: reportPath,
  branchName: branchName,
  sourceCodeManagementUri: sourceCodeManagementUri,
  branchesToKeep: branchesToKeep,
  scanType: scanType,
  productTags: productTags,
  deduplicationOnEngagement: deduplicationOnEngagement,
  productTypeName: productType,
  leadUsername: leadUsername,
  testDescription: testDescription

