#!/usr/bin/env groovy
package uploadClient

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

String reportPath = System.getenv("DD_REPORT_PATH") ?: "/tmp/dependency-check-results/dependency-check-report.xml"


dh = new File("/tmp/dependency-check-results")
if(dh.exists()) {
  dh.eachFileRecurse {
      println it
  }
}


File report = new File(reportPath)
if(!report.exists()) {
  println("Report ${reportPath} doesn't exists, exit")
  String exitCodeOnMissingReportString = System.getenv("EXIT_CODE_ON_MISSING_REPORT")
  int exitCodeOnMissingReport = 2
  if(exitCodeOnMissingReportString) exitCodeOnMissingReport = System.getenv("EXIT_CODE_ON_MISSING_REPORT").toInteger()
  System.exit(exitCodeOnMissingReport)
}
String scanType = System.getenv("DD_REPORT_TYPE") ?: "Dependency Check Scan"
println "using scanType ${scanType}"
String sourceCodeManagementUri = System.getenv("DD_SOURCE_CODE_MANAGEMENT_URI") ?: "https://github.com/SDA-SE/setme"

String branchesToKeepFromEnv =  System.getenv("DD_BRANCHES_TO_KEEP") ?: "*"
List<String> branchesToKeep = branchesToKeepFromEnv.replace('"', '').split(' ')

String tagsAsString =  System.getenv("DD_PRODUCT_TAGS")
List<String> productTags = new ArrayList<String>();
if(tagsAsString && !tagsAsString.isEmpty()) {
  productTags = tagsAsString.split(' ')
}

deduplicationOnEngagement = System.getenv("DD_DEDUPLICATION_ON_ENGAGEMENT")

String productType = System.getenv("DD_TEAM")
if(!productType) productType="nobody"
if(System.getenv("DD_TEAM")) productTags.add("team/" + System.getenv("DD_TEAM"))
if(System.getenv("ENVIRONMENT")) productTags.add("cluster/" + System.getenv("ENVIRONMENT"))
if(System.getenv("NAMESPACE")) productTags.add("namespace/" + System.getenv("NAMESPACE"))

String minimumSeverity = System.getenv("DD_MINIMUM_SEVERITY") ?: "High"

String leadUsername = System.getenv("DD_LEAD_USERNAME") ?: dojoUser
String testDescription = System.getenv("DD_TEST_DESCRIPTION") ?: ""
String exitCodeOnFinding = System.getenv("EXIT_CODE_ON_FINDING") ?: "10"

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
  testDescription: testDescription,
  exitCodeOnFinding: exitCodeOnFinding,
  minimumSeverity: minimumSeverity

