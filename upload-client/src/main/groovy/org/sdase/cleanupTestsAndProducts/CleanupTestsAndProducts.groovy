package org.sdase.cleanupTestsAndProducts

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

class CleanupTestsAndProducts {
    static void main(Object args) {
        def conf = new DefectDojoConfig(args.dojoUrl, args.dojoToken, args.dojoUser, 200);
        def productTypeService = new ProductTypeService(conf);
        def productService = new ProductService(conf);
        def testService = new TestService(conf);
        def engagagementService = new EngagementService(conf)
        def endpointService = new EndpointService(conf)
        def findingService = new FindingService(conf)

        def amountOfLastTestsToNotDelete=8
        Map<String, String> queryParams = new HashMap<>();
        productService.search(queryParams).each {
            println "In product ${it.id}"

            Map<String, String> queryParamsEng = new HashMap<>();
            queryParamsEng.put("product", it.id);
            queryParamsEng.put("active", "true");

            def isAllEngagementTestsEmpty=true
            def engagements = engagagementService.search(queryParamsEng);
            for (eng in engagements) {
                Map<String, String> queryParamsTest = new HashMap<>();
                queryParamsTest.put("engagement", Long.toString(eng.id))
                queryParamsTest.put("o", "title") // ordering
                def tests = testService.search(queryParamsTest)
                def i=0

                for (test in tests) {
                    i++
                    if(i<amountOfLastTestsToNotDelete) {
                        isAllEngagementTestsEmpty=false
                        continue
                    }
                    Map<String, String> queryParamsFinding = new HashMap<>();
                    queryParamsFinding.put("test", Long.toString(test.id));
                    def findings = findingService.search(queryParamsFinding)
                    if(findings.size() == 0) {
                        println "Finding size=0 for test ${test.id}, DELETING IT, eng ${eng.id}, title ${test.title}"
                        testService.delete(test.id)
                    }else {
                        isAllEngagementTestsEmpty=false
                    }
                }
            }
            if(isAllEngagementTestsEmpty) {
                it.lifecycle = "retirement"
            } else {
                it.lifecycle = "production"
            }
            productService.update(it, it.id)
        }
    }
}
