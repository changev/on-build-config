#!/bin/bash -ex

./on-build-config/build-release-tools/HWIMO-BUILD ./on-build-config/build-release-tools/application/pr_parser.py \
--change-url $ghprbPullLink \
--target-branch $ghprbTargetBranch \
--ghtoken ${GITHUB_TOKEN} \
--manifest-file-path "${stash_manifest_path}"
