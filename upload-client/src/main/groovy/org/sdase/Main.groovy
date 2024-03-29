package org.sdase

public class Main {
    static void main(String[] args) {
        /*
        File sourceFile = new File("importToDefectDojo.groovy");
        Class groovyClass = new GroovyClassLoader(getClass().getClassLoader()).parseClass(sourceFile);
        GroovyObject importToDefectDojo = (GroovyObject) groovyClass.newInstance();
*/
        String productName = System.getenv("DD_PRODUCT_NAME")
        String productNameTemplate = System.getenv("DD_PRODUCT_NAME_TEMPLATE")
        if(productName == null && productNameTemplate != null) {
            println "Using ProductName Template"
            productName=productNameTemplate
                    .replace("###ENVIRONMENT###", System.getenv("ENVIRONMENT"))
                    .replace("###NAMESPACE###", System.getenv("NAMESPACE"))
                    .replace("###APP_NAME###", System.getenv("APP_NAME"))
            if(productName.contains("###")) {
                println "Error: productName ${productName} still contains template variables"
                System.exit(3)
            }
        }

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

        String dojoUrl = System.getenv("DEFECTDOJO_URL")
        if (dojoUrl == null) {
            dojoUrl = System.getenv("DD_URL")
        }
        println "dojoUrl: ${dojoUrl}"
        String token = System.getenv("DEFECTDOJO_APIKEY")
        if(token == null) {
            token = System.getenv("DD_TOKEN")
        }
        String dojoUser = System.getenv("DEFECTDOJO_USERNAME")
        if(dojoUser == null) {
            dojoUser = System.getenv("DD_USER")
        }
        if(!token) {
            println "Error: No token"
            return
        }
        String reportPath = System.getenv("DD_REPORT_PATH") ?: "/tmp/dependency-check-results/dependency-check-report.xml"


        def dh = new File("/tmp/dependency-check-results")
        if(dh.exists()) {
            dh.eachFileRecurse {
                println it
            }
        }


        File report = new File(reportPath)
        if(!report.exists()) {
            println("Report ${reportPath} doesn't exists, exit")
            String exitCodeOnMissingReportString = System.getenv("EXIT_CODE_ON_MISSING_REPORT")
            new File("/tmp/defectDojoTestLink.txt").createNewFile()
            File isFindingFile = new File("/tmp/isFinding")
            isFindingFile.write "skipped"
            File findingsFiles = new File("/tmp/findings.json")
            findingsFiles.write ""
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
        String engagementTagsAsString =  System.getenv("DD_ENGAGEMENT_TAGS")
        List<String> engagementTags = new ArrayList<String>();
        if(engagementTagsAsString && !engagementTagsAsString.isEmpty()) {
            engagementTags = engagementTagsAsString.split(' ')
        }

        String lifecycle = System.getenv("DD_LIFECYCLE") ?: "production"

        def deduplicationOnEngagement = System.getenv("DD_DEDUPLICATION_ON_ENGAGEMENT")
        if(deduplicationOnEngagement == null) deduplicationOnEngagement="true"

        String productType = System.getenv("DD_PRODUCT_TYPE")
        if (productType == null) {
            productType="Research and Development"
        }
        String team = System.getenv("DD_TEAM")

        if(System.getenv("DD_TEAM")) productTags.add("team/" + System.getenv("DD_TEAM"))
        if(System.getenv("ENVIRONMENT")) productTags.add("cluster/" + System.getenv("ENVIRONMENT"))
        if(System.getenv("NAMESPACE")) productTags.add("namespace/" + System.getenv("NAMESPACE"))


        def dependencyTrackUnhandledPackagesMinimumToAlertString = System.getenv("DEPENDENCY_TRACK_UNHANDLED_PACKAGES_MINIMUM_TO_ALERT") ?: '{"npm": {"Critical": 1, "High": 3, "Medium": 9}, "maven": {"Critical": 1, "High": 1, "Medium": 1}, "deb": {"Critical": 1, "High": 3, "Medium": 100}, "rpm": {"Critical": 1, "High": 3, "Medium": 100}}'
        def jsonSlurper = new groovy.json.JsonSlurper()
        def dependencyTrackUnhandledPackagesMinimumToAlert = jsonSlurper.parseText(dependencyTrackUnhandledPackagesMinimumToAlertString)

        String minimumSeverity = System.getenv("DD_MINIMUM_SEVERITY") ?: "High"

        String leadUsername = System.getenv("DD_LEAD_USERNAME") ?: dojoUser
        String testDescription = System.getenv("DD_TEST_DESCRIPTION") ?: ""
        String exitCodeOnFinding = System.getenv("EXIT_CODE_ON_FINDING") ?: "10"
        String isCreateGroups  = System.getenv("IS_CREATE_GROUPS") ?: "true"

        org.sdase.UploadClient.main(
            [
                dojoToken: token,
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
                minimumSeverity: minimumSeverity,
                team: team,
                isCreateGroups: Boolean.parseBoolean(isCreateGroups),
                dependencyTrackUnhandledPackagesMinimumToAlert: dependencyTrackUnhandledPackagesMinimumToAlert,
                engagementTags: engagementTags,
                lifecycle: lifecycle
            ]
        )
    }
}