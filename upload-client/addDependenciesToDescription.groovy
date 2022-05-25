#!/usr/bin/env groovy
package client

def dependencyCheckReportPath = args[0] // e.g. 'build/reports/dependency-check-report-10.xml'
def dependencies = new File(args[1]).text // e.g. 'dependencies.txt'

def list = new XmlSlurper(false, false).parse(dependencyCheckReportPath)
list."dependencies"."dependency".each { content ->
  content."vulnerabilities"."vulnerability".each {
    it.description.replaceBody(it.description.text() + "\nDependencies:\n" + dependencies)
  }
}
def xmlUtil = new groovy.xml.XmlUtil()
String xmlString = xmlUtil.serialize(list)
xmlUtil.serialize(list, new FileWriter(new File(dependencyCheckReportPath)))
