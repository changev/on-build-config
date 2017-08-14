package pipeline.rackhd.debian

def build(String manifest_url, String bintray_subject, String stage_repo_name, String artifactory_url, String is_offical_release, String build_dir, String bintray_component, String library_dir){
  
  def shareMethod = new pipeline.common.ShareMethod()
  def label_name = "debian"
  def bintray_repo="debian"
  def debian_distribution="trusty"
  def debian_architecture="amd64"  
  lock(label: label_name, quantity: 1){
    node_name = shareMethod.occupyAvailableLockedResource(label_name, [])
    node(node_name){
          // credentials are binding to Jenkins Server
          withCredentials([
              usernamePassword(credentialsId: 'MN_ARTIFACTORY_CRED',
                               passwordVariable: 'ARTIFACTORY_PWD',
                               usernameVariable: 'ARTIFACTORY_USR'),

              usernameColonPassword(credentialsId: "ff7ab8d2-e678-41ef-a46b-dd0e780030e1",
                                    variable: "SUDO_CREDS"),
              usernameColonPassword(credentialsId: "a94afe79-82f5-495a-877c-183567c51e0b",
                                    variable:"BINTRAY_CREDS")]){
              deleteDir()
              def manifest = new pipeline.common.Manifest()
              def local_manifest_path = "rackhd-manifest"
              if(library_dir == null){
             
               library_dir = "on-build-config"
              }
               shareMethod.checkoutOnBuildConfig(library_dir)
              dir(library_dir){
                  local_manifest_path = manifest.downloadManifest(manifest_url, local_manifest_path)

                   sh """
                    #!/bin/bash -ex
                    rm -rf $build_dir
                    mkdir -p $build_dir
                    echo $library_dir
                    echo "using Artifactory: " $artifactory_url
                    echo $PWD
                    ./build-release-tools/HWIMO-BUILD build-release-tools/application/make_debian_packages.py \
                    --build-directory $build_dir \
                    --manifest-file  $local_manifest_path \
                    --sudo-credential SUDO_CREDS \
                    --parameter-file downstream_file \
                    --jobs 8 \
                    --force \
                    --is-official-release $is_offical_release \
                    --bintray-credential BINTRAY_CREDS \
                    --bintray-subject $bintray_subject\
                    --bintray-repo $bintray_repo \
                    --artifactory-url $artifactory_url \
                    --artifactory-repo $stage_repo_name \
                    --artifactory-username $ARTIFACTORY_USR \
                    --artifactory-password $ARTIFACTORY_PWD \
                 """
                  if(fileExists ("downstream_file")) {
                      def props = readProperties file: "downstream_file"
                      if(props["RACKHD_VERSION"]) {
                      env.RACKHD_VERSION = "${props.RACKHD_VERSION}"
                      }
                      if(props["RACKHD_COMMIT"]) {
                          env.RACKHD_COMMIT = "${props.RACKHD_COMMIT}"
                      }
                  }

                  // export if build new deb packages
                  def new_deb = "false"
                  def debFiles = "$build_dir/**/*.deb"
                  def debs = findFiles(glob: debFiles)
                  if (debs.length > 0){
                      archiveArtifacts "$debFiles, downstream_file"
                      stash name: 'debians', includes: debFiles
                      new_deb = "true"
                  }
                  def debian_publish = new pipeline.rackhd.debian.Publish()
                  debian_publish.publishToBintray(bintray_subject,new_deb,is_offical_release,build_dir, bintray_repo,bintray_component, library_dir)
 

              }//end work in dir

          } //end withCredentials
    }
  }
}
