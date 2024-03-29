package org.sdase.uploadValidation

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

class UploadValidator {
    static void main(String dojoToken, String dojoUser, String dojoUrl) {
        def conf = new DefectDojoConfig(dojoUrl, dojoToken, dojoUser, 200);
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


        URL resource = UploadValidator.getClassLoader().getResource("expectedFindings.json");

        File file = new File(resource.getPath())
        String fileContent = file.text
        def jsonSlurper = new groovy.json.JsonSlurper()
        def expectedFindings = jsonSlurper.parseText(fileContent)

        for(expectedFinding in expectedFindings) {
            def foundToSearchFinding= 0
            println "Searching for productName: ${expectedFinding.productName} with findingTitle ${expectedFinding.findingTitle}"


            def product = productService.searchUnique(Product.builder().name(expectedFinding.productName).build()).orElseThrow{
                new Exception("Could not find product with name '" + expectedFinding.productName + "' in DefectDojo API. DefectDojo might be running in an unsupported version.")
            };

            Map<String, String> queryParamsEng = new HashMap<>();
            queryParamsEng.put("product", product.id);
            def engagements = engagementService.search(queryParamsEng);
            for (eng in engagements) {
                println("found engagement ${eng.id}")
                Map<String, String> queryParamsTest = new HashMap<>();
                queryParamsTest.put("engagement", Long.toString(eng.id))
                def tests = testService.search(queryParamsTest);

                for(Test test in tests) {
                    Map<String, String> queryParamsFinding = new HashMap<>();
                    queryParamsFinding.put("test", test.getId());
                    queryParamsFinding.put("duplicate", "false");
                    queryParamsFinding.put("active", "true");
                    queryParamsFinding.put("title", expectedFinding.findingTitle);
                    println("searching for findings in test ${test.getId()} with title ${expectedFinding.findingTitle}")
                    findingService.search(queryParamsFinding).each {
                        if(it.title.startsWith(expectedFinding.findingTitle)) {
                            println "found finding ${it.id}, ${it.title} in ${test.getId()}"
                            foundToSearchFinding++
                        }
                    }
                }
            }
            if(foundToSearchFinding != expectedFinding.quantity) {
                print "Error, foundToSearchFinding != expectedFinding.quantity: ${foundToSearchFinding} != ${expectedFinding.quantity}"
                System.exit(1)
            }
        }
    }
}
