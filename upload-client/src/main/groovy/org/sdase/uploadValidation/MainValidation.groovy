package org.sdase.uploadValidation

public class MainValidation {
    static void main(String[] args) {
        String dojoUrl = System.getenv("DEFECTDOJO_URL")
        if (dojoUrl == null) {
            dojoUrl = System.getenv("DD_URL")
        }
        println "dojoUrl: ${dojoUrl}"
        String token = System.getenv("DEFECTDOJO_APIKEY")
        if(token == null) {
            token = System.getenv("DD_TOKEN")
        }
        String dojoUser = System.getenv("DEFECTDOJO_USERNAME")
        if(dojoUser == null) {
            dojoUser = System.getenv("DD_USER")
        }
        if(!token) {
            println "Error: No token"
            return
        }
        String scanType = System.getenv("DD_REPORT_TYPE") ?: "Dependency Check Scan"
        println "using scanType ${scanType}"
        org.sdase.uploadValidation.UploadValidator.main dojoToken: token,
                dojoUser: dojoUser,
                dojoUrl: dojoUrl
    }
}