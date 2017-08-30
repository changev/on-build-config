package pipeline.rackhd.ova

def keepEnv(String library_dir, boolean keep_env, int keep_minutes, String test_target, String test_name){
    if(keep_env){
        def message = "Job Name: ${env.JOB_NAME} \n" + "Build Full URL: ${env.BUILD_URL} \n" + "Status: FAILURE \n" + "Stage: $test_target/$test_name \n" + "Node Name: $NODE_NAME \n" + "Reserve Duration: $keep_minutes minutes \n"
        echo "$message"
        slackSend "$message"
        sleep time: keep_minutes, unit: 'MINUTES'
    }
}

def configFitOvaStack(library_dir, rackhd_dir){
    def stack_config_json = readJSON file: """$rackhd_dir/test/config/stack_config.json"""
    def replacement = readJSON file: """$library_dir/resources/pipeline/rackhd/ova/stack_config.json"""
    stack_config_json['ova'] = replacement['ova']
    writeJSON file: """$rackhd_dir/test/config/stack_config.json""", json: stack_config_json
}

/**
@para
stack_type: RackHD test stack type, a virtual one or baremetal one.
test_name: Run FIT or OS Installation test.
used_resources: A var stored resource usage of current pipelin.
*_creds: Id of jenkins credentials.
    ova_creds: ova user password credential Id.
    esxi_creds: esxi user password credential Id.
    ova_internal_ip_creds: ova_internal_ip string cred Id, the Ip to connect ova.
ova_gateway: Ususally the vmslave eth1 IP, the net interface of vmslave that connected to ova.
ova_stash_dict, ova_url: A stash dict contains stash name and path, or a ova url.
                         At least one not null. When both exist ova_url has high priority.
ova_net_interface: [only Valid when external_vswitch=null] Ova use this net interface to connect gateway.
dns_server_ip: [only Valid when external_vswitch=null] Dns server for ova.

@hidden para(stored in node env)
esxi_host, datastore: The location to deploy ova.
external_vswitch: If not null, ova eth0 will be connceted it.
                  Please use it only when this vswith connects to a dhcp external network.
                  If this is null, deployment script will configure ova to connect to external network throw gateway.

@More about the network config of ova(paras external_vswitch, ova_net_interface, dns_server_ip)
The deployed ova need to visit external network. There're three ways to achieve this:
1. Connect to external network with dhcp, 2. Connect to external network with static ip
3. Connect to external network through gateway provided by a machine which already has external network visit access.
But config static ip is inconvenient in our CI/CD env, so we choose 1 and 3.
external_vswitch is mutually exclusive with ova_net_interface and dns_server_ip.
If the ova is deployed to an esxi with dhcp network, the external_vswitch can be set un-null for ova eth0 to connect.
If not, the external_vswitch must be set null and then deployment script will use ova_net_interface and dns_server_ip
to config ova to connect to external network through gateway.
*/
def runTest(String stack_type,
            String test_name,
            ArrayList<String> used_resources,
            String sudo_creds,
            String esxi_creds,
            String ova_creds,
            String ova_gateway,
            String ova_internal_ip_creds,
            Map ova_stash_dict=null,
            String ova_url="",
            String ova_net_interface=null,
            String dns_server_ip = null,
            boolean keep_env_on_failure=false,
            int keep_minutes=60){
    def shareMethod = new pipeline.common.ShareMethod()
    String test_target = "ova"
    def fit_configure = new pipeline.fit.FitConfigure(stack_type, test_target, test_name)
    def fit_init_configure = new pipeline.fit.FitConfigure(stack_type, test_target, "INIT")
    fit_configure.configure()
    fit_init_configure.configure()
    String node_name = ""
    String label_name = fit_configure.getLabel()
    try{
        lock(label:label_name,quantity:1){
            node_name = shareMethod.occupyAvailableLockedResource(label_name, used_resources)
            node(node_name){
                withEnv([
                  "esxi_host=${env.ESXI_HOST}",
                  "datastore=${env.DATASTORE}",
                  "external_vswitch=${env.External_vSwitch}"
                  ]){
                    deleteDir()
                    def fit = new pipeline.fit.FIT()
                    def virtual_node = new pipeline.nodes.VirtualNode()
                    def rackhd_deployer = new pipeline.rackhd.ova.Deploy()
                    String library_dir = "$WORKSPACE/on-build-config"
                    shareMethod.checkoutOnBuildConfig(library_dir)
                    String ova_path = ""
                    if (ova_url != ""){
                        ova_path = ova_url
                    } else {
                        ova_path = shareMethod.unstashFile(ova_stash_dict, "$WORKSPACE")
                    }
                    String rackhd_dir = "$WORKSPACE/RACKHD"
                    shareMethod.checkout("https://github.com/RackHD/RackHD.git", "master", rackhd_dir)

                    boolean ignore_failure = false
                    String target_dir = test_target + "/" + test_name + "_$NODE_NAME"

                    try{
                        // clean up rackhd and virtual nodes
                        rackhd_deployer.cleanUp(library_dir, sudo_creds, node_name,
                                                esxi_host, esxi_creds, ignore_failure)
                        virtual_node.cleanUp(library_dir, ignore_failure)
                        // deploy rackhd and virtual nodes
                        rackhd_deployer.deploy( library_dir, ova_path, sudo_creds, node_name, esxi_host,
                                                esxi_creds, datastore, ova_creds, ova_gateway,
                                                ova_internal_ip_creds, external_vswitch,
                                                ova_net_interface, dns_server_ip)
                        virtual_node.deploy(library_dir)
                        virtual_node.remoteStartFetchLogs(target_dir, ova_creds, ova_internal_ip_creds, library_dir)

                        // run FIT test\
                        configFitOvaStack(library_dir, rackhd_dir)
                        fit.run(rackhd_dir, fit_init_configure)
                        fit.run(rackhd_dir, fit_configure)
                    } catch(error){
                        keepEnv(library_dir, keep_env_on_failure, keep_minutes, test_target, test_name)
                        error("[ERROR] Failed to run test $test_name against $test_target with error: $error")
                    } finally{
                        // archive rackhd logs
                        rackhd_deployer.archiveLogsToTarget(library_dir, target_dir, sudo_creds,
                                                            ova_creds, ova_internal_ip_creds)
                        fit.archiveLogsToTarget(target_dir, fit_configure)
                        virtual_node.remoteStopFetchLogs(target_dir, ova_creds, ova_internal_ip_creds, library_dir)
                        virtual_node.archiveLogsToTarget(target_dir)
                        // clean up rackhd and virtual nodes
                        ignore_failure = true
                        rackhd_deployer.cleanUp(library_dir, sudo_creds, node_name,
                                                esxi_host, esxi_creds, ignore_failure)
                        virtual_node.cleanUp(library_dir, ignore_failure)
                        // archive logs of virtual nodes and FIT
                    }
                }
            }
        }
    } finally{
        used_resources.remove(node_name)
    }
}

return this
