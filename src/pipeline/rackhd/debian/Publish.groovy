package pipeline.rackhd.debian

def publishToBintray(String bintray_subject,String new_deb, String is_offical_release, String deb_folder, String bintray_repo, String bintray_component, String library_dir){
    node(build_debian_node){

            deleteDir()
            if (new_deb == "true" ){
                def shareMethod = new pipeline.common.ShareMethod()
                def debian_distribution="trusty"
                def debian_architecture="amd64"
 
                if(library_dir == null){
                  library_dir = "on-build-config"
                }
                shareMethod.checkoutOnBuildConfig(library_dir)
                withCredentials([
                    usernameColonPassword(credentialsId: 'a94afe79-82f5-495a-877c-183567c51e0b',
                                        variable: 'BINTRAY_CREDS')]){
                    dir(library_dir){
                      dir(deb_folder){
                          unstash "debians"
                      }
                      sh """
                        #!/bin/bash
                        set -ex
                        echo "upload debian to bintray"

                        if [ $is_offical_release == true ]; then
                          $bintray_component=release
                        fi
                        echo $bintray_component

                        ./build-release-tools//HWIMO-BUILD ./build-release-tools/application/release_debian_packages.py \
                        --build-directory $deb_folder/ \
                        --bintray-credential BINTRAY_CREDS \
                        --bintray-subject $bintray_subject \
                        --bintray-repo $bintray_repo \
                        --bintray-component $bintray_component \
                        --bintray-distribution $debian_distribution \
                        --bintray-architecture $debian_architecture
                      """
                    }
                }
            } else {
              println "No new debian packages have been built, skip publish."
            }
    }
}

def publishToArtifactory(String debian_folder, String stage_repo_name, String artifactory_url, String debian_component, String library_dir ){
	node(build_debian_node){
        
  withCredentials([
              usernamePassword(credentialsId: 'MN_ARTIFACTORY_CRED',
                               passwordVariable: 'ARTIFACTORY_PWD',
                               usernameVariable: 'ARTIFACTORY_USR')
                ]){

        deleteDir()
        if(library_dir == null){

        library_dir = "on-build-config"
        }     
        def shareMethod = new pipeline.common.ShareMethod()
        shareMethod.checkoutOnBuildConfig(library_dir)

        dir(library_dir){
           dir(debian_folder){
              unstash "debians"
           }
         def debian_distribution="trusty"
         def debian_architecture="amd64"
         sh """
         #!/bin/bash
         set -ex
         ./build-release-tools/HWIMO-BUILD build-release-tools/application/upload_staging_deb_to_artifactory.py \
         --build-directory $debian_folder/ \
         --artifactory-url $artifactory_url \
         --artifactory-repo $stage_repo_name \
         --artifactory-username $ARTIFACTORY_USR \
         --artifactory-password $ARTIFACTORY_PWD \
         --deb-distribution $debian_distribution \
         --deb-component  $debian_component \
         --deb-architecture $debian_architecture
         """
}// end of dir
}//end of creds

}//end of node
}
