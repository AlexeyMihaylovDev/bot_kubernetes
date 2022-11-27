pipeline {
    agent {
        docker {
            label 'linux'
            image '352708296901.dkr.ecr.eu-central-1.amazonaws.com/alexey_jenk_agent:7'
            args '--user root -v /var/run/docker.sock:/var/run/docker.sock'
        }
    }
    triggers {
        GenericTrigger(
                genericVariables: [
                        [key: 'ref', value: '$.ref'],
                        [key: 'action', value: '$.action'],
                        [key: 'issue_number', value: '$.issue_number'],
                        [key: 'pull_request_number', value: '$.pull_request_number'],
                ],

                token: 'bot_dev',
                tokenCredentialId: '',

                printContributedVariables: true,
                printPostContent: true,

                silentResponse: false,

                shouldNotFlattern: false,

                regexpFilterText: '$action',
                regexpFilterExpression: '^(created|opened|reopened|synchronize)$'
        )
    }
    stages {
        stage('Unittest Bot') {
            steps {
                echo 'testing bot...'
            }
        }
        stage('Unittest Worker') {
            steps {
                echo 'testing worker...'
            }
        }
        stage('Linting test') {
            steps {
              echo 'code linting'
            }
        }
    }
}