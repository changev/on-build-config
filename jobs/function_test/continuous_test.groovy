@NonCPS
def printParams() {
  env.getEnvironment().each { name, value -> println "Name: $name -> Value $value" }
}
node{
    deleteDir()
    dir("Jenkinsfile_Library"){
        checkout scm
    }
    try{
        stage("Prepare Manifest"){
            node{
                deleteDir()
                dir("build-config"){
                    git 'https://github.com/PengTian0/on-build-config'
                }
                withEnv([
                    "branch=${env.branch}",
                    "date=current"]){

                    sh '''#!/bin/bash
                    ./build-config/build-release-tools/HWIMO-BUILD build-config/build-release-tools/application/generate_manifest.py \
                    --branch "$branch" \
                    --date "$date" \
                    --timezone "-0500" \
                    --builddir b \
                    --force \
                    --jobs 8

                    arrBranch=($(echo $branch | tr "/" "\n"))
                    slicedBranch=${arrBranch[-1]}
                    manifest_file=$(find -maxdepth 1 -name "$slicedBranch-[0-9]*" -printf "%f\n")
                    mv $manifest_file manifest
                    '''

                    archiveArtifacts "manifest"
                    stash name: "manifest", includes: "manifest"
                    env.stash_manifest_name = "manifest"
                    env.stash_manifest_path = "manifest"
                }
            }
        }
        stage("Function Test"){
            load("Jenkinsfile_Library/jobs/function_test/function_test.groovy")
        }

    } catch(error){
        echo "Caught: ${error}"    
        currentBuild.result = "FAILURE"
    } 
}
