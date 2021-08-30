#!/usr/bin/env groovy
@GrabConfig(systemClassLoader=true)
//#@Grab(group='com.fasterxml.jackson.core', module='jackson-core', version='2.9.9')
//@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.9.9.2')
//@Grab(group='org.codehaus.jackson', module='jackson-mapper-asl', version='1.9.13')
//@Grab(group= 'org.springframework', module='spring-web', version='5.2.12.RELEASE')
@GrabResolver(name='maven-snapshot', root='https://oss.sonatype.org/content/repositories/snapshots/')
@Grab("io.securecodebox:defectdojo-client:0.0.14-SNAPSHOT")

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

import java.util.stream.Collectors

String dojoUrl = System.getenv("DD_URL")

String dojoToken = System.getenv("DD_TOKEN")
String dojoUser = System.getenv("DD_USER")

def conf = new DefectDojoConfig(dojoUrl, dojoToken, dojoUser);
def productTypeService = new ProductTypeService(conf);
def productService = new ProductService(conf);
def testService = new TestService(conf);
def engagagementService = new EngagementService(conf)
def endpointService = new EndpointService(conf)
def findingService = new FindingService(conf)

// delete all findings based on name
/*
Map<String, String> queryParamsFinding = new HashMap<>();
queryParamsFinding.put("test__engagement__product", it.id);
queryParamsFinding.put("name", 'ImageAge > 10');
findingService.search(queryParamsFinding).each {
    println "Deleting finding ${it.id}"
    findingService.delete(it.id)
}
*/

// delete all products based on name
Map<String, String> queryParams = new HashMap<>();
queryParams.put("tags", 'team/XXX'); // CHANGE THE PRODUC FILTER HERE
def products = productService.search(queryParams).each {
    println "In product ${it.id}"
    Map<String, String> queryParamsEng = new HashMap<>();
    queryParamsEng.put("product", it.id);
    def endpoints = endpointService.search(queryParamsEng)
    for (endpoint in endpoints) {
        println "Deleting endpoint ${endpoint.id}"
        endpointService.delete(endpoint.id)
    }
    def engagements = engagagementService.search(queryParamsEng);
    for (eng in engagements) {

        def testsToDelete = []
        testService.search(Map.of("test__engagement", Long.toString(eng.id))).stream().filter((test -> {
            testsToDelete.push(test.id)
        })).collect(Collectors.toList());

        for(testId in testsToDelete) {
            println "Deleting test ${testId}"
            testService.delete(testId)

        }

        println "Deleting eng ${eng.id}"
        engagagementService.delete(eng.id)
    }

    println "deleting ${it.id}"
    productService.delete(it.id)
}
