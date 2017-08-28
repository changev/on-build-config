package pipeline.rackhd.ova

def buildOVAFromVMX(String ansible_playbook, String os_ver, String rackhd_version,String rackhd_dir, String rackhd_apt_repo, String target_dir, String cache_image_dir){
    timeout(90){
        sh """#!/bin/bash -ex
        pushd $rackhd_dir/packer
          ./build_ova.sh  --RACKHD_DIR $rackhd_dir \
                          --OS_VER $os_ver \
                          --RACKHD_VERSION $rackhd_version \
                          --DEBIAN_REPOSITORY "$rackhd_apt_repo" \
                          --CACHE_IMAGE_DIR $cache_image_dir/RackHD/packer/output-vmware-iso \
                          --TARGET_DIR $target_dir \
                          --ANSIBLE_PLAYBOOK $ansible_playbook \
                          --BUILD_STAGE "BUILD_FINAL"
        popd
        """
    }
}

def buildOVAFromISO(String ansible_playbook, String os_ver, String rackhd_version,String rackhd_dir, String rackhd_apt_repo, String target_dir){
    timeout(90){
        sh """#!/bin/bash -ex
        pushd $rackhd_dir/packer
          ./build_ova.sh  --RACKHD_DIR $rackhd_dir \
                          --OS_VER $os_ver \
                          --RACKHD_VERSION $rackhd_version \
                          --DEBIAN_REPOSITORY "$rackhd_apt_repo" \
                          --TARGET_DIR $target_dir \
                          --ANSIBLE_PLAYBOOK $ansible_playbook \
                          --BUILD_STAGE "BUILD_ALL"
        popd
        """
    }
}

def buildOVA(String rackhd_repo_url, String rackhd_commit, String rackhd_version, String rackhd_apt_repo, String os_ver, String cache_job_name=""){
    String label_name = "packer_ova"
    def share_method = new pipeline.common.ShareMethod()
    lock(label:label_name,quantity:1){
        String node_name = share_method.occupyAvailableLockedResource(label_name, [])
        node(node_name){
            deleteDir()
            String rackhd_dir = "$WORKSPACE/RackHD"
            String target_dir = "$WORKSPACE/ova" //A dir stores all the output of build process

            // Checkout RackHD
            share_method.checkout(rackhd_repo_url, rackhd_commit, rackhd_dir)

            // Choose ansible playbook, cause the apt source has been replaced so both playbook is ok here.
            String ansible_playbook = "rackhd_package_mini"

            if(cache_job_name == ""){
                buildOVAFromISO(ansible_playbook, os_ver, rackhd_version, rackhd_dir, rackhd_apt_repo, target_dir)
            }
            else{
                String cache_image_dir = "$WORKSPACE/cache_image"
                // Copy ovf from the artifact of project VAGRANT_CACHE_BUILD
                share_method.copyArtifact(cache_job_name, cache_image_dir)
                buildOVAFromVMX(ansible_playbook, os_ver, rackhd_version, rackhd_dir, rackhd_apt_repo, target_dir, cache_image_dir)
            }

            // After build step, target_dir has been full of ova outputs
            dir(target_dir){
                ova_name = sh( returnStdout: true, script: 'find -name *.ova  -printf %f' ).trim()
                stash name: "ova", includes: "$ova_name"
            }
            // Differ from stash, in order to classify artifacts, the outputs should be archived with dir path
            relative_path = target_dir.replaceAll("$WORKSPACE/", "")
            archiveArtifacts """$relative_path/*.ova, $relative_path/*.log, $relative_path/*.md5, $relative_path/*.sha"""

            // Construst ova_dict for possible next steps
            def ova_dict = [:]
            ova_dict["stash_name"] = "ova"
            ova_dict["stash_path"] = "$ova_name"
            return ova_dict
        }
    }
}
