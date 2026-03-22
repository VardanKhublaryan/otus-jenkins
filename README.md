jenkins-jobs --conf /home/vardan/otus-jenkins/uploader.ini --flush-cache update /home/vardan/otus-jenkins/jobs/

run jobs
java -jar jenkins-cli.jar -s http://188.130.251.59:6060/jenkins/ -auth admin:119b926a54146bbf90880da7d03643a5ea build runner_job -p TEST_TYPE=all -f
