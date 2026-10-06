pipeline {
    agent any
    tools {
        maven 'maven'
        jdk 'jdk17'
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('jacoco') {
        	steps {
        		jacoco(
              	execPattern: 'target/*.exec',
              	classPattern: 'target/classes',
              	sourcePattern: 'src/main/java',
              	exclusionPattern: 'src/test*'
        		)
        	}
        }
        stage('Sonarqube') {
            steps {
                withSonarQubeEnv('sonarcloud') {
                	sh 'mvn sonar:sonar -Dsonar.organization=laffer1-github'
                }
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }
    }
}
