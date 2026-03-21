pipeline {
    agent any

    stages {
        stage('Initialize') {
            steps {
                echo "Target selected: ${params.TEST_TYPE}"
            }
        }

        stage('Execute Tests') {
            steps {
                script {
                    switch(params.TEST_TYPE) {
                        case 'all':
                            parallel(
                                    "API Tests": {
                                        build job: 'job-api', parameters: [string(name: 'BRANCH', value: "master")]
                                    },
                                    "Web Tests": {
                                        build job: 'job-ui', parameters: [string(name: 'BRANCH', value: "main")]
                                    }
                            )
                            break

                        case 'api':
                            build job: 'job-api', parameters: [string(name: 'BRANCH', value: "master")]
                            break

                        case 'web':
                            build job: 'job-ui', parameters: [string(name: 'BRANCH', value: "main")]
                            break

                        default:
                            error "Unknown TEST_TYPE: ${params.TEST_TYPE}"
                    }
                }
            }
        }
    }

    stage('Collect Results') {
        steps {
            script {
                // This is the "Magic" part: Copying results from the other jobs
                // Requires "Copy Artifact Plugin" in Jenkins
                if (params.TEST_TYPE == 'api' || params.TEST_TYPE == 'all') {
                    copyArtifacts(projectName: 'jon-api', target: 'all-results/api')
                }
                if (params.TEST_TYPE == 'web' || params.TEST_TYPE == 'all') {
                    copyArtifacts(projectName: 'job-ui', target: 'all-results/web')
                }
            }
        }
    }
}

post {
    always {
        // Generate the Allure report for the Runner Job
        allure([
                results: [[path: 'all-results/api'], [path: 'all-results/web']]
        ])
    }
}