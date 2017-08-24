package pipeline.fit

def run(String rackhd_dir, Object fit_configure, Map rackhd_host_cred=null){
    String group = fit_configure.getGroup()
    String stack = fit_configure.getStack()
    String log_level = fit_configure.getLogLevel()
    String extra_options = fit_configure.getExtraOptions()

    if (rackhd_host_cred != null){
        String rackhd_user = rackhd_host_cred["user"]
        String rackhd_pass = rackhd_host_cred["password"]
        sh """#!/bin/bash -ex
        pushd $rackhd_dir/config
        sed -i "s/\"username\": \"vagrant\"/\"username\": \"$rackhd_user\"/g" credentials_default.json
        sed -i "s/\"password\": \"vagrant\"/\"password\": \"$rackhd_pass\"/g" credentials_default.json
        popd
        """
    }

    try{
        sh """#!/bin/bash -ex
        pushd $rackhd_dir/test
        ./runFIT.sh -g "-test deploy/rackhd_stack_init.py" -s "$stack" -v $log_level -e "$extra_options"
        ./runFIT.sh -g "$group" -s "$stack" -v $log_level -e "$extra_options" -w $WORKSPACE
        popd
        """
    } finally{
        dir("$WORKSPACE"){
            junit 'xunit-reports/*.xml'
        }
    }
}

def archiveLogsToTarget(String target_dir, Object fit_configure){
    String name = fit_configure.getName()
    try{
        dir(target_dir){
            sh """#!/bin/bash
            set +e
            mv $WORKSPACE/xunit-reports/*.xml .
            """
        }
        archiveArtifacts "$target_dir/*.xml"
    } catch(error){
        echo "[WARNING]Caught error during archive artifact for $name: $error"
    }
}
