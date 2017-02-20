@NonCPS
def printParams() {
  env.getEnvironment().each { name, value -> println "Name: $name -> Value $value" }
}

node{
    withEnv([
        "stash_manifest_name=${env.stash_manifest_name}",
        "stash_manifest_path=${env.stash_manifest_path}",
        "status=${currentBuild.result}"
        ]){
        deleteDir()
        dir("build-config"){
            git branch: 'feature/test-pr_gate_pipeline', url: 'https://github.com/PengTian0/on-build-config'
        }

        unstash "${stash_manifest_name}"

        withCredentials([string(credentialsId: 'JENKINSRHD_GITHUB_TOKEN', 
                                variable: 'GITHUB_TOKEN')]) {
            printParams()
            sh './build-config/jobs/write_back_github/write_back_github.sh'
        }
    }
}
