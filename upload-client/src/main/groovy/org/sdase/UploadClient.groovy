/*
 * This Groovy source file was generated by the Gradle 'init' task.
 */
package org.sdase

import io.securecodebox.persistence.defectdojo.config.DefectDojoConfig
import io.securecodebox.persistence.defectdojo.models.Finding
import io.securecodebox.persistence.defectdojo.service.EndpointService
import io.securecodebox.persistence.defectdojo.service.EngagementService
import io.securecodebox.persistence.defectdojo.service.FindingService
import io.securecodebox.persistence.defectdojo.service.ProductService;
import io.securecodebox.persistence.defectdojo.service.ProductTypeService;
import io.securecodebox.persistence.defectdojo.service.TestService
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import io.securecodebox.persistence.defectdojo.config.DefectDojoConfig
import io.securecodebox.persistence.defectdojo.models.Engagement
import io.securecodebox.persistence.defectdojo.models.Finding
import io.securecodebox.persistence.defectdojo.models.Product
import io.securecodebox.persistence.defectdojo.models.ProductType
import io.securecodebox.persistence.defectdojo.models.ScanFile
import io.securecodebox.persistence.defectdojo.models.Test
import io.securecodebox.persistence.defectdojo.models.TestType
import io.securecodebox.persistence.defectdojo.models.User
import io.securecodebox.persistence.defectdojo.models.DojoGroup
import io.securecodebox.persistence.defectdojo.models.ProductGroup

import io.securecodebox.persistence.defectdojo.service.EngagementService
import io.securecodebox.persistence.defectdojo.service.FindingService
import io.securecodebox.persistence.defectdojo.service.ImportScanService
import io.securecodebox.persistence.defectdojo.service.ProductService;
import io.securecodebox.persistence.defectdojo.service.ProductTypeService;
import io.securecodebox.persistence.defectdojo.service.TestService
import io.securecodebox.persistence.defectdojo.service.TestTypeService
import io.securecodebox.persistence.defectdojo.service.UserService
import io.securecodebox.persistence.defectdojo.service.DojoGroupService
import io.securecodebox.persistence.defectdojo.service.ProductGroupService
import io.securecodebox.persistence.defectdojo.ScanType
import java.util.Calendar;

class UploadClient {
    private static String extractPackageManager(String filePath) {
        return filePath.replace("pkg:", "").replaceAll("/.*", "")
    }

