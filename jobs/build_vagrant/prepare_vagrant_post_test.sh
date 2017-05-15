#!/bin/bash +xe
set -e
vagrant global-status --prune

BOX=`readlink -f ${VAGRANT_STASH_PATH}`
sed -e "s#rackhd/rackhd#${BOX}#g" \
    build-config/jobs/build_vagrant/Vagrantfile.in > $RackHD_DIR/example/Vagrantfile
CONFIG_PATH=`readlink -f build-config/vagrant/config/mongo/`

# Make sure this path is synced with cleanup.sh
WORKSPACE=`pwd`
cd $RackHD_DIR/example
CONFIG_DIR=${CONFIG_PATH} WORKSPACE=${WORKSPACE} vagrant up --provision
