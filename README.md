# DefectDojo Java Client Library
This repository uses OWASP SecureCodeBox defectdojo client library for API calls against OWASP DefectDojo. It is especially useful for Jenkins builds in the Jenkinsfile.

# Development
* `defectdojo.groovy` simulates Jenkins, all parameters (e.g. token) needs to be adjusted here
* run

```
EXPORT DD_TOKEN=XXX
/runTest.bash
```

# Exit Codes
* 0: Scan ok, no unhandled vulnerabilties
* 1: Error, e.g. mandatory parameters are missing
* 2: Engagement not found
* 10: Unhandled vulnerabilities exists (configureable)

# Credits
This project is based on https://github.com/secureCodeBox/defectdojo-client-java
