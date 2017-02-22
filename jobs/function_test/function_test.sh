#!/bin/bash +xe
set -x

export VCOMPUTE=("${NODE_NAME}-Rinjin1","${NODE_NAME}-Rinjin2","${NODE_NAME}-Quanta")
REPOS=("on-http" "on-taskgraph" "on-dhcp-proxy" "on-tftp" "on-syslog")
HTTP_STATIC_FILES="${HTTP_STATIC_FILES}"
TFTP_STATIC_FILES="${TFTP_STATIC_FILES}"
MANIFEST_FILE="${MANIFEST_FILE}"

if [ ! -z "${1}" ]; then
  HTTP_STATIC_FILES=$1
fi
if [ ! -z "${2}" ]; then
  TFTP_STATIC_FILES=$2
fi
SKIP_PREP_DEP="${SKIP_PREP_DEP}"
if [ ! -z "${3}" ]; then
  SKIP_PREP_DEP=$3
fi
RUN_FIT_TEST="${RUN_FIT_TEST}"
if [ ! -z "${4}" ]; then
  RUN_FIT_TEST=$4
fi
RUN_CIT_TEST="${RUN_CIT_TEST}"
if [ ! -z "${5}" ]; then
  RUN_FIT_TEST=$5
fi
MODIFY_API_PACKAGE="${MODIFY_API_PACKAGE}"

