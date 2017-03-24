#!/bin/bash -ex

export VCOMPUTE=("${NODE_NAME}-Rinjin1","${NODE_NAME}-Rinjin2","${NODE_NAME}-Quanta")
VCOMPUTE="${VCOMPUTE}"
if [ -z "${VCOMPUTE}" ]; then
  VCOMPUTE=("jvm-Quanta_T41-1" "jvm-vRinjin-1" "jvm-vRinjin-2")
fi

cleanupVMs(){
    vagrantDestroy
    # Suspend any other running vagrant boxes
    vagrantSuspendAll

    # Delete any running VMs
    virtualBoxDestroyAll

    rm -rf "$HOME/VirtualBox VMs"
}

vagrantSuspendAll() {
 for box in `vagrant global-status --prune | awk '/running/{print $1}'`; do
     vagrant suspend ${box}
 done
}

vagrantDestroy() {
  cd ${WORKSPACE}/RackHD/example
  vagrant destroy -f
}

virtualBoxDestroyAll() {
  set +e
  for uuid in `vboxmanage list vms | awk '{print $2}' | tr -d '{}'`; do
    echo "shutting down vm ${uuid}"
    vboxmanage controlvm ${uuid} poweroff
    echo "deleting vm ${uuid}"
    vboxmanage unregistervm ${uuid}
  done
  set -e
}

nodesDelete() {
  cd ${WORKSPACE}/build-config/deployment/
  if [ "${USE_VCOMPUTE}" != "false" ]; then
    if [ $OVA_POST_TEST == "true" ]; then
      VCOMPUTE+=("${NODE_NAME}-ova-for-post-test")
    fi
    for i in ${VCOMPUTE[@]}; do
      ./vm_control.sh "${ESXI_HOST},${ESXI_USER},${ESXI_PASS},delete,1,${i}_*"
    done
  fi
}

cleanupENVProcess() {
  # Kill possible socat process left by ova-post-smoke-test
  # eliminate the effect to other test
  socat_process=`ps -ef | grep socat | grep -v socat | awk '{print $2}' | xargs`
  if [ -n "$socat_process" ]; then
    kill $socat_process
  fi
}

cleanupVMs
nodesDelete
cleanupENVProcess