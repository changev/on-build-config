#!/bin/bash 

set -ex

manifest_file=$1

pushd ${WORKSPACE}

pwd

mkdir -p xunit-reports

./build-config/build-release-tools/HWIMO-BUILD ./build-config/build-release-tools/application/reprove.py \
--manifest ${manifest_file} \
--builddir ${WORKSPACE}/build-deps \
--jobs 8 \
--force \
checkout \
packagerefs

popd
