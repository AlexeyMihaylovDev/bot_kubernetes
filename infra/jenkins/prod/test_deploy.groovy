@Library('global_jenkins_functions') _

JOB = [:]
JOB.email_recepients= "asdasd"

pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timestamps()
        ansiColor('xterm')
    }
    agent {
        node {
            label 'linux'
        }
    }


    stages {
        stage('get last images from ECR') {
            steps {
                script {
                    sh "aws ecr list-images --repository-name alexey_bot_dev | jq \'.imageIds[] | .imageTag\' > images.txt"
                }

            }
        }

    }
        stage('Input Images to deploy') {
            steps {
                script {
                    def userInput = input id: 'UserInput', message: 'Please provide parameters.', ok: 'OK', parameters: [
                            [$class: 'CascadeChoiceParameter', choiceType: 'PT_CHECKBOX', filterLength: 1, filterable: false,
                             name: 'Images', referencedParameters: '',
                             script: [$class: 'GroovyScript', fallbackScript: [classpath: [], oldScript: '', sandbox: true, script: 'return [\'error\']'],
                                      script: [classpath: [], oldScript: '', sandbox: true,
                                               script: '''
                                 ef list = []


def list = readFile("${env.WORKSPASE}/images.txt").readLines()
return list.toList()
                                 '''.toString()
                                      ]]]]
]
                    }

                }
            }
        }
//
//        stage("build") {
//            steps {
//
//                }
//            }


        post {
            always {
                script {
                    currentBuild.description = ("Branch : ${JOB.branch}\n GitCommiter : ${JOB.commitAuthor}\nDeploy_server: ${JOB.deploy}")
                    EMAIL_MAP = [
                            "Job Name"    : JOB_NAME,
                            "ENV"         : params.ENV,
                            "Build Number": BUILD_NUMBER,
                            "More Info At": "<a href=${BUILD_URL}console> Click here to view build console on Jenkins. </a>",
                            "painted"     : "false"
                    ]
                    global_sendGlobalMail.sendByMapFormat(JOB.email_recepients, currentBuild.result, EMAIL_MAP,
                            "Jenkins Report", "Build Notification - Jenkins Report", "BOT build")

                }
            }
        }

}