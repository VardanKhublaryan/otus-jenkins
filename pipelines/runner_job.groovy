pipeline {
    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'master', description: 'Branch to build')
        choice(name: 'TEST_TYPE', choices: ['all', 'api', 'web'], description: 'Select suite to RUN')
        // NEW: Parameter to choose which JJB jobs to UPDATE
        choice(name: 'JJB_UPDATE_TAG', choices: ['none', 'all', 'api', 'web'], description: 'Select JJB tag to UPDATE')
    }

    stages {
        stage('Initialize & JJB Update') {
            steps {
                script {
                    // 1. Clean up old results
                    dir('all-results') { deleteDir() }

                    // 2. Run JJB Update if requested
                    if (params.JJB_UPDATE_TAG != 'none') {
                        echo "Updating Jenkins Jobs via JJB for tag: ${params.JJB_UPDATE_TAG}"

                        def baseCmd = "jenkins-jobs --conf /home/vardan/otus-jenkins/uploader.ini --flush-cache update /home/vardan/otus-jenkins/jobs/"

                        if (params.JJB_UPDATE_TAG == 'all') {
                            sh "${baseCmd}"
                        } else {
                            sh "${baseCmd} --tags ${params.JJB_UPDATE_TAG}"
                        }
                    } else {
                        echo "Skipping JJB Update stage."
                    }
                }
            }
        }

        stage('Execute Tests') {
            steps {
                script {
                    def jobs = [:]

                    if (params.TEST_TYPE == 'all' || params.TEST_TYPE == 'api') {
                        jobs["API Tests"] = {
                            build job: 'Api_tests', parameters: [string(name: 'BRANCH', value: params.BRANCH)], propagate: false
                        }
                    }

                    if (params.TEST_TYPE == 'all' || params.TEST_TYPE == 'web') {
                        jobs["Web Tests"] = {
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