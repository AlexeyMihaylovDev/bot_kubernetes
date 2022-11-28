@Library('global_jenkins_functions') _

JOB = [:]
JOB.email_recepients = "mamtata2022@gmail.com"

pipeline {
    options {
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
        timestamps()
        ansiColor('xterm')
    }
    docker {
        label 'k0s'
        image '352708296901.dkr.ecr.eu-central-1.amazonaws.com/alexey_jenk_agent:7'
        args '--user root -v /var/run/docker.sock:/var/run/docker.sock'
    }

    environment {
        APP_ENV = "prod"
    }

    stages {
        stage('get last images from ECR') {
            steps {
                script {

                    sh "aws ecr list-images --repository-name alexey_bot_prod | jq \'.imageIds[] | .imageTag\' > images.txt"
                    JOB.images = readFile("${env.WORKSPACE}/images.txt").replace("\"", "").split("\n") as List
                    println(JOB.images)

                }

            }
        }

        stage('Input Images to deploy') {
            steps {
                script {

                    println(workspace)
                    def userInput = input id: 'UserInput', message: 'Please provide parameters.', ok: 'OK', parameters: [
                            [name: 'Temp_var', $class: 'WHideParameterDefinition', defaultValue: JOB.images.join(",")],
                            [$class: 'CascadeChoiceParameter', choiceType: 'PT_SINGLE_SELECT', filterLength: 1, filterable: false,
                             name  : 'Images', referencedParameters: 'Temp_var',
                             script: [$class: 'GroovyScript', fallbackScript: [classpath: [], oldScript: '', sandbox: true, script: 'return [\'error\']'],
                                      script: [classpath: [], oldScript: '', sandbox: true,
                                               script   : '''
try{
Temp_var.split(",").toList().sort()
}catch (Exception e) {return [e.getMessage()]}
                                 '''
                                      ]]]]

                    JOB.deploy_image = userInput['Images']

                }
            }
        }


        stage('Bot Deploy') {
            steps {
                script {
                    BOT_IMAGE_NAME = JOB.deploy_image
                }
                withCredentials([
                        string(credentialsId: 'telegram-bot-token', variable: 'TELEGRAM_TOKEN'),
                        file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')
                ]) {

                    sh 'aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin 352708296901.dkr.ecr.eu-central-1.amazonaws.com'
                    sh '''
                    K8S_CONFIGS=infra/k8s

                    # replace placeholders in YAML k8s files
                    bash common/replaceInFile.sh $K8S_CONFIGS/bot.yaml APP_ENV $APP_ENV
                    bash common/replaceInFile.sh $K8S_CONFIGS/bot.yaml BOT_IMAGE $BOT_IMAGE_NAME
                    bash common/replaceInFile.sh $K8S_CONFIGS/bot.yaml TELEGRAM_TOKEN $(echo -n $TELEGRAM_TOKEN | base64)


                    # apply the configurations to k8s cluster
                    kubectl apply --kubeconfig ${KUBECONFIG} -f $K8S_CONFIGS/bot.yaml
                    '''
                }
            }
        }
    }

    post {
        always {
            script {
                EMAIL_MAP = [
                        "Job Name"    : JOB_NAME,
                        "ENV"         : params.ENV,
                        "Build Number": BUILD_NUMBER,
                        "More Info At": "<a href=${BUILD_URL}console> Click here to view build console on Jenkins. </a>",
                        "painted"     : "false"
                ]
                global_sendGlobalMail.sendByMapFormat(JOB.email_recepients, currentBuild.result, EMAIL_MAP,
                        "Jenkins Report", "Build Notification - Jenkins Report", "BOT deploy")

            }
        }
    }
}
