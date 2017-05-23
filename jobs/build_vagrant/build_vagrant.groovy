import groovy.transform.Field;
@Field def shareMethod
node{
    deleteDir()
    checkout scm
    shareMethod = load("jobs/ShareMethod.groovy")
}


lock("test"){
    String label_name = "smoke_test"
    lock(label:label_name,quantity:1){
        resources_name = shareMethod.getLockedResourceName(label_name)
        if(resources_name.size>0){
            node_name = resources_name[0]
        }
        else{
            error("Failed to find resource with label " + label_name)
        }
        node(node_name){
            timestamps{
                withEnv([
                    "RACKHD_COMMIT=${env.RACKHD_COMMIT}",
                    "RACKHD_VERSION=${env.RACKHD_VERSION}",
                    "IS_OFFICIAL_RELEASE=${env.IS_OFFICIAL_RELEASE}",
                    "OS_VER=${env.OS_VER}",
                    "BUILD_TYPE=virtualbox",
                    "BINTRAY_SUBJECT=${env.BINTRAY_SUBJECT}",
                    "BINTRAY_REPO=debian",
                    "CI_BINTRAY_SUBJECT=${env.CI_BINTRAY_SUBJECT}",
                    "CI_BINTRAY_REPO=debian",
                    "BINTRAY_COMPONENT=main", 
                    "BINTRAY_DISTRIBUTION=trusty", 
                    "BINTRAY_ARCHITECTURE=amd64"]){
                    def current_workspace = pwd()
                    deleteDir()
                    dir("on-build-config"){
                        checkout scm
                    }
                    def url = "https://github.com/RackHD/RackHD.git"
                    def branch = "${env.RACKHD_COMMIT}"
                    def targetDir = "build"
                    //shareMethod.checkout(url, branch, targetDir)
                
                    step ([$class: 'CopyArtifact',
                    projectName: 'VAGRANT_CACHE_BUILD',
                    target: 'cache_image']);
                    
                    timeout(180){
                        withEnv(["WORKSPACE=${current_workspace}"]){
                            sh 'mkdir -p build/packer && cd build/packer && wget http://10.240.19.21/job/Z-MC-V-PP/1/artifact/build/packer/rackhd-ubuntu-14.04-2.6.0-20170521UTC.box'
                        }
                    }
                    archiveArtifacts 'build/packer/*.box, build/packer/*.log'
                    stash name: 'vagrant', includes: 'build/packer/*.box'
                    env.VAGRANT_WORKSPACE="${current_workspace}"
                    env.VAGRANT_STASH_NAME = "vagrant"
                    env.VAGRANT_STASH_PATH = "build/packer/*.box"
                    echo "${env.VAGRANT_WORKSPACE}"
                }
            }
        }
    }
}
