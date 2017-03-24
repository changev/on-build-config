node{
    deleteDir()
    checkout scm
    stage("build ova"){
        load("jobs/build_ova/build_ova.groovy")
    }

    stage("post Test"){
        load("jobs/build_ova/ova_post_test.groovy")
    }
}
