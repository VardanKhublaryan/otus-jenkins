node('maven') {
    env.JOBS_DIR = "${WORKSPACE}/jobs"
    env.CONFIG_FILE = "${WORKSPACE}/uploader.ini"

    stage('Start Upload') {
        echo "Starting Jenkins Job Uploader..."
    }

    stage('Checkout Repo') {
        git branch: 'master', url: 'https://github.com/VardanKhublaryan/otus-jenkins.git'
    }

    stage('Create uploader.ini') {
        sh """
        cat > ${CONFIG_FILE} <<'EOF'
[job_builder]
recursive=True
keep_descriptions=False

[jenkins]
url=http://188.130.251.59:6060/jenkins/
user=admin
password=119b926a54146bbf90880da7d03643a5ea
query_plugins_info=False
EOF
        """
    }

    stage('Run Upload Script') {
        sh "jenkins-jobs --conf  ${CONFIG_FILE} --flush-cache update ${JOBS_DIR}"
    }

    stage('Finish Upload') {
        echo "Upload finished successfully!"
    }
}