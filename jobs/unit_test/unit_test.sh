#!/bin/bash 

set -ex

prepare_deps(){
pushd ${WORKSPACE}
mkdir -p xunit-reports
./build-config/build-release-tools/HWIMO-BUILD ./build-config/build-release-tools/application/reprove.py \
--manifest ${MANIFEST_FILE_PATH} \
--builddir ${WORKSPACE}/build-deps \
--jobs 8 \
--force \
checkout \
packagerefs

popd
}

unit_test(){
    echo "Run unit test under $1"
    npm install

    set +e
    ./node_modules/.bin/istanbul report lcov
    npm install --save-dev mocha-sonar-reporter
    npm_package_config_mocha_sonar_reporter_classname="Tests_build.spec" npm_package_config_mocha_sonar_reporter_outputfile=test/$1.xml ./node_modules/.bin/istanbul cover -x "**/spec/**" ./node_modules/.bin/_mocha -- $(find spec -name '*-spec.js') -R mocha-sonar-reporter --require spec/helper.js
    set -e
}



prepare_deps

pushd ${WORKSPACE}/build-deps/$1
unit_test $1
cp test/$1.xml ${WORKSPACE}/xunit-reports
popd
