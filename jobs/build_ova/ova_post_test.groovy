node(){
    timestamps{
        withEnv([
            "IS_OFFICIAL_RELEASE=${env.IS_OFFICIAL_RELEASE}", 
            "RACKHD_VERSION=${env.RACKHD_VERSION}",
            "OS_VER=${env.OS_VER}",
            "OVA_POST_TEST=true",
            "TESTS=${env.OVA_POST_TESTS}"]){
            deleteDir()
            dir("build-config"){
                checkout scm
            }

            withCredentials([
                usernamePassword(credentialsId: 'VCENTER_NT_CREDS', 
                                    passwordVariable: 'VCENTER_NT_PASSWORD', 
                                    usernameVariable: 'VCENTER_NT_USER'),
                usernamePassword(credentialsId: 'OVA_CREDS', 
                                    passwordVariable: 'OVA_PASSWORD', 
                                    usernameVariable: 'OVA_USER'),
                string(credentialsId: 'vCenter_IP', variable: 'VCENTER_IP'), 
                string(credentialsId: 'Deployed_OVA_INTERNAL_IP', variable: 'OVA_INTERNAL_IP')
                ]) {
                timeout(90){
                    load('build-config/jobs/function_test/function_test.groovy')
                }
            }
        }
    }
}
