pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                sh 'docker build -t sentinel-gateway:latest .'
                echo "testing CICD pipelin "
                echo " testing for multibranch pipeline"
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                sh '''
                    docker tag sentinel-gateway:latest vishwanathhubballi/sentinelgateway:latest
                    docker login -u vishwanathhubballi -p Matrix.neo
                    docker push vishwanathhubballi/sentinelgateway:latest
                
                '''
            }
        }
    }
}
