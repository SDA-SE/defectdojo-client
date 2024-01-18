package org.sdase.deleteProduct

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
import java.util.stream.Collectors

public class DeleteProducts {
    static void main(String[] args) {
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
        String productNameToDelete = System.getenv("PRODUCT_NAME_TO_DELETE")
        if(!token) {
            println "Error: No PRODUCT_NAME_TO_DELETE"
            return
        }

        def configuration = new DefectDojoConfig(dojoUrl, token, dojoUser, 200);

// delete all products based on name

        Map<String, String> queryParameter = new HashMap<>();
        queryParameter.put("name", productNameToDelete);
        deleteAllProducts(configuration, queryParameter)
    }
    def deleteAllProducts(conf, Map<String, String> queryParams) {
        def productTypeService = new ProductTypeService(conf);
        def productService = new ProductService(conf);
        def testService = new TestService(conf);
        def engagagementService = new EngagementService(conf)
        def endpointService = new EndpointService(conf)
        def findingService = new FindingService(conf)
        def products = productService.search(queryParams).each {
            println "In product ${it.id} ${it.name}"
            Map<String, String> queryParamsEng = new HashMap<>();
            queryParamsEng.put("product", it.id);
            def endpoints = endpointService.search(queryParamsEng)
            for (endpoint in endpoints) {
                println "Deleting endpoint ${endpoint.id}"
                endpointService.delete(endpoint.id)
            }
            def engagements = engagagementService.search(queryParamsEng);
            for (eng in engagements) {
                println("found engagement ${eng.id}")
                def testsToDelete = []
                Map<String, String> queryParamsTest = new HashMap<>();
                queryParamsTest.put("engagement", Long.toString(eng.id))
                testService.search(queryParamsTest).stream().filter((test -> {
                    testsToDelete.push(test.id)
                })).collect(Collectors.toList());

                for(testId in testsToDelete) {
                    Map<String, String> queryParamsFinding = new HashMap<>();
                    queryParamsFinding.put("test", "${testId}");
                    println("searching for findings in ${testId}")
                    findingService.search(queryParamsFinding).each {
                        println "Deleting finding ${it.id}"
                        findingService.delete(it.id)
                    }
                    println "Deleting test ${testId}"
                    testService.delete(testId)
                }

                println "Deleting eng ${eng.id}"
                engagagementService.delete(eng.id)
            }

            println "deleting ${it.id}"
            productService.delete(it.id)
        }
    }
}
