pipeline {
    agent {
        label 'image-java21'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        pollSCM('H */1 * * *')
    }

    stages {
        stage('Check Skip Build') {
            steps {
                script {
                    try {
                        def commitMsg = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()
                        echo "Commit message: ${commitMsg}"

                        if (commitMsg.toLowerCase().contains('ci skip') ||
                            commitMsg.toLowerCase().contains('[ci skip]') ||
                            commitMsg.toLowerCase().contains('[skip ci]')) {
                            echo "Build skipped due to [ci skip] in commit message"
                            currentBuild.rawBuild.executor.interrupt(Result.NOT_BUILT)
                            currentBuild.result = 'NOT_BUILT'
                            error('Build skipped due to [ci skip] in commit message')
                        }
                    } catch (Exception e) {
                        echo "Warning: Could not check commit message: ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Prepare') {
            steps {
                sh 'chmod +x ./gradlew'
            }
        }

        stage('Build') {
            steps {
                sh './gradlew publishAllPublicationsToVulpesCloudSnapshotsRepository'
                sh './gradlew buildAll'
            }
        }

        stage('Publish Artifacts') {
            steps {
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true, followSymlinks: false
            }
        }
    }

    post {
        always {
            script {
                if (currentBuild.result != 'NOT_BUILT') {
                    env.GIT_CHANGES = "- No changes detected"
                    def githubRepoUrl = "https://github.com/VulpesCloud/VulpesCloud"

                    try {
                        def changeLogSets = currentBuild.changeSets
                        def changes = []

                        changeLogSets.each { changeSet ->
                            changeSet.items.each { item ->
                                def shortCommit = item.commitId.take(7)
                                def commitUrl = "${githubRepoUrl}/commit/${item.commitId}"
                                def authorName = item.author.toString()

                                if (authorName.contains("+")) {
                                    authorName = authorName.substring(authorName.indexOf("+") + 1)
                                }

                                changes.add("- [`${shortCommit}`](${commitUrl}) ${item.msg} - ${authorName}")
                            }
                        }

                        if (changes.size() > 0) {
                            env.GIT_CHANGES = changes.join("\n")
                        } else {
                            if (currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').size() > 0) {
                                def user = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')[0].userName
                                env.GIT_CHANGES = "- Manual build triggered by ${user}"
                            } else {
                                def gitCommand = 'git log -1 --pretty=format:"%h %s - %an"'
                                def gitCommit = sh(script: gitCommand, returnStdout: true).trim()

                                if (gitCommit) {
                                    def commitParts = gitCommit.split(' - ')
                                    def commitInfo = commitParts[0]
                                    def authorRaw = commitParts.size() > 1 ? commitParts[1] : "Unknown"
                                    def authorName = authorRaw

                                    if (authorName.contains("+")) {
                                        authorName = authorName.substring(authorName.indexOf("+") + 1)
                                    }

                                    changes.add("- `${commitInfo}` - ${authorName}")
                                    def maxChanges = 6
                                    def displayedChanges = changes.take(maxChanges)
                                    if (changes.size() > maxChanges) {
                                        displayedChanges << "- ..."
                                    }
                                    env.GIT_CHANGES = displayedChanges.join("\n")
                                } else {
                                    env.GIT_CHANGES = "- Automated build - no detected changes"
                                }
                            }
                        }
                    } catch (Exception e) {
                        echo "Warning: Could not retrieve changes: ${e.getMessage()}"
                        env.GIT_CHANGES = "- Could not retrieve changes"
                    }

                    cleanWs()
                }
            }
        }

        success {
            script {
                if (currentBuild.result != 'NOT_BUILT') {
                    withCredentials([string(credentialsId: 'webhook-private', variable: 'DISCORD_WEBHOOK')]) {
                        def decodedJobName = java.net.URLDecoder.decode(env.JOB_NAME, 'UTF-8')
                        def cleanJobPath = env.JOB_NAME.replaceAll('%2[Ff]', '/')
                        def artifactsUrl = "https://jenkins.vulpescloud.de/job/${cleanJobPath}/artifact/build/libs/"
                        def branchName = env.BRANCH_NAME ?: env.GIT_BRANCH?.tokenize('/')?.last() ?: "unknown"

                        discordSend description: "**Build:** #${env.BUILD_NUMBER}\n**Status:** success\n**Changes:**\n${env.GIT_CHANGES}\n\n**Artifacts:** [Download Here](${artifactsUrl})",
                            link: env.BUILD_URL,
                            result: currentBuild.currentResult,
                            title: "${decodedJobName} Build #${env.BUILD_NUMBER}",
                            webhookURL: "${DISCORD_WEBHOOK}"
                    }
                }
            }
        }

        failure {
            script {
                if (currentBuild.result != 'NOT_BUILT') {
                    withCredentials([string(credentialsId: 'webhook-private', variable: 'DISCORD_WEBHOOK')]) {
                        def decodedJobName = java.net.URLDecoder.decode(env.JOB_NAME, 'UTF-8')

                        discordSend description: "**Build:** #${env.BUILD_NUMBER}\n**Status:** failed\n**Changes:**\n${env.GIT_CHANGES}\n\n**Build Details:** [View Log](${env.BUILD_URL}console)",
                            link: env.BUILD_URL,
                            result: currentBuild.currentResult,
                            title: "${decodedJobName} Build #${env.BUILD_NUMBER}",
                            webhookURL: "${DISCORD_WEBHOOK}"
                    }
                }
            }
        }
    }
}
