#!/usr/bin/env groovy
package statisticClient

@GrabConfig(systemClassLoader=true)
@GrabResolver(name='maven-snapshot', root='https://oss.sonatype.org/content/repositories/snapshots/')
// Click on the dep and hit ALT+Enter to grab
@Grab("io.securecodebox:defectdojo-client:0.0.27-SNAPSHOT")

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

def generateResponseStatistic(conf, queryParams) {
    def productTypeService = new ProductTypeService(conf);
    def productService = new ProductService(conf);
    def testService = new TestService(conf);
    def engagagementService = new EngagementService(conf)
    def endpointService = new EndpointService(conf)
    def findingService = new FindingService(conf)
    def today = new Date()
    println "Will fetch all products and iterate over findings"
    println "Products with tag jenkins are getting skipped"
    Map<String, ArrayList<Integer>> teamsResponseTimes = new HashMap<String, ArrayList<Integer>>()
    productService.search(queryParams).each {
        def delete = true
        Map<String, String> queryParamsEng = new HashMap<>();
        def product = it;
        //println "In product ${it.id} with tags ${it.tags}"
        def team = ""
        for (tag in it.tags) {
            if (tag.startsWith("team/")) {
                if (!team.empty) {
                    println "There are min. two teams for product ${it.id} ${it.name} defined ${it.tags}"
                }
                team = tag
            }
        }

        Map<String, String> queryParamsFindings = new HashMap<>();
        queryParamsFindings.put("test__engagement__product", it.id);
        queryParamsFindings.put("duplicate", "false");
        def findings = findingService.search(queryParamsFindings)
        for (finding in findings) {
            if(it.tags.contains("jenkins")) continue;
            if(finding.severity.getNumericRepresentation() <= 2) {
                continue;
            }
            def mitigatedDateOrToday = today;
            if (!finding.active) {
                if(finding.getMitigatedAt() != null) {
                    mitigatedDateOrToday = finding.getMitigatedAt().toDate()
                }else if (finding.getRiskAccepted()) {
                    println "TODO risk acceptance for finding ${finding.id};"
                    continue
                } else if (finding.verified) { //suppressed findings
                    continue
                } else {
                    println "Unknown state for finding ${finding.id} in product ${product.id}"
                }
            }
            TimeDuration duration = groovy.time.TimeCategory.minus(
                    mitigatedDateOrToday,
                    finding.getCreatedAt().toDate()
            );
            def teamAndSeverity = team + "-" + finding.severity
            ArrayList<Integer> durations = teamsResponseTimes.getOrDefault(teamAndSeverity, new ArrayList<Integer>())
            durations.push(duration)
            teamsResponseTimes.put(teamAndSeverity, durations)
        }
    }
    teamsResponseTimes.sort()
    def statisticFile = new File(System.getenv("STATISTIC_FILE_PATH"))
    statisticFile.delete()
    statisticFile.createNewFile()
    statisticFile.append("team,average in days,maximum,amount of findings\n")
    for (teamResponseTimes in teamsResponseTimes) {
        def sumDuration=0
        def amount = 0;
        for (TimeDuration duration in teamResponseTimes.value) {
            amount++
            sumDuration = sumDuration+duration.days
            //println "team ${teamResponseTimes.key}: ${duration.days}"
        }
        avgDuration = java.lang.Math.round(sumDuration/teamResponseTimes.value.size())
        def statusText = "\"${teamResponseTimes.key}\",\"${avgDuration}\",\"" + Collections.max(teamResponseTimes.value) + "\",${amount}\n"
        print statusText
        statisticFile.append(statusText)
    }
}
Map<String, String> queryParamsProduct = new HashMap<>();
generateResponseStatistic(conf, queryParamsProduct)