@Library("ova-function-test") _
node{
        def functionTest = new pipeline.rackhd.ova.FunctionTest()
                deleteDir()
                        def target_dir = "ova/function_test/"
                                def ova_dict = [:]
                                        ova_dict['ip']='172.31.128.1'
                                                ova_dict['username']='vagrant'
                                                        ova_dict['password']='vagrant'
                                                                ova_dict['ova_url']='http://10.62.59.175:8080/job/alan/job/ova-source/lastSuccessfulBuild/artifact/rackhd-ubuntu-14.04-2.18.0-20170822UTC.ova'
                                                                        def ova_resource = []
                                                                                println "start to run test"
                                                                                        functionTest.runTest("virtual_stack","FIT",ova_resource, ova_dict, false, 30, "true")


}
