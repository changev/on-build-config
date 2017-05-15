node(){
    timestamps{
        withEnv([
            "IS_OFFICIAL_RELEASE=${env.IS_OFFICIAL_RELEASE}", 
            "RACKHD_VERSION=${env.RACKHD_VERSION}",
            "OS_VER=${env.OS_VER}"]){
            deleteDir()
            checkout scm
            def function_test = load("jobs/FunctionTest/FunctionTest.groovy")
            def repo_dir = pwd()
            def TESTS = "${env.VAGRANT_POST_TESTS}"
            def test_type = "vagrant"
            try{
                withCredentials([
                    usernamePassword(credentialsId: 'VAGRANT_CREDS', 
                                        passwordVariable: 'VAGRANT_PASSWORD', 
                                        usernameVariable: 'VAGRANT_USER')
                    ]) {
                    // Start to run test
                    def VAGRANT_STASH_NAME = "${env.VAGRANT_STASH_NAME}"
                    def VAGRANT_STASH_PATH = "${env.VAGRANT_STASH_PATH}"
                    function_test.vagrantPostTest(TESTS, VAGRANT_STASH_NAME, VAGRANT_STASH_PATH, repo_dir, test_type)
                }
            }finally{
                function_test.archiveArtifactsToTarget("VAGRANT_POST_TEST", TESTS, test_type)
            }
        }
    }
}
