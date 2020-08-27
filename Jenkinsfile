node('java') {
    cleanWs()
    git url: 'https://github.com/jfrog/ide-plugins-common.git'
    def jdktool = tool name: "jdk-8u111-linux-x64-jce-unlimited-policy"
    env.JAVA_HOME = jdktool
    echo jdktool
    def server = Artifactory.server('oss.jfrog.org')
    def buildInfo
    def release = RELEASE && VERSION != "" && NEXT_DEVELOPMENT_VERSION != ""

    if (release) {
        stage('Set release version') {
            changeCurrentVersion(VERSION)
        }
    }

    stage('Build IDE plugins common') {
        buildInfo = buildIdePluginsCommon(server, release)
    }

    stage('Publish build info') {
        server.publishBuildInfo(buildInfo)
    }

    if (!release) {
        return
    }

    stage('Commit and create version tag') {
        sh("git commit -am '[artifactory-release] Release version ${VERSION}'")
        sh("git tag '${VERSION}'")
    }

    stage('Set next development version') {
        changeCurrentVersion(NEXT_DEVELOPMENT_VERSION)
        sh("git commit -am '[artifactory-release] Next development version'")
    }

    stage('Push changes') {
        sh '''#!/bin/bash
           set -o pipefail
           git push https://${GITHUB_USERNAME}:${GITHUB_PASSWORD}@github.com/JFrog/ide-plugins-common.git 2>&1 | grep -v "http"
           git push https://${GITHUB_USERNAME}:${GITHUB_PASSWORD}@github.com/JFrog/ide-plugins-common.git --tags 2>&1 | grep -v "http"
        '''
    }

    stage('Distribute') {
        def distributionConfig = [
                // Mandatory parameters
                'buildName'             : buildInfo.name,
                'buildNumber'           : buildInfo.number,
                'targetRepo'            : 'jfrog-packages',

                // Optional parameters
                'publish'               : false, // Default: true. If true, artifacts are published when deployed to Bintray.
                'overrideExistingFiles' : true, // Default: false. If true, Artifactory overwrites builds already existing in the target path in Bintray.
                'async'                 : false, // Default: false. If true, the build will be distributed asynchronously. Errors and warnings may be viewed in the Artifactory log.
                'dryRun'                : false, // Default: false. If true, distribution is only simulated. No files are actually moved.
        ]
        server.distribute distributionConfig
    }
}

def buildIdePluginsCommon(server, release) {
    def deployRepo = release ? 'oss-release-local' : 'oss-snapshot-local'
    def gradleBuild = Artifactory.newGradleBuild()
    gradleBuild.useWrapper = true
    gradleBuild.deployer.deployMavenDescriptors = true
    gradleBuild.deployer.deployIvyDescriptors = false
    gradleBuild.deployer repo: deployRepo, server: server
    gradleBuild.resolver repo: 'remote-repos', server: server

    def buildInfo = Artifactory.newBuildInfo()
    buildInfo.name = 'EcoSystem :: ide-plugins-common'
    gradleBuild.run rootDir: ".", buildFile: 'build.gradle',tasks: 'clean install artifactoryPublish -x test -x javadoc', buildInfo: buildInfo
    return buildInfo
}

def changeCurrentVersion(version) {
    def props = readProperties file: 'gradle.properties'
    props['version'] = version
    def content = ""
    for(s in props) {
        content += s.toString() + "\n"
    }
    writeFile file: 'gradle.properties', text: content
}