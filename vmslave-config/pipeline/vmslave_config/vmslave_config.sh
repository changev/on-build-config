#!/bin/bash
set -x
mv vmslave-config/pipeline/group_vars/* vmslave-config/ansible/group_vars
hosts_file=`readlink -f hosts`
export ANSIBLE_INVENTORY=$hosts_file
cd vmslave-config/ansible
ansible-playbook docker_build_vmslave_config.yml
ansible-playbook prgate_vmslave_config.yml
ansible-playbook smoke_test_vmslave_config.yml

