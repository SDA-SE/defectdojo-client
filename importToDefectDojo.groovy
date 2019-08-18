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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

def call(args) {
    DefectDojoService defectDojoService = new DefectDojoService();

    defectDojoService.defectDojoUrl = args.dojoUrl
    defectDojoService.defectDojoApiKey = args.token
    defectDojoService.defectDojoDefaultUserName = args.user

    EngagementPayload engagement = new EngagementPayload()
    engagement.setBranch args.branchName
    engagement.setBuildID args.buildId
    engagement.setRepo args.sourceCodeManagementUri
    engagement.setDeduplicationOnEngagement true

    String reportContents = new File(args.reportPath).text
    def date = new Date()
    def dateNow = date.format("yyyy-mm-dd")
    dateNow = "2019-08-12"
    def timeNow = date.format("HH:mm:ss")
    def engagementName = "Dep Check " + args.branchName
    def reportType = "Dependency Check Scan"
    // In DefectDojo Version 1.5.4 you can specify test_type/testName
    //def testName = "${engagementName} ${timeNow}"
    def testName = reportType
    def minimumServerity = "High"
    
    TestPayload testPayload = new TestPayload()
    testPayload.setTitle(null) // for DefectDojo < 1.5.4 'null' should be used, afterwards testName can be given
    testPayload.setTargetStart(dateNow + " " + timeNow)
    testPayload.setTargetEnd(dateNow + " " + timeNow)
    MultiValueMap<String, Object> options =  new LinkedMultiValueMap<String, Object>();
    if(args.branchName.equals("master")) {
        testPayload.setEnvironment("3")
        options.add("active", "true")
    }else {
        testPayload.setEnvironment("1")
        options.add("active", "false")
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
    
    def existingBranchesInGit = [args.branchName, "branch2", "branch3"] // TODO
    defectDojoService.deleteUnusedBranches(existingBranchesInGit, args.product)

    List<Finding> findings = defectDojoService.receiveNonHandledFindings(args.product, engagementName, minimumServerity, new LinkedMultiValueMap<>());
    for(Finding finding : findings) {
        println finding.getTitle() + " " + finding.getSeverity()
    }
    long findingSize = findings.size()
    if(findingSize > 0) {
        // Mark build as unstable
        println "$findingSize vulnerabilities found with serverity $minimumServerity or higher"
        System.exit(1)
    }
}

