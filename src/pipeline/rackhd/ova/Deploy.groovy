package pipeline.rackhd.ova

def deploy( String library_dir,
            String ova_path,
            String sudo_creds,
            String node_name,
            String esxi_host,
            String esxi_creds,
            String datastore,
            String ova_creds,
            String ova_gateway,
            String ova_internal_ip_creds,
            String external_vswitch=null,
            String ova_net_interface=null,
            String dns_server_ip=null){
    withCredentials([
        usernamePassword(credentialsId: ova_creds,
                        passwordVariable: 'OVA_PASSWORD',
                        usernameVariable: 'OVA_USER'),
        usernamePassword(credentialsId: esxi_creds,
                        passwordVariable: 'ESXI_PASS',
                        usernameVariable: 'ESXI_USER'),
        usernamePassword(credentialsId: sudo_creds,
                         passwordVariable: 'SUDO_PASSWORD',
                         usernameVariable: 'SUDO_USER'),
        string(credentialsId: ova_internal_ip_creds, variable: 'OVA_INTERNAL_IP')
    ]){
      sh """#!/bin/bash -ex
      pushd $library_dir/src/pipeline/rackhd/ova
      # Deploy rackhd ova
      if [ $external_vswitch == "null" ] || [ $external_vswitch == "" ]; then
          echo "Deploy ova with gateway network"
          EXTERNAL_VSWITCH="" \
          ./deploy.sh deploy -w $WORKSPACE -p $SUDO_PASSWORD -b $library_dir \
                             -i $ova_path -n $node_name \
                             -d $datastore -eu $ESXI_USER -ep $ESXI_PASS -h $esxi_host \
                             -o $OVA_INTERNAL_IP -g $ova_gateway -ni $ova_net_interface \
                             -ou $OVA_USER -op $OVA_PASSWORD -s $dns_server_ip
      else
        echo "Deploy ova with dhcp network"
        ./deploy.sh deploy -w $WORKSPACE -p $SUDO_PASSWORD -b $library_dir \
                           -i $ova_path  -n $node_name -d $datastore \
                           -eu $ESXI_USER -ep $ESXI_PASS -h $esxi_host \
                           -o $OVA_INTERNAL_IP -g $ova_gateway \
                           -ou $OVA_USER -op $OVA_PASSWORD
      fi
      popd
      """
    }
}

def cleanUp(String library_dir,
            String sudo_creds,
            String node_name,
            String esxi_host,
            String esxi_creds,
            boolean ignore_failure=false){
    try{
        withCredentials([
            usernamePassword(credentialsId: esxi_creds,
                            passwordVariable: 'ESXI_PASS',
                            usernameVariable: 'ESXI_USER'),
            usernamePassword(credentialsId: sudo_creds,
                             passwordVariable: 'SUDO_PASSWORD',
                             usernameVariable: 'SUDO_USER')])
        {
            sh """#!/bin/bash -e
            pushd $library_dir/src/pipeline/rackhd/ova
            # Clean up exsiting ova
            ./deploy.sh cleanUp -p $SUDO_PASSWORD -b $library_dir -n $node_name \
                                -eu $ESXI_USER -ep $ESXI_PASS -h $esxi_host
            popd
            """
        }
    }catch(error){
        if(ignore_failure){
            echo "[WARNING]: Failed to clean up rackhd with error: ${error}"
        } else{
            error("[ERROR]: Failed to clean up rackhd with error: ${error}")
        }
    }
}

def archiveLogsToTarget(String library_dir,
                        String target_dir,
                        String sudo_creds,
                        String ova_creds,
                        String ova_internal_ip_creds){
    try{
      withCredentials([
          usernamePassword(credentialsId: sudo_creds,
                           passwordVariable: 'SUDO_PASSWORD',
                           usernameVariable: 'SUDO_USER'),
          usernamePassword(credentialsId: ova_creds,
                           passwordVariable: 'OVA_PASSWORD',
                           usernameVariable: 'OVA_USER'),
          string(credentialsId: ova_internal_ip_creds, variable: 'OVA_INTERNAL_IP')])
      {
          dir(target_dir){
            sh """#!/bin/bash -e
            current_dir=`pwd`
            pushd $library_dir/src/pipeline/rackhd/ova
            # export log of rackhd
            ./deploy.sh exportLog -w $WORKSPACE -l \$current_dir -b $library_dir -p $SUDO_PASSWORD \
                                  -o $OVA_INTERNAL_IP -ou $OVA_USER -op $OVA_PASSWORD


            popd
            """
          }
          archiveArtifacts "$target_dir/*.log"
      }
    } catch(error){
        echo "[WARNING]Caught error during archive artifact of rackhd to $target_dir: ${error}"
    }
}
