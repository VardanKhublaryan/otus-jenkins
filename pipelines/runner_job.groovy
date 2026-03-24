pipeline {
    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'master', description: 'Branch to build')
        choice(name: 'TEST_TYPE', choices: ['all', 'api', 'web'], description: 'Select suite to RUN')
    }

    stages {

        stage('Execute Tests') {
            steps {
                script {
                    def jobs = [:]

                    if (params.TEST_TYPE == 'all' || params.TEST_TYPE == 'api') {
                        jobs["API_Tests"] = {
                            build job: 'Api_tests', parameters: [string(name: 'BRANCH', value: params.BRANCH)], propagate: false
                        }
                    }

                    if (params.TEST_TYPE == 'all' || params.TEST_TYPE == 'web') {
                        jobs["Web_Tests"] = {
                            build job: 'Web_tests', parameters: [string(name: 'BRANCH', value: params.BRANCH)], propagate: false
                        }
                    }

                    parallel jobs
                }
            }
        }

        stage('Collect and Mix Results') {
            steps {
                script {
                    if (params.TEST_TYPE == 'all' || params.TEST_TYPE == 'api') {
                        copyArtifacts(
                                projectName: 'Api_tests',
                                selector: lastCompleted(),
                                filter: '**/allure-results/**',
                                target: 'all-results/api',
                                flatten: true,
                                optional: true
                        )
                    }
                    if (params.TEST_TYPE == 'all' || params.TEST_TYPE == 'web') {
                        copyArtifacts(
                                projectName: 'Web_tests',
                                selector: lastCompleted(),
                                filter: 'target/allure-results/**',
                                target: 'all-results/web',
                                flatten: true,
                                optional: true
                        )
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Generating Mixed Allure Report..."
            allure([
                    reportBuildPolicy: 'ALWAYS',
                    results: [
                            [path: 'all-results/api'],
                            [path: 'all-results/web']
                    ]
            ])
        }
    }
}