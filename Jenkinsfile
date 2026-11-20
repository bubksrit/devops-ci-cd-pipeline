pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
    }

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    environment {
        APP_BASE_VERSION = '0.0.1'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    def checkoutInfo = checkout scm
                    env.GIT_SHA = checkoutInfo.GIT_COMMIT.take(7)
                    env.APP_VERSION = "${env.APP_BASE_VERSION}-${env.BUILD_NUMBER}-${env.GIT_SHA}"
                    echo "Building Todo API version ${env.APP_VERSION}"
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    def command = "mvn -B -Drevision=${env.APP_VERSION} test"
                    if (isUnix()) {
                        sh command
                    } else {
                        bat command
                    }
                }
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                script {
                    def command = "mvn -B -Drevision=${env.APP_VERSION} -DskipTests clean package"
                    if (isUnix()) {
                        sh command
                    } else {
                        bat command
                    }
                    echo "Packaged artifact: todo-api-${env.APP_VERSION}.jar"
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: "target/todo-api-${env.APP_VERSION}.jar", fingerprint: true
        }
    }
}
