//file:noinspection LineLength
package org.sdase.deleteTestProduct

import groovy.time.TimeDuration
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import io.securecodebox.persistence.defectdojo.config.DefectDojoConfig
import io.securecodebox.persistence.defectdojo.models.Finding
import io.securecodebox.persistence.defectdojo.models.Product
import io.securecodebox.persistence.defectdojo.service.FindingService
import io.securecodebox.persistence.defectdojo.service.GenericDefectDojoService
import io.securecodebox.persistence.defectdojo.service.ProductService

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors

@Slf4j @CompileDynamic
class TestProductDeletor {
    static void call() {
        log.info('DefectDojo Test Deletion Client')

        def dojoConf = createDojoConf()

        URL resource = TestProductDeletor.getClassLoader().getResource("expectedFindings.json");
        File file = new File(resource.getPath())
        String fileContent = file.text
        def jsonSlurper = new groovy.json.JsonSlurper()
        def expectedFindings = jsonSlurper.parseText(fileContent)

        // set DEFECT_DOJO_OBJET_LIMIT to 3000 to save tons of unnecessary requests.
        // using Reflection, not inheritance because weird compile time classpath issues seem to prevent subclassing
        GenericDefectDojoService.getDeclaredField("DEFECT_DOJO_OBJET_LIMIT").setAccessible(true)
        def productService = new ProductService(dojoConf)
        //noinspection GroovyAccessibility -- see comment above
        productService.DEFECT_DOJO_OBJET_LIMIT = 3000
        def findingService = new FindingService(dojoConf)
        //noinspection GroovyAccessibility -- see comment above
        findingService.DEFECT_DOJO_OBJET_LIMIT = 3000

        def lastProductName = ""
        for (expectedFinding in expectedFindings) {
            if (lastProductName == expectedFinding.productName) {
                log.info "Skipping ${expectedFinding.productName} because product is deleted or doesn't exists"
                continue
            }
            lastProductName = expectedFinding.productName
            log.info "iterating over expectedFinding with query (${expectedFinding.productName})"
            Map<String, String> queryParameter = new HashMap<>();
            queryParameter.put("name", expectedFinding.productName);
            try {
                def product = productService.searchUnique(Product.builder().name(expectedFinding.productName).build()).orElseThrow{
                    new Exception("Could not find product with name '" + expectedFinding.productName + "' in DefectDojo API. DefectDojo might be running in an unsupported version.")
                };
                log.info "deleting product ${product.id}"

                productService.delete(product.id)
            } catch(Exception e) {
                log.warn("Product ${expectedFinding.productName} not found")
                println e
            }
        }
    }

    private static DefectDojoConfig createDojoConf() {
        def dojoUrl = System.getenv('DEFECTDOJO_URL') ?: System.getenv('DD_URL')
        if (isNullOrEmpty(dojoUrl)) {
            log.error 'DEFECTDOJO_URL not set'
            System.exit(1)
        }

        def dojoToken = System.getenv('DEFECTDOJO_APIKEY') ?: System.getenv('DD_TOKEN')
        if (isNullOrEmpty(dojoToken)) {
            log.error 'DEFECTDOJO_APIKEY not set'
            System.exit(1)
        }
        def dojoUser = System.getenv('DEFECTDOJO_USERNAME')
        if (dojoUser == null) {
            dojoUser = System.getenv('DD_USER')
        }
        return new DefectDojoConfig(dojoUrl, dojoToken, dojoUser, 200)
    }
    private static boolean isNullOrEmpty(String string) {
        return string == null || string.empty
    }
}
