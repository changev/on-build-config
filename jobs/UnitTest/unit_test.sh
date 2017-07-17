#!/bin/bash -ex

start_depends_services(){
    set +e
    echo $SUDO_PASSWORD |sudo -S service mongodb start
    echo $SUDO_PASSWORD |sudo -S service rabbitmq-server start
    set -e
}

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
    if [ -d "build-deps/on-build-config" ]; then
        rm -rf build-config
        cp -r build-deps/on-build-config build-config
    fi
    mongo pxe --eval "db.dropDatabase()"
    mongo monorail-test --eval "db.dropDatabase()"
    cd build-config && ./build-config "$1"
    cd ..
    popd
}

unit_test(){
    echo "Run unit test under $1"
    npm install
    npm install --save-dev mocha-sonar-reporter

    if [ "$1" == "on-core" ];then
      ./node_modules/.bin/istanbul cover -x '**/spec/**' ./node_modules/.bin/_mocha -- spec/lib/di-spec.js spec/lib/services/log-publisher-spec.js spec/lib/services/configuration-spec.js spec/lib/services/messenger-spec.js spec/lib/services/graph-progress-spec.js spec/lib/services/waterline-spec.js spec/lib/services/hook-spec.js spec/lib/services/lookup-spec.js spec/lib/services/heartbeat-spec.js spec/lib/services/encryption-spec.js spec/lib/services/core-spec.js spec/lib/services/environment-spec.js spec/lib/services/argument-handler-spec.js spec/lib/services/statsd-spec.js spec/lib/extensions-spec.js spec/lib/models/work-item-spec.js spec/lib/models/base-spec.js spec/lib/models/sku-spec.js spec/lib/models/file-spec.js spec/lib/models/localusers-spec.js spec/lib/models/tags-spec.js spec/lib/models/catalog-spec.js spec/lib/models/lookup-spec.js spec/lib/models/task-definition-spec.js spec/lib/models/log-spec.js spec/lib/models/template-spec.js spec/lib/models/obms-spec.js spec/lib/models/node-spec.js spec/lib/models/graph-object-spec.js spec/lib/models/roles-spec.js spec/lib/models/graph-definition-spec.js spec/lib/models/profile-spec.js spec/lib/protocol/waterline-spec.js spec/lib/protocol/tftp-spec.js spec/lib/protocol/events-spec.js spec/lib/protocol/http-spec.js spec/lib/protocol/task-graph-runner-spec.js spec/lib/protocol/scheduler-spec.js spec/lib/protocol/dhcp-spec.js spec/lib/protocol/task-spec.js spec/lib/serializables/result-spec.js spec/lib/serializables/ip-address-spec.js spec/lib/serializables/log-event-spec.js spec/lib/serializables/error-event-spec.js spec/lib/serializables/mac-address-spec.js spec/lib/common/child-process-spec.js spec/lib/common/assert-spec.js spec/lib/common/message-spec.js spec/lib/common/system-uuid-spec.js spec/lib/common/json-schema-validator-spec.js spec/lib/common/graph-progress-spec.js spec/lib/common/model-spec.js spec/lib/common/arp-spec.js spec/lib/common/util-spec.js spec/lib/common/db-renderable-content-spec.js spec/lib/common/http-tool-spec.js spec/lib/common/encryption-spec.js spec/lib/common/subscription-spec.js spec/lib/common/constants-spec.js spec/lib/common/file-loader-spec.js spec/lib/common/validatable-spec.js spec/lib/common/logger-spec.js spec/lib/common/errors-spec.js spec/lib/common/serializable-spec.js spec/lib/common/profile-spec.js spec/lib/common/sanitizer-spec.js spec/lib/common/timer-spec.js spec/lib/common/connection-spec.js spec/lib/workflow/stores/store-spec.js spec/lib/workflow/stores/mongo-spec.js spec/lib/workflow/messengers/messenger-spec.js spec/lib/workflow/messengers/messenger-AMQP-spec.js -R mocha-sonar-reporter --require spec/helper.js -t 10000
      retrurn $?
    elif [ "$1" == "on-http" ]; then
    ./node_modules/.bin/istanbul cover -x '**/spec/**' ./node_modules/.bin/_mocha -- spec/lib/services/obm-api-service-spec.js spec/lib/services/file-service-spec.js spec/lib/services/taskgraph-api-service-spec.js spec/lib/services/schema-api-service-spec.js spec/lib/services/pollers-api-service-spec.js spec/lib/services/versions-api-service-spec.js spec/lib/services/swagger-api-service-spec.js spec/lib/services/rest-api-service-spec.js spec/lib/services/workflow-api-service-spec.js spec/lib/services/hooks-api-service-spec.js spec/lib/services/profiles-api-service-spec.js spec/lib/services/gridfs-service-spec.js spec/lib/services/redfish-validator-service-spec.js spec/lib/services/upnp-service-spec.js spec/lib/services/sku-pack-service-spec.js spec/lib/services/templates-api-service-spec.js spec/lib/services/files/file-plugin-spec.js spec/lib/services/http-service-spec.js spec/lib/services/nodes-api-service-spec.js spec/lib/services/account-api-service-spec.js spec/lib/services/catalogs-api-service-spec.js spec/lib/services/notification-api-service-spec.js spec/lib/services/static-files-api-service-spec.js spec/lib/services/tags-api-service-spec.js spec/lib/fittings/json-error-handler-spec.js spec/lib/api/redfish-1.0/registry-spec.js spec/lib/api/redfish-1.0/chassis-spec.js spec/lib/api/redfish-1.0/schemas-spec.js spec/lib/api/redfish-1.0/update-service-spec.js spec/lib/api/redfish-1.0/managers-spec.js spec/lib/api/redfish-1.0/roles-spec.js spec/lib/api/redfish-1.0/task-service-spec.js spec/lib/api/redfish-1.0/event-service-spec.js spec/lib/api/redfish-1.0/system-spec.js spec/lib/api/redfish-1.0/account-service-spec.js spec/lib/api/redfish-1.0/session-service-spec.js spec/lib/api/redfish-1.0/metadata-spec.js spec/lib/api/redfish-1.0/service-root-spec.js spec/lib/api/2.0/files-spec.js spec/lib/api/2.0/templates-spec.js spec/lib/api/2.0/profiles-spec.js spec/lib/api/2.0/obms-spec.js spec/lib/api/2.0/tasks-spec.js spec/lib/api/2.0/ibms-spec.js spec/lib/api/2.0/schemas-spec.js spec/lib/api/2.0/catalogs-spec.js spec/lib/api/2.0/skus-spec.js spec/lib/api/2.0/workflowGraphs-spec.js spec/lib/api/2.0/users-spec.js spec/lib/api/2.0/workflows-spec.js spec/lib/api/2.0/workflowTasks-spec.js spec/lib/api/2.0/lookups-spec.js spec/lib/api/2.0/views-spec.js spec/lib/api/2.0/roles2.0-spec.js spec/lib/api/2.0/notification-spec.js spec/lib/api/2.0/tags-spec.js spec/lib/api/2.0/hooks-spec.js spec/lib/api/2.0/nodes-spec.js spec/lib/api/2.0/config-spec.js spec/lib/api/2.0/pollers-spec.js spec/lib/api/login/login-spec.js spec/lib/common/file-loader-spec.js spec/lib/serializables/v1/node-spec.js spec/lib/serializables/v1/obm-spec.js spec/lib/serializables/v1/ssh-spec.js spec/lib/serializables/v1/snmp-spec.js spec/data/templates/get_driveid-spec.js -R mocha-sonar-reporter --require spec/helper.js -t 10000
    return $?
   fi

    set +e
    ./node_modules/.bin/istanbul report lcov
    npm_package_config_mocha_sonar_reporter_classname="Tests_build.spec" npm_package_config_mocha_sonar_reporter_outputfile=test/$1.xml ./node_modules/.bin/istanbul cover -x "**/spec/**" ./node_modules/.bin/_mocha -- $(find spec -name '*-spec.js') -R mocha-sonar-reporter --require spec/helper.js -t 10000
    set -e
}

start_depends_services
prepare_deps $1
pushd ${WORKSPACE}/build-deps/$1
unit_test $1
cp test/$1.xml ${WORKSPACE}/xunit-reports
popd
