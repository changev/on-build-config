def execute(){
    node(build_docker_node){
        def shareMethod = new rackhd.utils.ShareMethod()
        shareMethod.checkoutBuildReleaseTools("build-config")
        dir("DOCKER"){
            unstash env.DOCKER_STASH_NAME
        }
        withCredentials([
            usernamePassword(credentialsId: 'rackhd-ci-docker-hub', 
                            passwordVariable: 'DOCKERHUB_PASS', 
                            usernameVariable: 'DOCKERHUB_USER')]) {
            timeout(120){
                sh './build-config/src/rackhd/release/release_docker.sh'
            }
        }
    }
}


