package org.sdase.cleanupTestsAndProducts

public class MainCleanupTestsAndProducts {
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

        org.sdase.cleanupTestsAndProducts.CleanupTestsAndProducts.main dojoToken: token,
                dojoUser: dojoUser,
                dojoUrl: dojoUrl
    }
}