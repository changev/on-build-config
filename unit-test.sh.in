#!/bin/bash +xe
runUnitTest() {
   cd ${WORKSPACE}/build
   #npm cache clean >& /dev/null
   rm -rf ./node_modules && npm install && npm test 
   #npm cache clean && npm install && npm install mocha-jenkins-reporter
   #JUNIT_REPORT_PATH=report.xml JUNIT_REPORT_STACK=1 \
   #    ./node_modules/.bin/mocha $(find spec -name '*-spec.js') -R spec --require spec/helper.js --reporter mocha-jenkins-reporter || true 
   #cp -rf report.xml ${WORKSPACE}/xunit-reports/mocha-report.xml
   REPO_NAME=`pushd ${WORKSPACE}/build >/dev/null && git remote show origin -n | grep "Fetch URL:" | sed "s#^.*/\(.*\).git#\1#" && popd > /dev/null`
   if [ "$REPO_NAME" == "on-http" ]; then
    npm run apidoc
    npm run taskdoc
   fi
}
runUnitTest
exit 0
