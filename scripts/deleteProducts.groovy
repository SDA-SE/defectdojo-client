#!/usr/bin/env groovy
@GrabConfig(systemClassLoader=true)
//@Grab(group='com.fasterxml.jackson.core', module='jackson-core', version='2.13.2')
//@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.13.2')
//@Grab(group='org.codehaus.jackson', module='jackson-mapper-asl', version='2.13.2')
//@Grab(group= 'org.springframework', module='spring-web', version='5.2.12.RELEASE')
@GrabResolver(name='maven-snapshot', root='https://oss.sonatype.org/content/repositories/snapshots/')
// Click on the dep and hit ALT+Enter to grab
@Grab("io.securecodebox:defectdojo-client:0.0.41-SNAPSHOT")

import io.securecodebox.persistence.defectdojo.config.DefectDojoConfig
import io.securecodebox.persistence.defectdojo.models.Engagement
import io.securecodebox.persistence.defectdojo.models.Finding
import io.securecodebox.persistence.defectdojo.models.Product
import io.securecodebox.persistence.defectdojo.models.ProductType
import io.securecodebox.persistence.defectdojo.models.Test
import io.securecodebox.persistence.defectdojo.models.TestType
import io.securecodebox.persistence.defectdojo.models.User
import io.securecodebox.persistence.defectdojo.service.EndpointService
import io.securecodebox.persistence.defectdojo.service.EngagementService
import io.securecodebox.persistence.defectdojo.service.FindingService
import io.securecodebox.persistence.defectdojo.service.ImportScanService
import io.securecodebox.persistence.defectdojo.service.ProductService;
import io.securecodebox.persistence.defectdojo.service.ProductTypeService;
import io.securecodebox.persistence.defectdojo.service.TestService
import io.securecodebox.persistence.defectdojo.service.TestTypeService
import io.securecodebox.persistence.defectdojo.service.UserService
import io.securecodebox.persistence.defectdojo.ScanType
import groovy.time.*

import java.util.stream.Collectors

String dojoUrl = System.getenv("DEFECTDOJO_URL")
String productNameToDelete = System.getenv("PRODUCT_NAME_TO_DELETE")

String dojoToken = System.getenv("DEFECTDOJO_APIKEY")
String dojoUser = System.getenv("DEFECTDOJO_USERNAME")

def conf = DefectDojoConfig.fromEnv(); //(dojoUrl, dojoToken, dojoUser);
def productTypeService = new ProductTypeService(conf);
def productService = new ProductService(conf);
def testService = new TestService(conf);
def engagagementService = new EngagementService(conf)
def endpointService = new EndpointService(conf)
def findingService = new FindingService(conf)

// delete all products based on name
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
Map<String, String> queryParams = new HashMap<>();
queryParams.put("name", productNameToDelete);
deleteAllProducts(conf, queryParams)