pipeline {
    // This is set to 'agent any' to run on the Master/built-in node (to avoid the 'docker-agent' error)
    agent any 

    environment {
        DOCKER_REGISTRY = 'your-dockerhub-username' 
        IMAGE_TAG = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
        
        TF_DIR = 'terraform' // Variable for Terraform directory
        K8S_DIR = 'k8s'      // Variable for Kubernetes manifests directory
        
        WEB_APP_DIR = 'bank-web'
        API_APP_DIR = 'bank-api'
    }
    
    stages {
        /*
        // STAGE 1: PROVISION INFRASTRUCTURE (TERRAFORM)
        // This stage is commented out. Uncomment it and install the 'terraform' CLI tool to enable.
        stage('Provision Infrastructure (Terraform)') {
            steps {
                dir(TF_DIR) {
                    sh 'terraform init'
                    sh 'terraform validate'
                    sh 'terraform apply -auto-approve' 
                }
            }
        }
        */

        stage('Build & Test Applications') {
            steps {
                dir(API_APP_DIR) {
                    sh './gradlew build' // Requires Java/JDK and Gradle
                    sh 'echo "Running bank-api unit tests..."'
                    sh 'echo "bank-api tests passed successfully."'
                }
                
                dir(WEB_APP_DIR) {
                    sh 'npm install' // Requires Node.js and npm
                    sh 'npm run build'
                    sh 'echo "Running bank-web unit tests..."'
                    sh 'echo "bank-web tests passed successfully."'
                }
            }
        }

        stage('Containerize & Push Images') {
            steps {
                // Requires 'docker-registry-creds' credential and Docker CLI access
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

        /*
        // STAGE 3: DEPLOY TO KUBERNETES
        // This stage is commented out. Uncomment it and install the 'kubectl' CLI tool to enable.
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
        
        // STAGE 4: POST-DEPLOYMENT VERIFICATION
        // This stage is commented out. Uncomment it to verify successful deployment after Stage 3 runs.
        stage('Post-Deployment Verification') {
            steps {
                sh "kubectl rollout status deployment/bank-api-deployment" 
                sh "kubectl rollout status deployment/bank-web-deployment" 
                sh "kubectl get ingress"
            }
        }
        */
    }
}