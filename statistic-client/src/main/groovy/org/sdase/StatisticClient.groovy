/*
 * This Groovy source file was generated by the Gradle 'init' task.
 */
package org.sdase

import io.securecodebox.persistence.defectdojo.config.DefectDojoConfig
import io.securecodebox.persistence.defectdojo.models.Finding
import io.securecodebox.persistence.defectdojo.service.EndpointService
import io.securecodebox.persistence.defectdojo.service.EngagementService
import io.securecodebox.persistence.defectdojo.service.FindingService
import io.securecodebox.persistence.defectdojo.service.ProductService;
import io.securecodebox.persistence.defectdojo.service.ProductTypeService;
import io.securecodebox.persistence.defectdojo.service.TestService

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class StatisticClient {
    static void main(String[] args) {
        String dojoUrl = System.getenv("DEFECTDOJO_URL")
        if ( dojoUrl == null ) {
            dojoUrl = System.getenv("DD_URL")
        }
        if(dojoUrl == null || dojoUrl.empty()) {
            println "DEFECTDOJO_URL not set"
            System.exit(1)
        }

        String dojoToken = System.getenv("DEFECTDOJO_APIKEY")
        if ( dojoToken == null ) {
            dojoToken = System.getenv("DD_TOKEN")
        }
        String dojoUser = System.getenv("DEFECTDOJO_USERNAME")
        if ( dojoUser == null ) {
            dojoUser = System.getenv("DD_USER")
        }
        String dateFormat = "yyyy-MM-dd HH:mm"

        String startDateString = System.getenv("START_DATE")
        String endDateString = System.getenv("END_DATE")

        if ( startDateString == null ) {
            println "START_DATE is empty, assuming the default of calculating the last 90 days"
            endDateString = LocalDateTime.now().format(dateFormat)
            startDateString = LocalDateTime.now().minus(90, ChronoUnit.DAYS).format(dateFormat)
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        LocalDateTime endDate = LocalDateTime.parse(endDateString, formatter)
        LocalDateTime startDate = LocalDateTime.parse(startDateString, formatter)

        println " Time range to look at:  ${startDate} - ${endDate} "
        def conf = new DefectDojoConfig(dojoUrl, dojoToken, dojoUser, 200)

        Map<String, String> queryParamsProduct = new HashMap<>();

        StatisticClient.generateResponseStatistic(conf, queryParamsProduct, startDate, endDate)
    }
    public static generateResponseStatistic(conf, queryParams, startDate, endDate) {
        def productTypeService = new ProductTypeService(conf);
        def productService = new ProductService(conf);
        def testService = new TestService(conf);
        def engagagementService = new EngagementService(conf)
        def endpointService = new EndpointService(conf)
        def findingService = new FindingService(conf)

        println "Will fetch all products and iterate over findings"
        println "Products with tag jenkins are getting skipped"
        List<StatisticFinding> responseFindings = new LinkedList<StatisticFinding>()
        List<String> environments = new LinkedList<String>()
        List<String> teams = new LinkedList<String>()
        productService.search(queryParams).each {
            def delete = true
            Map<String, String> queryParamsEng = new HashMap<>();
            def product = it;
            //println "In product ${it.id} with tags ${it.tags}"

            Map<String, String> queryParamsFindings = new HashMap<>();
            queryParamsFindings.put("test__engagement__product", it.id);
            queryParamsFindings.put("duplicate", "false");

            def findings = findingService.search(queryParamsFindings)
            for (finding in findings) {
                if (it.tags.contains("jenkins")) continue;
                if (StatisticFinding.getTag("team", it.tags) == null) {
                    println "product ${it.getId()} doesn't has a team tag, skipping"
                    continue
                }
                if (StatisticFinding.getTag("cluster", it.tags) == null) {
                    println "product ${it.getId()} doesn't has a cluster tag, skipping"
                    continue
                }
                if (finding.getCreatedAt() > endDate) {
                    //println "Skipping ${finding.id}: ${finding.getCreatedAt()} > ${endDate}"
                    continue
                }
                if (!finding.active) {
                    if (finding.getMitigatedAt() != null) {
                        //
                    } else if (finding.getRiskAccepted()) {
                        //
                    } else if (finding.verified) { //suppressed findings
                        println "TODO suppressed for finding ${finding.id}; skipping"
                        continue
                    } else {
                        println "Unknown state for finding ${finding.id} in product ${product.id}"
                    }
                }

                def statisticFinding = new StatisticFinding(finding, it.tags)

                responseFindings.push(statisticFinding)
                environments.push(statisticFinding.getEnvironment())
                teams.push(statisticFinding.getTeam())
            }
        }
        def file = System.getenv("STATISTIC_FILE_PATH")
        if(file == null || file.empty()) file = "/tmp/team-response-statistics.csv"
        def statisticFile = new File(file)
        if (!statisticFile.exists()) {
            statisticFile.createNewFile()
            def fileHeader = "environment,team,severity,average in days (including all open findings),average in days for findings opened within ${startDate}-${endDate},maximum (all opened findings),maximum in days for findings opened within ${startDate}-${endDate},amount of findings (all open),amount of findings in days for findings opened within ${startDate}-${endDate}\n"
            print fileHeader
            statisticFile.append(fileHeader)
        }

        environments = environments.sort().unique()
        teams = teams.sort().unique()

        if (environments.contains(null)) environments.remove(null)
        if (teams.contains(null)) teams.remove(null)
        for (environment in environments) {
            for (team in teams) {
                for (String severity in Finding.Severity.values()) {
                    List<StatisticFinding> filterFindings = StatisticFinding.findEnvironment(responseFindings, environment, team, severity);
                    def sumDuration = 0
                    def amount = 0;
                    def maxDuration
                    for (StatisticFinding statisticFinding in filterFindings) {
                        if (statisticFinding.getCreatedAt() < startDate && !statisticFinding.getActive()) {
                            println "skipping inactive old finding ${statisticFinding.getId()}"
                            continue
                        }

                        if (statisticFinding.getSeverity().name().matches(severity)) {
                            if (maxDuration == null || maxDuration < statisticFinding.duration) {
                                maxDuration = statisticFinding.getDuration()
                            }
                            amount++
                            sumDuration = sumDuration + statisticFinding.getDuration().days
                        }
                    }
                    def sumDurationWithLimitStart = 0
                    def amountWithLimitStart = 0;
                    def maxDurationWithLimitStart
                    for (StatisticFinding statisticFinding in filterFindings) {
                        if (statisticFinding.getCreatedAt() < startDate) {
                            continue
                        }
                        if (statisticFinding.getSeverity().name().matches(severity)) {
                            if (maxDurationWithLimitStart == null || maxDurationWithLimitStart < statisticFinding.duration) {
                                maxDurationWithLimitStart = statisticFinding.getDuration()
                            }

                            amountWithLimitStart++
                            sumDurationWithLimitStart = sumDurationWithLimitStart + statisticFinding.getDuration().days
                        }
                    }

                    if (amount == 0 && amountWithLimitStart == 0) continue
                    def avgDuration = 0
                    if (amount != 0) {
                        avgDuration = java.lang.Math.round(sumDuration / amount)
                    }
                    def avgDurationWithLimitStart = 0
                    if (amountWithLimitStart != 0) {
                        avgDurationWithLimitStart = java.lang.Math.round(sumDurationWithLimitStart / amountWithLimitStart)
                    }
                    def maxDurationString = 0
                    if (maxDuration) maxDurationString = maxDuration.days
                    def maxDurationWithLimitStartString = 0
                    if (maxDurationWithLimitStart) maxDurationWithLimitStartString = maxDurationWithLimitStart.days
                    def statusText = "\"${environment}\",\"${team}\",\"${severity}\",${avgDuration},${avgDurationWithLimitStart},${maxDurationString},${maxDurationWithLimitStartString},${amount},${amountWithLimitStart}\n"
                    print statusText
                    statisticFile.append(statusText)

                }

            }
        }
    }
}
