# DefectDojo Java Client Library
This repository uses OWASP SecureCodeBox defectdojo client library for API calls against OWASP DefectDojo. It is especially useful for Jenkins builds in the Jenkinsfile.

# Development
* `defectdojo.groovy` simulates Jenkins, all parameters (e.g. token) needs to be adjusted here
* Copy libs and run
`/copyLibs.bash && groovy defectdojo.groovy`

# Local Build
`buildah unshare ./build.sh`
## Test build like on jenkins (best on rhel 8)
`BUILD_EXPORT_OCI_ARCHIVES=true buildah unshare ./build.sh`

# Exit Codes
* 0: Scan ok, no unhandled vulnerabilties
* 1: Error, e.g. mandatory parameters are missing
* 2: Engagement not found
* 10: Unhandled vulnerabilities exists

# Credits
This project is based on https://github.com/secureCodeBox/defectdojo-client-java
