#!/usr/bin/env groovy
import io.securecodebox.persistence.defectdojo.ScanType
import io.securecodebox.persistence.defectdojo.TestType
@GrabConfig(systemClassLoader=true)
@Grab(group='com.fasterxml.jackson.core', module='jackson-core', version='2.9.9')
@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.9.9.2')
@Grab(group='org.codehaus.jackson', module='jackson-mapper-asl', version='1.9.13')
@Grab(group= 'org.springframework', module='spring-web', version='5.2.12.RELEASE')
@GrabResolver(name='maven-snapshot', root='https://oss.sonatype.org/content/repositories/snapshots/')
@Grab("io.securecodebox:defectdojo-client:0.0.3-SNAPSHOT")

import io.securecodebox.persistence.defectdojo.config.DefectDojoConfig
import io.securecodebox.persistence.defectdojo.models.Engagement
import io.securecodebox.persistence.defectdojo.models.Finding
import io.securecodebox.persistence.defectdojo.models.Product
import io.securecodebox.persistence.defectdojo.models.ProductType
import io.securecodebox.persistence.defectdojo.models.Test
import io.securecodebox.persistence.defectdojo.models.User
import io.securecodebox.persistence.defectdojo.service.EngagementService
import io.securecodebox.persistence.defectdojo.service.FindingService
import io.securecodebox.persistence.defectdojo.service.ImportScanService
import io.securecodebox.persistence.defectdojo.service.ProductService;
import io.securecodebox.persistence.defectdojo.service.ProductTypeService;
import io.securecodebox.persistence.defectdojo.service.TestService
import io.securecodebox.persistence.defectdojo.service.UserService

import java.util.stream.Stream;

def call(args) {
    def conf = new DefectDojoConfig(args.dojoUrl, args.dojoToken, args.dojoUser);
    def productTypeService = new ProductTypeService(conf);
    def productService = new ProductService(conf);
    def engagementService = new EngagementService(conf)
    def testService = new TestService(conf)
    def userService = new UserService(conf)
    def findingService = new FindingService(conf)
    def importScanService = new ImportScanService(conf)

    def productType = productTypeService.searchUnique(ProductType.builder().name(args.productTypeName).build()).orElseGet {
        return productTypeService.create(
                ProductType.builder()
                        .name(args.productTypeName)
                        .build()
        );
    }

    def product = productService.searchUnique(Product.builder().name(args.productName).productType(productType.id).build()).orElseGet {
        return productService.create(
                Product.builder()
                        .name(args.productName)
                        .description(args.productDescription)
                        .productType(productType.id)
                        .tags(args.productTags)
                        .build()
        );
    }

    System.out.println("Created or found Product: " + product.name + ", id :" + product.id);

    def engagementObj = Engagement.builder()
        .name("Dep Check " + args.branchName)
        .branch(args.branchName)
        .buildID(args.buildId)
        .deduplicationOnEngagement(args.deduplicationOnEngagement.toBoolean())
        .repo(args.sourceCodeManagementUri)
        .product(product.id)
        .build()

    def date = new Date()
    def dateNow = date.format("yyyy-MM-dd")
    def timeNow = date.format("HH:mm:ss")

    def engagement = engagementService.searchUnique(engagementObj).orElseGet {
        engagementObj.setTargetStart(dateNow)
        engagementObj.setTargetEnd(dateNow)

        return engagementService.create(engagementObj);
    }

    //TODO Test with reimport

    def leadUser = userService.searchUnique(User.builder().username(args.leadUsername).build()).orElseThrow {
        return new RuntimeException("Failed to find user '${args.leadUsername}' in DefectDojo")
    }

    def test = testService.create(
            Test.builder()
                    .title(args.scanType)
                    .targetStart(dateNow + " " + timeNow)
                    .targetEnd(dateNow + " " + timeNow)
                    .engagement(engagement.id)
                    .percentComplete(100L)
                    .lead(leadUser.id)
                    .testType(TestType.STATIC_CHECK.id)
                    .description(args.testDescription)
                    .build()
    )

    String reportContents = new File(args.reportPath).text

    // In DefectDojo Version 1.5.4 you can specify test_type/testName; BE AWARE: close_old_findings will not work by using something else than reportType
    ScanType scanType;
    for(ScanType scanTypeMatch : ScanType.values()) {
        if(scanTypeMatch.getTestType() == args.scanType) {
            scanType = scanTypeMatch;
        }
    }
    def response = importScanService.reimportScan(
            reportContents,
            test.id,
            leadUser.id,
            dateNow,
            scanType,
            TestType.STATIC_CHECK
    )

    println("Uploaded Finding.")

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
    def findings = findingService.getUnhandledFindingsForEngagement(engagement.id, minimumSeverity)

    println("Got ${findings.size()} unhandled findings")

    def defectDojoTestLink = args.dojoUrl + "/test/" + test.id;

    File file = new File("/code/defectDojoTestLink.txt")
    file.write defectDojoTestLink
    println "DefectDojo test with scan results can be viewed at $defectDojoTestLink"

    if(findings.size() > 0) {
        // Mark build as unstable
        println "${findings.size()} vulnerabilities found with severity $minimumSeverity or higher"
        System.exit(10)
    }
}
