node('maven') {

    def apiBuild = null
    def webBuild = null

    stage('Run All Tests in Parallel') {
        parallel(
                'API Tests': {
                    script {
                        apiBuild = build job: 'Api_tests', propagate: false
                    }
                },
                'Web Tests': {
                    script {
                        webBuild = build job: 'Web_tests', propagate: false
                    }
                }
        )
    }

    stage('Collect Allure Results') {
        script {
            echo "API build number: ${apiBuild.number}, result: ${apiBuild.result}"
            echo "Mobile build number: ${mobileBuild.number}, result: ${mobileBuild.result}"
            echo "Web build number: ${webBuild.number}, result: ${webBuild.result}"

            if (apiBuild != null) {
                copyArtifacts(
                        projectName: 'Api_tests',
                        selector: lastSuccessful(),
                        filter: 'allure-results/**',
                        target: 'allure-results/api',
                        flatten: true,
                        optional: true
                )
            }

            if (webBuild != null) {
                copyArtifacts(
                        projectName: 'Web_tests',
                        selector: lastSuccessful(),
                        filter: 'allure-results/**',
                        target: 'allure-results/web',
                        flatten: true,
                        optional: true
                )
            }
        }
    }

    stage('Generate Allure Report') {
        script {
            allure includeProperties: false, jdk: '', results: [
                    [path: 'allure-results/api'],
                    [path: 'allure-results/mobile'],
                    [path: 'allure-results/web']
            ]
        }
    }
}