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

def closeDependencyCheckFindings(conf, queryParamsEng, dojoUrl) {
    def productTypeService = new ProductTypeService(conf);
    def productService = new ProductService(conf);
    def testService = new TestService(conf);
    def engagagementService = new EngagementService(conf)
    def endpointService = new EndpointService(conf)
    def findingService = new FindingService(conf)
    def engagements = engagagementService.search(queryParamsEng);
    for (eng in engagements) {
        if(!eng.getName().contains("Dependency Check Scan")) {
            println "Skipping engagement ${eng.id}"
            continue;
        }
        Map<String, String> queryParamsTest = new HashMap<>();
        queryParamsTest.put("engagement", Long.toString(eng.id))
        queryParamsTest.put("o", "title") // ordering
        def tests = testService.search(queryParamsTest)
        for(test in tests) {
            Map<String, String> queryParamsFinding = new HashMap<>();
            queryParamsFinding.put("test", test.id)
            queryParamsFinding.put("active", "true")
            findingService.search(queryParamsFinding).each {
                println "finding ${it.id} is active, setting to inactive"
                it.active = false;
                it.verified = false;
                findingService.update(it, it.id)
            }

        }
   }
}
Map<String, String> queryParamsEng = new HashMap<>();
closeDependencyCheckFindings(conf, queryParamsEng, dojoUrl)

