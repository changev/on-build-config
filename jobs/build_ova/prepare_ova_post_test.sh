#!/bin/bash +xe
set -x
OVA=`ls ${OVA_PATH}`

echo "Post Test starts "

deployOva() {
    echo yes | ovftool \
    --overwrite --powerOffTarget --powerOn --skipManifestCheck \
    --net:"CONTROL=${NODE_NAME}-switch" \
    --datastore=${DATASTORE} \
    --name=${NODE_NAME}-ova-for-post-test \
    ${OVA} \
    "vi://${VCENTER_NT_USER}:${VCENTER_NT_PASSWORD}@${VCENTER_IP}/${VCENTER_PATH}/${ESXI_HOST}"/
    if [ $? = 0 ]; then
        echo "[Info] Deploy OVA successfully".
    else
        echo "[Error] Deploy OVA failed."
        exit 3
    fi
    ssh-keygen -f "$HOME/.ssh/known_hosts" -R $OVA_INTERNAL_IP
}

waitForAPI() {
  service_normal_sentence="No auth token"
  timeout=0
  maxto=60
  set +e
  url=http://$OVA_INTERNAL_IP:8080/api/2.0/nodes
  while [ ${timeout} != ${maxto} ]; do
    api_test_result=`curl ${url}`
    echo $api_test_result | grep "$service_normal_sentence" > /dev/null  2>&1
    if [ $? = 0 ]; then
      echo "[Debug] successful.        in this retry time: OVA ansible returns: $api_test_result"
      break
    fi
    sleep 10
    timeout=`expr ${timeout} + 1`
  done
  set -e
  if [ ${timeout} == ${maxto} ]; then
    echo "Timed out waiting for RackHD API service (duration=`expr $maxto \* 10`s)."
    exit 1
  fi
}

configOVA() {
  # config the OVA for post test
  pushd ${WORKSPACE}/build-config/jobs/build_ova/ansible
    echo "ova-post-test ansible_host=$OVA_INTERNAL_IP ansible_user=$OVA_USER ansible_ssh_pass=$OVA_PASSWORD ansible_become_pass=$OVA_PASSWORD" > hosts
    cp -f ${WORKSPACE}/build-config/vagrant/config/mongo/config.json .
    ansible-playbook -i hosts main.yml --tags "before-test"
  popd
}

deployOva
waitForAPI
configOVA


echo "Finished preparation for ova post smoke test"
