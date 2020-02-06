#!/usr/bin/env groovy

@GrabConfig(systemClassLoader=true)
@Grab(group='com.fasterxml.jackson.core', module='jackson-core', version='2.9.9')
@Grab(group='com.fasterxml.jackson.core', module='jackson-databind', version='2.9.9.2')
@Grab(group='org.codehaus.jackson', module='jackson-mapper-asl', version='1.9.13')
@Grab('io.securecodebox.core:sdk:0.0.1-SNAPSHOT')
@Grab('io.securecodebox.persistenceproviders:defectdojo-persistenceprovider:0.0.1-SNAPSHOT')

import io.securecodebox.persistence.*
import io.securecodebox.persistence.models.*
import io.securecodebox.model.rest.Report
import io.securecodebox.model.securitytest.SecurityTest
import io.securecodebox.model.execution.Target
import com.fasterxml.jackson.databind.ObjectMapper
import org.codehaus.jackson.map.DeserializationConfig
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.client.RestTemplate
import org.springframework.http.*
import io.securecodebox.persistence.models.TestResponse


def call(args) {
    DefectDojoService defectDojoService = new DefectDojoService();

    defectDojoService.defectDojoUrl = args.dojoUrl
    defectDojoService.defectDojoApiKey = args.token
    defectDojoService.defectDojoDefaultUserName = args.user

    EngagementPayload engagement = new EngagementPayload()
    engagement.setBranch args.branchName
    engagement.setBuildID args.buildId
    engagement.setDeduplicationOnEngagement true
    engagement.setRepo args.sourceCodeManagementUri

    String reportContents = new File(args.reportPath).text
    def date = new Date()
    def dateNow = date.format("yyyy-MM-dd")
    def timeNow = date.format("HH:mm:ss")
    def engagementName = "Dep Check " + args.branchName
    def reportType = "Dependency Check Scan"
    // In DefectDojo Version 1.5.4 you can specify test_type/testName; BE AWARE: close_old_findings will not work by using something else than reportType
    def testName = reportType // "${engagementName} ${timeNow}"
    def minimumSeverity = "High"
    
    TestPayload testPayload = new TestPayload()
    testPayload.setTitle(testName) // for DefectDojo < 1.5.4 'null' should be used, afterwards testName can be given
    testPayload.setTargetStart(dateNow + " " + timeNow)
    testPayload.setTargetEnd(dateNow + " " + timeNow)
    MultiValueMap<String, Object> options =  new LinkedMultiValueMap<String, Object>();
    
    if(args.branchName.equals("master")) {
        options.add("active", "true")
        options.add("verified", "true")
        testPayload.setEnvironment("3")
    }else {
        options.add("active", "false")
        options.add("verified", "false")
        testPayload.setEnvironment("1")
    }

    if(args.importType.equals("import")) {
        defectDojoService.createFindingsForEngagementName(
            engagementName,
            reportContents,
            reportType,
            args.product,
            args.lead,
            engagement,
            testName,
            options
        );
    }else if(args.importType.equals("reimport")) {
        defectDojoService.createFindingsReImport(
            reportContents, 
            args.product, 
            engagementName,  
            args.lead, 
            dateNow, 
            reportType, 
            engagement, 
            testPayload, 
            options)
    }else {
        println "Error: importType not known"
        return
    }

    def keepAllBranches = false;
    for (branchToKeep in args.branchesToKeep) {
        if(branchToKeep.equals("*")) {
            keepAllBranches=true;
            break;
        } else {
            println "Will keep the enagagement with branch '${branchToKeep}' in DefectDojo"
        }
    }
    if(!keepAllBranches) defectDojoService.deleteUnusedBranches(args.branchesToKeep, args.product)

    MultiValueMap<String, Object> optionsToGetFindings =  new LinkedMultiValueMap<String, Object>();
    optionsToGetFindings.add("active", "true")
    List<Finding> findings = defectDojoService.receiveNonHandledFindings(args.product, engagementName, minimumSeverity, optionsToGetFindings);
    for(Finding finding : findings) {
        println finding.getTitle() + " " + finding.getSeverity()
    }
    long findingSize = findings.size()

    def testId = defectDojoService.getLatestTestIdByEngagementName(engagementName, args.product, testName, 0L)
    if(!testId.isPresent()){
        println "Could not find engagement"
        System.exit(2)
    }    
    def defectDojoTestLink = args.dojoUrl + "/test/" + testId.get();

    File file = new File("defectDojoTestLink.txt")
    file.write defectDojoTestLink
    println "DefectDojo test with scan results can be viewed at $defectDojoTestLink"

    if(findingSize > 0) {
        // Mark build as unstable
        println "$findingSize vulnerabilities found with severity $minimumSeverity or higher"
        println "DefectDojo test with scan results can be viewed at $defectDojoTestLink"
        System.exit(1)
    }
}

