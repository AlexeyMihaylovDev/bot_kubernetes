@Library('global_jenkins_functions') _


import groovy.time.TimeDuration
import groovy.transform.Field
/*************************************************************** | PARAMETERS | ******************************************************************/

// GLOABAL PARAMETERS

@Field JOB = [:]

JOB.docker_file_path = "infra/jenkins/jenAgent/JenkinsAgent.Dockerfile"
JOB.git_project_url = "https://github.com/AlexeyMihaylovDev/bot_kubernetes.git"
JOB.project_name = "JenkinsAgent"
JOB.devops_sys_user = "my_polybot_key"
JOB.branch = "feature/deploy"
JOB.email_recepients = "mamtata2022@gmail.com"
JOB.user_run = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')

pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timestamps()
        ansiColor('xterm')
    }
    agent { label 'linux' }
    environment {
        REGISTRY_URL = "352708296901.dkr.ecr.eu-central-1.amazonaws.com"
        REGISTRY_REGION = "eu-central-1"
        BOT_ECR_NAME = "alexey_jenk_agent"
        IMAGE_ID = "${env.REGISTRY_URL}/alexey_jenk_agent"
    }

    stages {
        stage('Get & Print Job Parameters') {
            steps {
                script {
                    println("======================== print all jobs properties ===================")
                    JOB.each { key, value ->
                        println(' ' + key + '==' + ' ' + value)
                    }

                }
            }
        }
        stage('Clone') {
            steps {
                script {
                    // Clone PolyBot repository.
                    git branch: "${JOB.branch}", url: "${JOB.git_project_url}"
                    JOB.gitCommitHash = global_gitInfo.getCommitHash(JOB.branch)
                    println("====================${JOB.gitCommitHash}==============")
                }
            }
        }
        stage("build") {
            steps {
                sh "aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin $REGISTRY_URL"
                script {
                            def imageName = "$BOT_ECR_NAME:${env.BUILD_NUMBER}"
                            def  finalImageName = "$REGISTRY_URL/$BOT_ECR_NAME:${env.BUILD_NUMBER}"
                            sh "docker build -t $imageName -f  ${JOB['docker_file_path']} ."
                            sh "docker tag $imageName $finalImageName"
                            sh "docker push $finalImageName"
                        }
                    }

                }
            }
    post {
        always {
            script {
                currentBuild.description = ("Branch : ${JOB.branch}\n GitCommiter : ${JOB.commitAuthor}\nDeploy_server: ${JOB.deploy}")

                EMAIL_MAP = [

                        "Job Name"      : JOB_NAME,
                        "Build Number"  : BUILD_NUMBER,
                        "Git tag Name"  : JOB.tagName,
                        "Branch"        : "${JOB.branch}",
                        "More Info At"  : "<a href=${BUILD_URL}console> Click here to view build console on Jenkins. </a>",
                        "painted"       : "false"
                ]
                global_sendGlobalMail.sendByMapFormat(JOB.email_recepients, currentBuild.result, EMAIL_MAP,
                        "Jenkins Report", "Build Notification - Jenkins Report", "BOT build")

            }
        }
    }
}
