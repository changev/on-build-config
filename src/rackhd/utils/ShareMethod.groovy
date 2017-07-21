package rackhd.utils;

def checkout(String url, String branch, String targetDir){
    checkout(
    [$class: 'GitSCM', branches: [[name: branch]],
    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: targetDir]],
    userRemoteConfigs: [[url: url]]])
}

def checkoutBuildReleaseTools(String targetDir){
    checkout(env.LIB_URL, env.LIB_VERSION, targetDir)
}


def getLockedResourceName(String label_name){
    // Get the resource name whose label contains the parameter label_name
    // The locked resources of the build
    def resources=org.jenkins.plugins.lockableresources.LockableResourcesManager.class.get().getResourcesFromBuild(currentBuild.getRawBuild())
    def resources_name=[]
    for(int i=0;i<resources.size();i++){
        String labels = resources[i].getLabels();
        List label_names = Arrays.asList(labels.split("\\s+"));
        for(int j=0;j<label_names.size();j++){
            if(label_names[j]==label_name){
                resources_name.add(resources[i].getName());
            }
        }
    }
    return resources_name
}

def occupyAvailableLockedResource(String label_name, ArrayList<String> used_resources){
     // The locked resources whose label contains the parameter label_name
    resources = getLockedResourceName(label_name)
    def available_resources = resources - used_resources
    if(available_resources.size > 0){
        used_resources.add(available_resources[0])
        String resource_name = available_resources[0]
        return resource_name
    }
    else{
        error("There is no available resources for $label_name")
    }
}



def sendResult(boolean sendJenkinsBuildResults, boolean sendTestResults){
    stage("Send Test Result"){
        try{
            if ("${currentBuild.result}" != "SUCCESS"){
                currentBuild.result = "FAILURE"
            }
            step([$class: 'VTestResultsAnalyzerStep', sendJenkinsBuildResults: sendJenkinsBuildResults, sendTestResults: sendTestResults])
            def message = "Job Name: ${env.JOB_NAME} \n" + "Build Full URL: ${env.BUILD_URL} \n" + "Status: " + currentBuild.result + "\n"
            echo "$message"
            slackSend "$message"
        } catch(error){
            echo "Caught: ${error}"
        }
    }
}

def downloadManifest(String url, String target){
    withCredentials([
            usernamePassword(credentialsId: 'a94afe79-82f5-495a-877c-183567c51e0b',
            passwordVariable: 'BINTRAY_API_KEY',
            usernameVariable: 'BINTRAY_USERNAME')
    ]){
        sh 'curl --user $BINTRAY_USERNAME:$BINTRAY_API_KEY --retry 5 --retry-delay 5 ' + "$url" + ' -o ' + "${target}"
    }
}

