package org.sdase.deleteTestProduct

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

class TestProductDeletor {
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


        URL resource = TestProductDeletor.getClassLoader().getResource("expectedFindings.json");

        File file = new File(resource.getPath())
        String fileContent = file.text
        def jsonSlurper = new groovy.json.JsonSlurper()
        def expectedFindings = jsonSlurper.parseText(fileContent)

        for(expectedFinding in expectedFindings) {
            def foundToSearchFinding= 0
            //println "Searching for productName: ${expectedFinding.productName}"
            def product = productService.searchUnique(Product.builder().name(expectedFinding.productName).build())
            if(product != null) {
                println "Deleting product ${product.name} with id ${product.id}"
                product.delete()
            } else {
                println "Product ${expectedFinding.productName} not found"
            }
        }
    }
}
