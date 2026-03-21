pipeline {
    agent any

    parameters {
        // Defining them here in the script acts as a backup to the YAML
        string(name: 'BRANCH', defaultValue: 'master', description: 'Branch to build')
        choice(name: 'TEST_TYPE', choices: ['all', 'api', 'web'], description: 'Select suite')
    }

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
                                        build job: 'Api_tests', parameters: [string(name: 'BRANCH', value: "master")]
                                    },
                                    "Web Tests": {
                                        build job: 'Web_tests', parameters: [string(name: 'BRANCH', value: "main")]
                                    }
                            )
                            break

                        case 'api':
                            build job: 'Api_tests', parameters: [string(name: 'BRANCH', value: "master")]
                            break

                        case 'web':
                            build job: 'Web_tests', parameters: [string(name: 'BRANCH', value: "main")]
                            break

                        default:
                            error "Unknown TEST_TYPE: ${params.TEST_TYPE}"
                    }
                }
            }
        }

        stage('Collect Results') {
            steps {
                script {
                    // Note: Ensure 'Copy Artifact Plugin' is installed in Jenkins
                    // Also check your job names: you had 'jon-api' (typo) vs 'job-api'
                    if (params.TEST_TYPE == 'api' || params.TEST_TYPE == 'all') {
                        copyArtifacts(projectName: 'Api_tests',filter: 'target/allure-results/**', target: 'all-results/api', optional: true)
                    }
                    if (params.TEST_TYPE == 'web' || params.TEST_TYPE == 'all') {
                        copyArtifacts(projectName: 'Web_tests',filter: 'target/allure-results/**', target: 'all-results/web', optional: true)
                    }
                }
            }
        }
    } // End of Stages

    post {
        always {
            echo "Generating Allure Report..."
            allure([
                    results: [[path: 'all-results/api'], [path: 'all-results/web']]
            ])
        }
    }
} // End of Pipeline