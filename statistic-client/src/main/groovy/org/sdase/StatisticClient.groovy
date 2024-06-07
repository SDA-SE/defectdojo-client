//file:noinspection LineLength
package org.sdase

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
class StatisticClient {

    private static String[] excludeTags = ['jenkins']

    static void call() {
        log.info('StatisticClient')
        log.debug('Debug logging enabled')

        String dateFormat = 'yyyy-MM-dd HH:mm'

        String startDateString = System.getenv('START_DATE')
        String endDateString = System.getenv('END_DATE')

        if (startDateString == null) {
            log.info 'START_DATE is empty, assuming the default of calculating the last 90 days'
            endDateString = LocalDateTime.now().format(dateFormat)
            startDateString = LocalDateTime.now().minus(90, ChronoUnit.DAYS).format(dateFormat)
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat)
        LocalDateTime endDate = LocalDateTime.parse(endDateString, formatter)
        LocalDateTime startDate = LocalDateTime.parse(startDateString, formatter)

        log.info "Time range to look at:  ${startDate} - ${endDate}"
        def dojoConf = createDojoConf()

        // set DEFECT_DOJO_OBJET_LIMIT to 3000 to save tons of unnecessary requests.
        // using Reflection, not inheritance because weird compile time classpath issues seem to prevent subclassing
        GenericDefectDojoService.getDeclaredField("DEFECT_DOJO_OBJET_LIMIT").setAccessible(true)
        def productService = new ProductService(dojoConf)
        //noinspection GroovyAccessibility -- see comment above
        productService.DEFECT_DOJO_OBJET_LIMIT = 3000
	log.info("limit is set to productService.DEFECT_DOJO_OBJET_LIMIT ${productService.DEFECT_DOJO_OBJET_LIMIT}")
        def findingService = new FindingService(dojoConf)
        //noinspection GroovyAccessibility -- see comment above
        findingService.DEFECT_DOJO_OBJET_LIMIT = 3000
	log.info("limit is set to findingService.DEFECT_DOJO_OBJET_LIMIT ${findingService.DEFECT_DOJO_OBJET_LIMIT}")

        generateResponseStatistic(productService, findingService, startDate, endDate)
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

    static generateResponseStatistic(
            ProductService productService,
            FindingService findingService,
            LocalDateTime startDate,
            LocalDateTime endDate)
    {
        log.debug 'Will fetch all products and iterate over findings'
        log.debug "Products with tags ${excludeTags} are getting skipped"
        List<StatisticFinding> responseFindings = new LinkedList<StatisticFinding>()
        SortedSet<String> environments = new TreeSet<String>()
        SortedSet<String> teams = new TreeSet<String>()
        List<Product> products = productService.search([:])
        log.info "${products.size()} products found, filtering"

        // filter products, @see shouldIncludeProduct
        products = products.stream()
                .filter(product -> shouldIncludeProduct(product))
                .collect(Collectors.toList())

        log.info "${products.size()} products found, iterating"

        for (product in products) {
            def findings = findingService.search([
                    test__engagement__product: product.id.toString(),
                    duplicate: "false"
            ])
	    log.info "Processing product ${product.id.toString()}"
            for (finding in findings) {
                if (finding.getCreatedAt() > endDate) {
                    // skip all findings that were created after the end date. note that we can't filter for that at
                    // query time, because the DefectDojo API only filters for pre-set time ranges, not an exact date.
                    // e.g. "past 7 days", "past 30 days..."
                    continue
                }

                def statisticFinding = new StatisticFinding(finding, product.tags)

                responseFindings.push(statisticFinding)
                environments.add(statisticFinding.getEnvironment())
                teams.add(statisticFinding.getTeam())
            }
        }
        def file = System.getenv('STATISTIC_FILE_PATH')
        if (file == null || file.empty) {
            file = '/tmp/team-response-statistics.csv'
        }
        def statisticFile = new File(file)
        if (!statisticFile.exists()) {
            statisticFile.createNewFile()
            def fileHeader =
                    "environment,team,severity," +
                            "average days to react (including all findings created before ${endDate})," +
                            "average days to react for findings created within ${startDate}-${endDate}," +
                            "maximum days to react (all findings created before ${endDate})," +
                            "maximum days to react for findings created within ${startDate}-${endDate}," +
                            "amount of findings (all created before ${endDate})," +
                            "amount of findings in days for findings created within ${startDate}-${endDate}," +
                            "average days to patch (including all findings created before ${endDate})," +
                            "average days to patch for findings created within ${startDate}-${endDate}" +
                            "\n"
            statisticFile.append(fileHeader)
        }

        log.debug "File opened, starting loop through environments/teams/severities..."

        for (environment in environments) {
            for (team in teams) {
                for (String severity in Finding.Severity.values()) {
                    List<StatisticFinding> statisticFindings = StatisticFinding.findByEnvTeamSev(responseFindings, environment, team, severity)
                    def sumDurationAllTimeUpToEndDate_Reacted = 0
                    def amountAllTimeUpToEndDate_Reacted = 0

                    def sumDurationAllTimeUpToEndDate_Patched = 0
                    def amountAllTimeUpToEndDate_Patched = 0

                    def maxDurationAllTimeUpToEndDate = null

                    // note that we already skipped all findings before "endDate" while creating the StatisticFindings
                    for (StatisticFinding statisticFinding in statisticFindings) {
                        // first, we look at all findings even before "createdAt"...
                        if (statisticFinding.getCreatedAt() < startDate && !statisticFinding.getActive()) {
                            // ... but not if the old findings are inactive
                            continue
                        }
                        // note that StatisticFinding automatically substitutes "today" if no "mitigatedAt" date exists
                        maxDurationAllTimeUpToEndDate = max(maxDurationAllTimeUpToEndDate, statisticFinding.duration)
                        amountAllTimeUpToEndDate_Reacted++
                        sumDurationAllTimeUpToEndDate_Reacted += statisticFinding.duration.days
                        if (statisticFinding.getMitigatedAt() != null) {
                            // if the finding is marked as "mitigated", count it towards the "patched" statistic
                            amountAllTimeUpToEndDate_Patched++
                            sumDurationAllTimeUpToEndDate_Patched += statisticFinding.duration.days
                        }
                    }

                    def sumDurationWithinTimeRange_Reacted = 0
                    def amountWithinTimeRange_Reacted = 0
                    def sumDurationWithinTimeRange_Patched = 0
                    def amountWithinTimeRange_Patched = 0
                    def maxDurationWithinTimeRange = null

                    for (StatisticFinding statisticFinding in statisticFindings) {
                        if (statisticFinding.createdAt < startDate) {
                            continue
                        }
                        maxDurationWithinTimeRange = max(maxDurationWithinTimeRange, statisticFinding.duration)
                        amountWithinTimeRange_Reacted++
                        sumDurationWithinTimeRange_Reacted += statisticFinding.duration.days
                        if (statisticFinding.getMitigatedAt() != null) {
                            amountWithinTimeRange_Patched++
                            sumDurationWithinTimeRange_Patched += statisticFinding.duration.days
                        }
                    }

                    // if there has never been a reaction for this severity+team+product, don't count it
                    if (amountAllTimeUpToEndDate_Reacted == 0 && amountWithinTimeRange_Reacted == 0) {
                        continue
                    }
                    def avgDurationAllTimeUpToEndDate_Reacted = roundedMean(sumDurationAllTimeUpToEndDate_Reacted, amountAllTimeUpToEndDate_Reacted)
                    def avgDurationWithinTimeRange_Reacted = roundedMean(sumDurationWithinTimeRange_Reacted, amountWithinTimeRange_Reacted)

                    def avgDurationAllTimeUpToEndDate_Patched = 'n/a'
                    if (sumDurationAllTimeUpToEndDate_Patched > 0) {
                        avgDurationAllTimeUpToEndDate_Patched = roundedMean(sumDurationAllTimeUpToEndDate_Patched, amountAllTimeUpToEndDate_Patched)
                    }
                    def avgDurationWithinTimeRange_Patched = 'n/a'
                    if (sumDurationWithinTimeRange_Patched > 0) {
                        avgDurationWithinTimeRange_Patched = roundedMean(sumDurationWithinTimeRange_Patched, amountWithinTimeRange_Patched)
                    }
                    def maxDurationAllTimeUpToEndDateOrZero = maxDurationAllTimeUpToEndDate?.days ?: 0
                    def maxDurationWithinTimeRangeOrZero = maxDurationWithinTimeRange?.days ?: 0

                    def line =
                            "\"${environment}\",\"${team}\",\"${severity}\"," +
                                    "${avgDurationAllTimeUpToEndDate_Reacted}," +
                                    "${avgDurationWithinTimeRange_Reacted}," +
                                    "${maxDurationAllTimeUpToEndDateOrZero}," +
                                    "${maxDurationWithinTimeRangeOrZero}," +
                                    "${amountAllTimeUpToEndDate_Reacted}," +
                                    "${amountWithinTimeRange_Reacted}," +
                                    "${avgDurationAllTimeUpToEndDate_Patched}," +
                                    "${avgDurationWithinTimeRange_Patched}" +
                                    "\n"

                    statisticFile.append(line)
                }

            }
        }
        log.debug "wrote to file ${statisticFile.canonicalPath}"
    }

    private static Integer roundedMean(Integer sum, Integer amount) {
        if (amount == 0) return 0
        return Math.round(sum / amount)
    }

    private static TimeDuration max(TimeDuration durationA, TimeDuration durationB) {
        if (durationB == null) return durationA
        if (durationA == null) return durationB
        return durationB > durationA ? durationB : durationA
    }

    private static boolean shouldIncludeProduct(Product product) {
        // exclude products that have a tag in `excludeTags`
        if (product.tags.any(tag -> excludeTags.contains(tag))) {
            return false
        }
        // exclude products without a team tag
        if (StatisticFinding.findTagWithPrefix('team', product.tags) == null) {
            log.debug "product ${product.getId()} has no team tag, skipping"
            return false
        }
        // exclude products without a cluster tag
        if (StatisticFinding.findTagWithPrefix('cluster', product.tags) == null) {
            log.debug "product ${product.getId()} has no cluster tag, skipping"
            return false
        }
        return true
    }

    private static boolean isNullOrEmpty(String string) {
        return string == null || string.empty
    }
}
