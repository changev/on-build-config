node{
    deleteDir()
    withEnv([
        "ghprbPullLink = ${env.ghprbPullLink}",
        "ghprbTargetBranch = ${env.ghprbTargetBranch}"
    ]) {
        def shareMethod
        dir("PR_Parser_Files") {
            checkout scm
            shareMethod = load("jobs/shareMethod.groovy")
        }
        def url = "https://github.com/PengTian0/on-build-config.git"
        def branch = "feature/test-pr_gate_pipeline"
        def targetDir = "on-build-config"
        shareMethod.checkout(url, branch, targetDir)

        env.stash_manifest_path = "manifest"
        withCredentials([string(credentialsId: 'JENKINSRHD_GITHUB_TOKEN',
                                variable: 'GITHUB_TOKEN')]) {
            sh 'PR_Parser_Files/jobs/pr_gate/pr_parser.sh'
        }

        archiveArtifacts 'manifest'

        //sh 'curl https://dl.bintray.com/pengtian0/binary/master-test -o pr-manifest'
 
       // archiveArtifacts 'pr-manifest'

        stash name: 'manifest', includes: 'manifest'
        env.stash_manifest_name = "manifest"
    }
}