dlHttpFiles() {
  dir=${WORKSPACE}/build-deps/on-http/static/http/common
  mkdir -p ${dir} && cd ${dir}
  if [ -n "${INTERNAL_HTTP_ZIP_FILE_URL}" ]; then
    # use INTERNAL TEMP SOURCE
    wget ${INTERNAL_HTTP_ZIP_FILE_URL} 
    unzip common.zip && mv common/* . && rm -rf common
  else
    # pull down index from bintray repo and parse files from index
    wget --no-check-certificate https://dl.bintray.com/rackhd/binary/builds/ && \
        exec  cat index.html |grep -o href=.*\"|sed 's/href=//' | sed 's/"//g' > files
    for i in `cat ./files`; do
      wget --no-check-certificate https://dl.bintray.com/rackhd/binary/builds/${i}
    done
    # attempt to pull down user specified static files
    for i in ${HTTP_STATIC_FILES}; do
      wget --no-check-certificate https://bintray.com/artifact/download/rackhd/binary/builds/${i}
    done
  fi
}

dlTftpFiles() {
  dir=${WORKSPACE}/build-deps/on-tftp/static/tftp
  mkdir -p ${dir} && cd ${dir}
  if [ -n "${INTERNAL_TFTP_ZIP_FILE_URL}" ]; then
    # use INTERNAL TEMP SOURCE
    wget ${INTERNAL_TFTP_ZIP_FILE_URL} 
    unzip pxe.zip && mv pxe/* . && rm -rf pxe pxe.zip
  else
    # pull down index from bintray repo and parse files from index
    wget --no-check-certificate https://dl.bintray.com/rackhd/binary/ipxe/ && \
        exec  cat index.html |grep -o href=.*\"|sed 's/href=//' | sed 's/"//g' > files
    for i in `cat ./files`; do
      wget --no-check-certificate https://dl.bintray.com/rackhd/binary/ipxe/${i}
    done
    # attempt to pull down user specified static files
    for i in ${TFTP_STATIC_FILES}; do
      wget --no-check-certificate https://bintray.com/artifact/download/rackhd/binary/ipxe/${i}
    done
  fi
}

preparePackages() {
      pushd ${WORKSPACE}
      ./on-build-config/build-release-tools/HWIMO-BUILD ./on-build-config/build-release-tools/application/reprove.py \
      --manifest ${MANIFEST_FILE} \
      --builddir ${WORKSPACE}/build-deps \
      --jobs 8 \
      --force \
      checkout \
      packagerefs

      for i in ${REPOS[@]}; do
          pushd ${WORKSPACE}/build-deps/${i}
          npm install --production
          popd
      done
      cp -r build-deps/RackHD .
      cp -r build-deps/on-build-config build-config
      pushd build-config
      ./build-config
      popd

      popd
}

cleanupVMs(){
    rm -rf "$HOME/VirtualBox VMs"
}
prepareDeps(){
  preparePackages
  dlTftpFiles
  dlHttpFiles
}

apiPackageModify() {
    pushd ${WORKSPACE}/build-deps/on-http/extra
    sed -i "s/.*git symbolic-ref.*/ continue/g" make-deb.sh
    sed -i "/build-package.bash/d" make-deb.sh
    sed -i "/GITCOMMITDATE/d" make-deb.sh
    sed -i "/mkdir/d" make-deb.sh
    bash make-deb.sh
    popd
    for package in ${API_PACKAGE_LIST}; do
      pip uninstall -y ${package//./-}
      pushd ${WORKSPACE}/build-deps/on-http/$package
        fail=true
        while $fail; do
          python setup.py install
          if [ $? -eq 0 ];then
        	  fail=false
          fi
        done
      popd
    done
}

VCOMPUTE="${VCOMPUTE}"
if [ -z "${VCOMPUTE}" ]; then
  VCOMPUTE=("jvm-Quanta_T41-1" "jvm-vRinjin-1" "jvm-vRinjin-2")
fi

TEST_GROUP="${TEST_GROUP}"
if [ -z "${TEST_GROUP}" ]; then
   TEST_GROUP="smoke-tests"
fi

nodesOff() {
  cd ${WORKSPACE}/build-config/deployment/
  if [ "${USE_VCOMPUTE}" != "false" ]; then
    for i in ${VCOMPUTE[@]}; do
      ./vm_control.sh "${ESXI_HOST},${ESXI_USER},${ESXI_PASS},power_off,1,${i}_*"
    done
  else
     ./telnet_sentry.exp ${SENTRY_HOST} ${SENTRY_USER} ${SENTRY_PASS} off ${OUTLET_NAME}
     sleep 5
  fi
}

nodesOn() {
  cd ${WORKSPACE}/build-config/deployment/
  if [ "${USE_VCOMPUTE}" != "false" ]; then
    for i in ${VCOMPUTE[@]}; do
      ./vm_control.sh "${ESXI_HOST},${ESXI_USER},${ESXI_PASS},power_on,1,${i}_*"
    done
  else
     ./telnet_sentry.exp ${SENTRY_HOST} ${SENTRY_USER} ${SENTRY_PASS} on ${OUTLET_NAME}
     sleep 5
  fi
}

nodesDelete() {
  cd ${WORKSPACE}/build-config/deployment/
  if [ "${USE_VCOMPUTE}" != "false" ]; then
    for i in ${VCOMPUTE[@]}; do
      ./vm_control.sh "${ESXI_HOST},${ESXI_USER},${ESXI_PASS},delete,1,${i}_*"
    done
  fi
}

nodesCreate() {
  cd ${WORKSPACE}/build-config/deployment/
  if [ "${USE_VCOMPUTE}" != "false" ]; then
    for i in {1..2}
    do
       ovftool --noSSLVerify --diskMode=${DISKMODE} --datastore=${DATASTORE}  --name="${NODE_NAME}-Rinjin${i}" --net:"${NIC}=${NODE_NAME}-switch" "${HOME}/isofarm/OVA/vRinjin-Haswell.ova"   vi://${ESXI_USER}:${ESXI_PASS}@${ESXI_HOST}
    done
    ovftool  --noSSLVerify --diskMode=${DISKMODE} --datastore=${DATASTORE} --name="${NODE_NAME}-Quanta" --net:"${NIC}=${NODE_NAME}-switch" "${HOME}/isofarm/OVA/vQuanta-T41-Haswell.ova"   vi://${ESXI_USER}:${ESXI_PASS}@${ESXI_HOST}
  else
    nodesOff
  fi
}

CONFIG_PATH=${CONFIG_PATH-build-config/vagrant/config/mongo}
vagrantUp() {
  cd ${WORKSPACE}/RackHD/example
  cp -rf ${WORKSPACE}/build-config/vagrant/* .
  CONFIG_DIR=${CONFIG_PATH} WORKSPACE=${WORKSPACE}  vagrant up --provision
  if [ $? -ne 0 ]; then 
      echo "Vagrant up failed."
      exit 1
  fi
}

vagrantDestroy() {
  cd ${WORKSPACE}/RackHD/example
  vagrant destroy -f
  nodesDelete
}

vagrantHalt() {
  cd ${WORKSPACE}/RackHD/example
  vagrant halt
}

vagrantSuspendAll() {
  for box in `vagrant global-status --prune | awk '/running/{print $1}'`; do
    vagrant suspend ${box}
  done
}

virtualBoxDestroyRunning() {
  for uuid in `vboxmanage list runningvms | awk '{print $2}' | tr -d '{}'`; do
    echo "shutting down vm ${uuid}"
    vboxmanage controlvm ${uuid} poweroff
    echo "deleting vm ${uuid}"
    vboxmanage unregistervm ${uuid}
  done
}

generateSolLog(){
  cd ${WORKSPACE}/RackHD/example
  vagrant ssh -c 'cd /home/vagrant/src/build-config/; \
        bash generate-sol-log.sh' > ${WORKSPACE}/sol.log &
}

setupVirtualEnv(){
  pushd ${WORKSPACE}/RackHD/test
  rm -rf .venv/on-build-config
  ./mkenv.sh on-build-config
  source myenv_on-build-config
  popd
  if [ "$MODIFY_API_PACKAGE" == true ] ; then
    apiPackageModify
  fi
}

BASE_REPO_URL="${BASE_REPO_URL}"
runTests() {
  if [ "$RUN_FIT_TEST" == true ] ; then
     cd ${WORKSPACE}/RackHD/test
     #TODO Parameterize FIT args
     python run_tests.py -test deploy/rackhd_stack_init.py -stack vagrant  -port 9090 -xunit
     python run_tests.py -test tests -group smoke -stack vagrant -port 9090 -ora localhost -v 9 -xunit
     mkdir -p ${WORKSPACE}/xunit-reports
     cp *.xml ${WORKSPACE}/xunit-reports
  fi
  if [ "$RUN_CIT_TEST" == true ] ; then
     read array<<<"${TEST_GROUP}"
     args=()
     group=" --group="
     for item in $array; do
        args+="${group}${item}"
     done
     cp -f ${WORKSPACE}/build-config/config.ini ${WORKSPACE}/RackHD/test/config
     cd ${WORKSPACE}/RackHD/test
     RACKHD_BASE_REPO_URL=${BASE_REPO_URL} RACKHD_TEST_LOGLVL=INFO \
     python run.py ${args} --with-xunit
     mkdir -p ${WORKSPACE}/xunit-reports
     cp *.xml ${WORKSPACE}/xunit-reports
  fi
}


waitForAPI() {
  timeout=0
  maxto=60
  url=http://localhost:9090/api/1.1/nodes
  while [ ${timeout} != ${maxto} ]; do
    wget --retry-connrefused --waitretry=1 --read-timeout=20 --timeout=15 -t 1 --continue ${url}
    if [ $? = 0 ]; then 
      break
    fi
    sleep 10
    timeout=`expr ${timeout} + 1`
  done
  if [ ${timeout} == ${maxto} ]; then
    echo "Timed out waiting for RackHD API service (duration=`expr $maxto \* 10`s)."
    exit 1
  fi
}
if [ "$SKIP_PREP_DEP" == false ] ; then
  # Prepare the latest dependent repos to be shared with vagrant
  prepareDeps
  nodesDelete
fi


#testa(){
if [ "$RUN_CIT_TEST" == true ] || [ "$RUN_FIT_TEST" == true ] ; then
  echo "start to tun test"

#fi

#test(){
  # register the signal handler to clean up( vagrantDestroy ), with process being killed
  trap vagrantDestroy SIGINT SIGTERM SIGKILL

  # it's dangerous, it will impact other running vagrant instance. in theory, "vagrant destory" in test.sh will remove cache data . but actually, we still see some slave's  "~/VirtualBox VMs"  folder huge with dirty boxes.
  cleanupVMs

  # based on the assumption that in the same folder, the VMs has been exist normally. so don't destroy VM here.
  
  nodesCreate
  # Power on vagrant box and nodes 
  vagrantUp
  # We setup the virtual-environment here, since once we
  # call "nodesOn", it's a race to get to the first test
  # before the nodes get booted far enough to start being
  # seen by RackHD. Logically, it would be better IN runTests.
  # We do it between the vagrant and waitForAPI to use the
  # time to make the env instead of doing sleeps...
  setupVirtualEnv
  waitForAPI
  nodesOn
  generateSolLog
  # Run tests
  runTests

  # Clean Up below

  #shutdown vagrant box and delete all resource (like removing vm disk files in "~/VirtualBox VMs/")
  vagrantDestroy

  # Suspend any other running vagrant boxes
  #vagrantSuspendAll

  # Delete any running VMs
  #virtualBoxDestroyRunning

fi

if [ "$MODIFY_API_PACKAGE" == true ] ; then
 sudo rm -rf ${WORKSPACE}/build-deps/on-http
fi

