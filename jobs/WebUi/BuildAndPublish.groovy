def buildPackage(String repo_dir){
    // retry times for package build to avoid failing caused by network
    int retry_times = 3
    stage("Packages Build"){
        retry(retry_times){
            load(repo_dir + "/jobs/build_debian/build_debian.groovy")
        }
    }
}

def buildDocker(String repo_dir){
    def bd = load(repo_dir + "/jobs/build_docker/build_docker.groovy")
    bd.setRunScript("./build-config/jobs/WebUi/BuildDocker/build_docker.sh")
}

def buildImages(String repo_dir){
    // retry times for images build to avoid failing caused by network
    int retry_times = 3
    stage("Build Docker"){
        retry(retry_times){
            buildDocker(repo_dir)
        }
    }
}

def publishImages(String repo_dir){
    stage("Publish"){
        parallel 'Publish Debian':{
            load(repo_dir + "/jobs/release/release_debian.groovy")
        }, 'Publish Docker':{
            load(repo_dir + "/jobs/release/release_docker.groovy")
        }
    }
}

def createTag(String repo_dir){
    stage("Create Tag"){
        load(repo_dir + "/jobs/SprintRelease/create_tag.groovy")
    }
}

def buildAndPublish(Boolean publish, Boolean tag, String repo_dir){
    buildPackage(repo_dir)
    buildImages(repo_dir)
    if(tag){
        createTag(repo_dir)
    }
    if(publish){
        publishImages(repo_dir)
    }
}

return this
