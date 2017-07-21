def execute(){
    node(build_debian_node){
        withEnv([
            "BINTRAY_SUBJECT=${env.BINTRAY_SUBJECT}",
            "BINTRAY_REPO=debian",
            "BINTRAY_COMPONENT=main", 
            "BINTRAY_DISTRIBUTION=trusty", 
            "BINTRAY_ARCHITECTURE=amd64"]){
            deleteDir()
            if (env.NEW_DEB == true ){
                def shareMethod = new rackhd.utils.ShareMethod()
                shareMethod.checkoutBuildReleaseTools("build-config")
                dir("DEBIAN"){
                    unstash "debians"
                }
                withCredentials([
                    usernameColonPassword(credentialsId: 'a94afe79-82f5-495a-877c-183567c51e0b', 
                                        variable: 'BINTRAY_CREDS')]){
                    sh './build-config/src/rackhd/release/release_debian.sh'
                }
            }
        }
    }
}

