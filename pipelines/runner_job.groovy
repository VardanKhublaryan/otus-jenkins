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
                    switch (params.TEST_TYPE) {
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
                    // Using 'all-results' as the base folder to match the post block
                    if (params.TEST_TYPE == 'api' || params.TEST_TYPE == 'all') {
                        copyArtifacts(
                                projectName: 'Api_tests',
                                selector: lastSuccessful(),
                                // Ensure this filter matches what was archived in the child job
                                filter: '**/allure-results/**',
                                target: 'all-results/api',
                                flatten: true,
                                optional: true
                        )
                    }
                    if (params.TEST_TYPE == 'web' || params.TEST_TYPE == 'all') {
                        copyArtifacts(
                                projectName: 'Web_tests',
                                selector: lastSuccessful(),
                                filter: '**/allure-results/**',
                                target: 'all-results/web',
                                flatten: true,
                                optional: true
                        )
                    }
                }
            }
        }
    } // End of Stages

    post {
        always {
            echo "Generating Allure Report..."
            allure([
                    // These paths now EXACTLY match the 'target' in the copyArtifacts step
                    results: [
                            [path: 'all-results/api'],
                            [path: 'all-results/web']
                    ]
            ])
        }
    }
}