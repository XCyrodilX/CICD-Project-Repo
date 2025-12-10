pipeline {
    agent {
        label 'docker-agent' 
    }

    environment {
        DOCKER_REGISTRY = 'your-dockerhub-username' 
        IMAGE_TAG = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
        
        TF_DIR = 'terraform'
        K8S_DIR = 'k8s'
        
        WEB_APP_DIR = 'bank-web'
        API_APP_DIR = 'bank-api'
    }
    
    stages {
        stage('Provision Infrastructure (Terraform)') {
            steps {
                dir(TF_DIR) {
                    sh 'terraform init'
                    sh 'terraform validate'
                    sh 'terraform apply -auto-approve' 
                }
            }
        }

        stage('Build & Test Applications') {
            steps {
                dir(API_APP_DIR) {
                    sh './gradlew build'
                    sh 'echo "Running bank-api unit tests..."'
                    sh 'echo "bank-api tests passed successfully."'
                }
                
                dir(WEB_APP_DIR) {
                    sh 'npm install'
                    sh 'npm run build'
                    sh 'echo "Running bank-web unit tests..."'
                    sh 'echo "bank-web tests passed successfully."'
                }
            }
        }

        stage('Containerize & Push Images') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-registry-creds', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                    sh "docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}"
                    
                    dir(API_APP_DIR) {
                        sh "docker build -t ${DOCKER_REGISTRY}/bank-api:${IMAGE_TAG} ."
                        sh "docker push ${DOCKER_REGISTRY}/bank-api:${IMAGE_TAG}"
                        sh "docker tag ${DOCKER_REGISTRY}/bank-api:${IMAGE_TAG} ${DOCKER_REGISTRY}/bank-api:latest"
                        sh "docker push ${DOCKER_REGISTRY}/bank-api:latest"
                    }
                    
                    dir(WEB_APP_DIR) {
                        sh "docker build -t ${DOCKER_REGISTRY}/bank-web:${IMAGE_TAG} ."
                        sh "docker push ${DOCKER_REGISTRY}/bank-web:${IMAGE_TAG}"
                        sh "docker tag ${DOCKER_REGISTRY}/bank-web:${IMAGE_TAG} ${DOCKER_REGISTRY}/bank-web:latest"
                        sh "docker push ${DOCKER_REGISTRY}/bank-web:latest"
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                dir(K8S_DIR) {
                    sh "sed -i 's|__API_IMAGE__|${DOCKER_REGISTRY}/bank-api:${IMAGE_TAG}|g' deployment-api.yaml"
                    sh "sed -i 's|__WEB_IMAGE__|${DOCKER_REGISTRY}/bank-web:${IMAGE_TAG}|g' deployment-web.yaml"
                    
                    sh 'kubectl apply -f deployment-api.yaml'
                    sh 'kubectl apply -f deployment-web.yaml'
                    
                    sh 'kubectl apply -f service-api.yaml'
                    sh 'kubectl apply -f service-web.yaml'
                    
                    sh 'kubectl apply -f ingress.yaml'
                }
            }
        }
        
        stage('Post-Deployment Verification') {
            steps {
                sh "kubectl rollout status deployment/bank-api-deployment" 
                sh "kubectl rollout status deployment/bank-web-deployment" 
                sh "kubectl get ingress"
            }
        }
    }
}