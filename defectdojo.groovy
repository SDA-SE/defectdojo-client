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

String productDescription = System.getenv("DD_PRODUCT_DESCRIPTION") ?: productName

String user = System.getenv("DD_USER") ?: "clusterscanner"
String dojoUrl = System.getenv("DD_URL") ?: "https://defectdojo-test.tools.sda-se.io/"

String reportPath = System.getenv("DD_REPORT_PATH") ?: "/dependency-check-report.xml"
String reportType = System.getenv("DD_REPORT_TYPE") ?: "Dependency Check Scan"

String branchName = System.getenv("DD_BRANCH_NAME")
if(!branchName) {
  println "Error: No branchName"
  return
}

String buildId = System.getenv("DD_BUILD_ID")
String sourceCodeManagementUri = System.getenv("DD_SOURCE_CODE_MANAGEMENT_URI")

String branchesToKeepFromEnv =  System.getenv("DD_BRANCHES_TO_KEEP")

// passing boolean values is not possible
// inactive, because it can be inactive due to beeing a branch, also
String isFindingInactive = "false"
if (System.getenv("DD_IS_MARKED_AS_INACTIVE")) {
  isFindingInactive = System.getenv("DD_IS_MARKED_AS_INACTIVE")
}

//overwrites isFindingInactive
String isMarkedAsActive = System.getenv("DD_IS_MARKED_AS_ACTIVE") ?: "false"
List<String> branchesToKeep;
if(!branchesToKeepFromEnv) {
  println "Error: No DD_BRANCHES_TO_KEEP"
  return
}else {
  branchesToKeep = branchesToKeepFromEnv.replace('"', '').split(' ')
}

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

String leadUsername = System.getenv("DD_LEAD_USERNAME") ?: "clusterscanner"
String testDescription = System.getenv("DD_TEST_DESCRIPTION") ?: ""


importToDefectDojo token: token,
  user: user,
  dojoUrl: dojoUrl,
  productName: productName,
  productDescription: productDescription,
  reportPath: reportPath,
  branchName: branchName,
  buildId: buildId,
  sourceCodeManagementUri: sourceCodeManagementUri,
  branchesToKeep: branchesToKeep,
  isMarkedAsActive: isMarkedAsActive,
  reportType: reportType,
  productTags: productTags,
  deduplicationOnEngagement: deduplicationOnEngagement,
  isFindingInactive: isFindingInactive,
  productTypeName: productType,
  leadUsername: leadUsername,
  testDescription: testDescription

