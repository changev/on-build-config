node{
    deleteDir()
    withEnv([
        "ghprbPullLink = ${env.ghprbPullLink}",
        "ghprbTargetBranch = ${env.ghprbTargetBranch}"
    ]) {
        def shareMethod
        dir("on-build-config") {
            checkout scm
        }
        env.stash_manifest_path = "manifest"
        withCredentials([string(credentialsId: 'JENKINSRHD_GITHUB_TOKEN',
                                variable: 'GITHUB_TOKEN')]) {
            sh 'on-build-config/jobs/pr_gate/pr_parser.sh'
        }
        archiveArtifacts 'manifest'

        stash name: 'manifest', includes: 'manifest'
        env.stash_manifest_name = "manifest"
    }
}
