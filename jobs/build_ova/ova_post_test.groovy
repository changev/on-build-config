node(){
    timestamps{
        withEnv([
            "IS_OFFICIAL_RELEASE=${env.IS_OFFICIAL_RELEASE}", 
            "RACKHD_VERSION=${env.RACKHD_VERSION}",
            "OS_VER=${env.OS_VER}",
            "OVA_GATEWAY=${env.OVA_GATEWAY}",
            "OVA_NET_INTERFACE=${env.OVA_NET_INTERFACE}"]){
            deleteDir()
            checkout scm
            def function_test = load("jobs/FunctionTest/FunctionTest.groovy")
            def repo_dir = pwd()
            withCredentials([
                usernamePassword(credentialsId: 'OVA_CREDS', 
                                    passwordVariable: 'OVA_PASSWORD', 
                                    usernameVariable: 'OVA_USER'),
                string(credentialsId: 'vCenter_IP', variable: 'VCENTER_IP'), 
                string(credentialsId: 'Deployed_OVA_INTERNAL_IP', variable: 'OVA_INTERNAL_IP')
                ]) {
                timeout(90){
                    // Start to run test
                    def TESTS = "${env.OVA_POST_TESTS}"
                    function_test.run(TESTS=TESTS, repo_dir=repo_dir, "ova", ova_name=env.OVA_STASH_NAME, ova_path=env.OVA_PATH)
                }
            }
        }
    }
}
