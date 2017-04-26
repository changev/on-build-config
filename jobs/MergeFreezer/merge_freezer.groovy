node {
    deleteDir()
    checkout scm
    stage("Launch MergeFreezer"){
        withCredentials([string(credentialsId: 'JENKINSRHD_GITHUB_TOKEN', variable: 'GITHUB_TOKEN'),
                         usernamePassword(credentialsId: 'JENKINS_ADMIN', 
                             passwordVariable: 'JENKINS_PASS', 
                             usernameVariable: 'JENKINS_USER')]) {

            // Run MergeFreezer
            sh '''#!/bin/bash -x
            ./jobs/MergeFreezer/merge_freeze.sh
            '''
        }
    }
}