    static void main(Object args) {
        def conf = new DefectDojoConfig(args.dojoUrl, args.dojoToken, args.dojoUser, 200);
        def productTypeService = new ProductTypeService(conf);
        def productService = new ProductService(conf);
        def engagementService = new EngagementService(conf)
        def testService = new TestService(conf)
        def testTypeService = new TestTypeService(conf)
        def userService = new UserService(conf)
        def findingService = new FindingService(conf)
        def importScanService = new ImportScanService(conf)
        def dojoGroupService = new DojoGroupService(conf)
        def productGroupService = new ProductGroupService(conf)

        def leadUser = userService.searchUnique(User.builder().username(args.leadUsername).build()).orElseThrow {
            return new RuntimeException("Failed to find user '${args.leadUsername}' in DefectDojo")
        }

        def productType = productTypeService.searchUnique(ProductType.builder().name(args.productTypeName).build()).orElseGet {
            return productTypeService.create(
                    ProductType.builder()
                            .name(args.productTypeName)
                            .build()
            );
        }

        println("Will fetch or create ${args.productName}")
        def product = productService.searchUnique(Product.builder().name(args.productName).build()).orElseGet {
            return productService.create(
                    Product.builder()
                            .name(args.productName)
                            .description(args.productDescription)
                            .productType(productType.id)
                            .tags(args.productTags)
                            .enableSimpleRiskAcceptance(true)
                            .build()
            );
        }
        def dojoGroup = dojoGroupService.searchUnique(DojoGroup.builder().name(args.team).build()).orElseGet {
            if(args.isCreateGroups) {
                return dojoGroupService.create(
                        DojoGroup.builder()
                                .name(args.team)
                                .description("created via ClusterImageScanner")
                                .socialProvider("AzureAD")
                                .build()
                );
            }
            return null
        }
        if(dojoGroup == null) {
            throw new Exception("Couldn't get or create DojoGroup. DojoGroup ${args.team} doesn't exit and IS_CREATE_GROUPS is " + args.isCreateGroups)
        }
        println("Using dojoGroup ${dojoGroup.getId()}")
        def productGroup = productGroupService.searchUnique(ProductGroup.builder().group(dojoGroup.getId()).product(product.getId()).build()).orElseGet {
            return productGroupService.create(
                    ProductGroup.builder()
                            .group(dojoGroup.getId())
                            .product(product.getId())
                            .role(3) // 3=maintainer
                            .build()
            );
        }
        println("Will update product")
        if(!args.productDescription && product.getDescription() != args.productDescription) {
            product.setDescription(args.productDescription);
        }
        product.setProductType(productType.id)
        List<String> existingProductTags = product.getTags();
        List<String> tagsToBeSet = new ArrayList<>();
        for (int i = 0; i < existingProductTags.size(); i++) {
            if(existingProductTags.get(i).contains("team")) { // refresh team every time
                println("Not adding tag ${existingProductTags.get(i)}, ${i}")
            }else {
                tagsToBeSet.add(existingProductTags.get(i))
            }
        }
        tagsToBeSet.addAll(args.productTags)
        tagsToBeSet.unique()
        product.setTags(tagsToBeSet)
        println "Set product tags ${tagsToBeSet}"
        product.setEnableSimpleRiskAcceptance(true)
        productService.update(product, product.getId())

        System.out.println("Created or found Product: " + product.name + ", id :" + product.id);
        def branchParameter
        if(args.branchName.contains(":")) {
            branchParameter = args.branchName.split(":")
        } else {
            branchParameter = [args.branchName, args.branchName] // name, test title
        }
        def date = new Date()
        final DateFormat formatDay = new SimpleDateFormat("yyyy-MM-dd");
        final String dateNow = formatDay.format(date);
        final DateFormat formatTime = new SimpleDateFormat("HH:mm:ss");
        final String timeNow = formatTime.format(date);
        Calendar engagementEndDate = Calendar.getInstance();
        engagementEndDate.setTime(date);
        engagementEndDate.add(Calendar.DATE, 7);

        final String engagementEndDateAsString = formatDay.format(engagementEndDate.getTime());

        def engagementObj = Engagement.builder()
                .name(args.scanType + " | " + branchParameter[0])
                .branch(branchParameter[0])
                .description(branchParameter[0])
                .deduplicationOnEngagement(args.deduplicationOnEngagement.toBoolean())
                .repo(args.sourceCodeManagementUri)
                .product(product.id)
                .lead(leadUser.id)
                .build()

        def engagement = engagementService.searchUnique(engagementObj).orElseGet {
            engagementObj.setTargetStart(dateNow)
            engagementObj.setTargetEnd(engagementEndDateAsString)

            return engagementService.create(engagementObj);
        }
        engagement.setTargetEnd(engagementEndDateAsString)
        engagementService.update(engagement, engagement.id);


        ScanFile reportContents = new ScanFile(new File(args.reportPath).text);
        // In DefectDojo Version 1.5.4 you can specify test_type/testName; BE AWARE: close_old_findings will not work by using something else than reportType
        ScanType scanType;
        for(ScanType scanTypeMatch : ScanType.values()) {
            if(scanTypeMatch.getTestType() == args.scanType) {
                scanType = scanTypeMatch;
            }
        }

        TestType testType = testTypeService.searchUnique(TestType.builder().name(ScanType.STATIC_CHECK.getTestType()).build())
                .orElseThrow{
                    new Exception("Could not find test type '" + ScanType.STATIC_CHECK.getTestType() + "' in DefectDojo API. DefectDojo might be running in an unsupported version.")
                };

        def response = importScanService.importScan(
                reportContents,
                engagement.id,
                leadUser.id,
                dateNow,
                scanType,
                testType.getId()
        )
        println("Uploaded Finding.")

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", String.valueOf(response.getTestId()));
        println "test ${ String.valueOf(response.getTestId())}"

        Test test = testService.search(queryParams).first();
        test.setDescription("Date: " + dateNow + " " + timeNow + "\nImage: " + branchParameter[0] + "\nTag: " + branchParameter[1] +"\nTest Scan Type: " + scanType.getTestType() + "\n" + args.testDescription)
        test.setTitle(dateNow + " " + timeNow + " | " + branchParameter[1] +" (" + scanType.getTestType() + ")");
        testService.update(test, test.getId());
        println("Changed Test Name.")


        // Delete engagements for deleted branches
        List<String> branchesToKeep = args.branchesToKeep
        if (!branchesToKeep.contains("*")) {
            engagementService.search([ product: Long.toString(product.id) ]).each {
                println("Engagement: '${it.name}', Branch: '${it.branch}'")

                if (branchesToKeep.contains(it.branch)) {
                    return
                }

                println("Deleting Engagment(${it.name}) for branch ${it.branch}")
                engagementService.delete(it.id)
            }
        }
        def minimumSeverity = Finding.Severity.High
        switch (args.minimumSeverity) {
            case "Critical":
                minimumSeverity = Finding.Severity.Critical
                break;
            case "High":
                minimumSeverity = Finding.Severity.High
                break
            case "Medium":
                minimumSeverity = Finding.Severity.Medium
                break
            case "Low":
                minimumSeverity = Finding.Severity.Low
                break
            case "Informational":
                minimumSeverity = Finding.Severity.Informational
                break
            default:
                println("Error, minimumSeverity '${args.minimumSeverity}' doesn't exist")
                break
        }
        def findings = findingService.getUnhandledFindingsForEngagement(engagement.id, minimumSeverity)
        def isDependencyTrackFinding = false
        def findingCountsPerSeverity = new HashMap<String, Integer>()
        if(args.scanType.equals("Dependency Track Finding Packaging Format (FPF) Export")) {
            println("Got ${findings.size()} unhandled Dep. Track findings, will check for severity filter")
            for(finding in findings) {
                def packageManager = extractPackageManager(finding.filePath)
                def count = findingCountsPerSeverity.getOrDefault("${packageManager}_${finding.severity}", 0)
                count++
                findingCountsPerSeverity.put("${packageManager}_${finding.severity}", count)
            }

            println "findingCountsPerSeverity: ${findingCountsPerSeverity}, findings.size(): ${findings.size()}"
            for(int i=findings.size()-1; i>=0 ;i--) {
                def isDelete= false
                def finding = findings.get(i)
                def packageManager = extractPackageManager(finding.filePath)
                //println "packageManager: ${packageManager}"
                //println "dependencyTrackUnhandledPackagesMinimumToAlert: ${args.dependencyTrackUnhandledPackagesMinimumToAlert}"
                def severitiesForPackageManager = args.dependencyTrackUnhandledPackagesMinimumToAlert.get(packageManager)
                //def minimumToWarnForThisSeverity = unhandledLanguageFilter2.value.get(finding.severity.toString())
                //def findingCountPerSeverity = findingCount.value
                def minimumToWarnForThisSeverity = 999999999999999999
                if(severitiesForPackageManager == null) {
                    println "ERROR: Package manager '${packageManager}' is not defined in DEPENDENCY_TRACK_UNHANDLED_PACKAGES_MINIMUM_TO_ALERT!"
                }else {
                    minimumToWarnForThisSeverity = severitiesForPackageManager.get(finding.severity.toString())
                }
                def findingCount = findingCountsPerSeverity.get("${packageManager}_" + finding.severity.toString())
                //println "${findingCountPerSeverity} >= ${minimumToWarnForThisSeverity}"
                if(findingCount >= minimumToWarnForThisSeverity) {
                    //println "keeping ${finding.id} with package manager ${packageManager} with severity ${finding.severity} index: ${i}"
                } else {
                    //println "removing ${finding.id} with package manager ${packageManager} with severity ${finding.severity} index: ${i}"
                    findings.remove(i)
                }
            }
        }
        println("Got ${findings.size()} unhandled findings")
        def defectDojoTestLink = args.dojoUrl + "/test/" + response.getTestId();

        File file = new File("/tmp/defectDojoTestLink.txt")
        file.write defectDojoTestLink
        println "DefectDojo test with scan results can be viewed at $defectDojoTestLink"

        File isFindingFile = new File("/tmp/isFinding")
        File findingsFiles = new File("/tmp/findings.json")
        def serializedFindings = groovy.json.JsonOutput.toJson(findings)
        findingsFiles.write serializedFindings
        if(findings.size() > 0) {
            // Mark build as unstable in Jenkins via exit code
            println "${findings.size()} vulnerabilities found with severity $minimumSeverity or higher"

            isFindingFile.write "true"
            println "Exiting with exitCodeOnFinding " + args.exitCodeOnFinding.toInteger()
            System.exit(args.exitCodeOnFinding.toInteger())
        } else {
            isFindingFile.write "false"
        }
    }
}