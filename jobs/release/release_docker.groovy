import groovy.transform.Field;
@Field def shareMethod
node{
    deleteDir()
    checkout scm
    shareMethod = load("jobs/ShareMethod.groovy")
}

String label_name = build_docker_label
lock(label:label_name,quantity:1){
    resources_name = shareMethod.getLockedResourceName(label_name)
    if(resources_name.size>0){
        node_name = resources_name[0]
    }
    node(node_name){
        dir("on-build-config"){
            checkout scm
        }
        dir("DOCKER"){
            unstash env.DOCKER_STASH_NAME
        }
        withCredentials([
            usernamePassword(credentialsId: 'da1e60c6-f23a-429d-b0f5-19e3b287f5dc', 
                            passwordVariable: 'DOCKERHUB_PASS', 
                            usernameVariable: 'DOCKERHUB_USER')]) {
            timeout(120){
                sh './on-build-config/jobs/release/release_docker.sh'
            }
        }
    }
}