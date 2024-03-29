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

String dojoToken = System.getenv("DEFECTDOJO_APIKEY")
String dojoUser = System.getenv("DEFECTDOJO_USERNAME")

def conf = DefectDojoConfig.fromEnv(); //(dojoUrl, dojoToken, dojoUser);
def productTypeService = new ProductTypeService(conf);
def productService = new ProductService(conf);
def testService = new TestService(conf);
def engagagementService = new EngagementService(conf)
def endpointService = new EndpointService(conf)
def findingService = new FindingService(conf)

// delete all findings based on name
def deleteAllFindings(conf, queryParamsFinding) {
    def findingService = new FindingService(conf)
    findingService.search(queryParamsFinding).each {
        println "Deleting finding ${it.id}"
        findingService.delete(it.id)
    }
}
Map<String, String> queryParamsFinding = new HashMap<>();
queryParamsFinding.put("title", 'BaseImage Age > 60 Days');
//deleteAllFindings(conf, queryParamsFinding)

def deleteAllProductsSimple(conf, queryParamsSimpleDelete) {
    def productService = new ProductService(conf);

    def products = productService.search(queryParamsSimpleDelete).each {
        println "deleting product ${it.id}"
        productService.delete(it.id)
    }
}
Map<String, String> queryParamsSimpleDelete = new HashMap<>();
queryParamsFinding.put("title", '|');
//deleteAllProductsSimple(conf, queryParamsSimpleDelete)

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
queryParams.put("name", '|');
//deleteAllProducts(conf, queryParams)


def findProductsWithNoCurrentTestAndDelete(conf, int mayAgeOfTestInDays, queryParams, dojoUrl) {

    def productTypeService = new ProductTypeService(conf);
    def productService = new ProductService(conf);
    def testService = new TestService(conf);
    def engagagementService = new EngagementService(conf)
    def endpointService = new EndpointService(conf)
    def findingService = new FindingService(conf)
    println "Will fetch products according to query params"
    productService.search(queryParams).each {
        def delete = true
        Map<String, String> queryParamsEng = new HashMap<>();
        def product = it;
        queryParamsEng.put("product", it.id);
	    println "In product ${it.id}"
        def engagements = engagagementService.search(queryParamsEng);
        for (eng in engagements) {
            Map<String, String> queryParamsTest = new HashMap<>();
            queryParamsTest.put("engagement", Long.toString(eng.id))
            queryParamsTest.put("o", "title") // ordering
            def tests = testService.search(queryParamsTest)
            if(tests.size() == 0) continue;
            test = tests.last()
            def dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
            if (test.targetStart.length() == 27) {
                dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSS'Z'"
            }
            def testDate = Date.parse(dateFormat, test.targetStart)
            def duration = groovy.time.TimeCategory.minus(
                    new Date(),
                    testDate
            );
            if(duration.days < mayAgeOfTestInDays) {
                println "Will NOT delete product: ${product.name} (${product.id}), test: ${test.title} (${test.id}, ${test.targetStart}), duration: ${duration.days} ${dojoUrl}/test/${test.id}"
                delete = false
                break;
            }
        }

        try {
            if(delete) {
                println "Deleting product: ${product.name} (${product.id})"
                productService.delete(product.id)
            }
        }catch(Exception) {
            println "COULD NOT DELETE PRODUCT ${product.id}, try that afterwards"
        }
    }
}
Map<String, String> queryParamsProduct = new HashMap<>();
//queryParamsProduct.put("name", ':'); //shows that it comes via ClusterImageScanner or SecureCodeBox
mayAgeOfTestInDays=60
//findProductsWithNoCurrentTestAndDelete(conf, mayAgeOfTestInDays, queryParamsProduct, dojoUrl)

def deleteFindingsViaProduct(conf, Map<String, String> queryParams) {
    def productTypeService = new ProductTypeService(conf);
    def productService = new ProductService(conf);
    def testService = new TestService(conf);
    def engagagementService = new EngagementService(conf)
    def endpointService = new EndpointService(conf)
    def findingService = new FindingService(conf)
    def products = productService.search();

    for (product in products)  {
        println "product ${product.id}"
        Map<String, String> queryParamsFindings = new HashMap<>();
        queryParamsFindings.put("tag", 'suppressed');
        queryParamsFindings.put("test__engagement__product", product.id);
        findingService.search(queryParamsFindings).each {
            println "finding ${it.id}"
            findingService.delete(it.id)

        }
    }

}
//queryParams.put("name", '|'); //shows that it comes via ClusterImageScanner or SecureCodeBox
mayAgeOfTestInDays=15
//deleteFindingsViaProduct(conf, queryParams);

def deleteFindings(conf, Map<String, String> queryParams) {
    def productTypeService = new ProductTypeService(conf);
    def productService = new ProductService(conf);
    def testService = new TestService(conf);
    def engagagementService = new EngagementService(conf)
    def endpointService = new EndpointService(conf)
    def findingService = new FindingService(conf)

    Map<String, String> queryParamsFindings = new HashMap<>();
    queryParamsFindings.put("title", 'Age');
    queryParamsFindings.put("duplicate", 'false');
    findingService.search(queryParamsFindings).each {
        println "finding ${it.id}, ${it.title}"
        if(it.title.contains("Image Age >")) {
            println("deleting ${it.id}")
            findingService.delete(it.id)
        }

    }
}

//deleteFindings(conf, queryParams);



def findProducts(conf, Map<String, String> queryParams) {
    def productTypeService = new ProductTypeService(conf);
    def productService = new ProductService(conf);
    def testService = new TestService(conf);
    def engagagementService = new EngagementService(conf)
    def endpointService = new EndpointService(conf)
    def findingService = new FindingService(conf)
    def products = productService.search(queryParams);

    for (product in products) {
        println "product: ${product.id} ${product.name}"
	productService.delete(product.id)
    }
}
Map<String, String> queryParamsFindProduct = new HashMap<>();
queryParamsFindProduct.put("name", '|'); //shows that it comes via ClusterImageScanner or SecureCodeBox
//findProducts(conf, queryParamsFindProduct);



def findImagesWithAge(conf, Map<String, String> queryParams) {
    def productTypeService = new ProductTypeService(conf);
    def productService = new ProductService(conf);
    def testService = new TestService(conf);
    def engagagementService = new EngagementService(conf)
    def endpointService = new EndpointService(conf)
    def findingService = new FindingService(conf)

    def products = productService.search(queryParams);

    f = new File('myfile.txt')

    for (product in products) {
        Map<String, String> queryParamsFindings = new HashMap<>();
        queryParamsFindings.put("title", 'Age');
        queryParamsFindings.put("duplicate", 'false');
        queryParamsFindings.put("test__engagement__product", product.id);
        findingService.search(queryParamsFindings).each {
            //println "finding ${it.id}, ${it.title}"
            if(it.title.startsWith("Image Age >")) {
                def descriptionArray = it.description.split("\r?\n|\r");
                //println "descriptionArray ${descriptionArray[1]}"
                def age = descriptionArray[1].replace("Image is ", "").replaceAll("days old.*", "")
                def image = descriptionArray[2].replace("Image ", "")
                f.append("${age} ${image}\n")
            }

        }

    }
}
Map<String, String> findImagesWithAgeQuery = new HashMap<>();
findImagesWithAgeQuery.put("name", '|');
//findImagesWithAge(conf, findImagesWithAgeQuery);
