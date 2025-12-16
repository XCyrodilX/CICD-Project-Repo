pipeline {
    // Runs on Windows Jenkins node
    //test
    agent any 

    environment {
        // GitHub Container Registry
        DOCKER_REGISTRY = 'ghcr.io/xcyrodilx'
        DOCKER_DOMAIN   = 'ghcr.io'

        // Repo directories
        TF_DIR      = 'terraform'
        WEB_APP_DIR = 'bank-web'
        API_APP_DIR = 'bank-api'
    }
    
    stages {

        // ------------------------------------------------------------
        // CHECKOUT SOURCE CODE
        // ------------------------------------------------------------
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // ------------------------------------------------------------
        // CHECK DOCKER (fail fast if Docker Desktop engine isn't reachable)
        // ------------------------------------------------------------
        stage('Check Docker') {
            steps {
                bat 'docker version'
            }
        }

        // ------------------------------------------------------------
        // SET IMAGE TAG 
        // ------------------------------------------------------------
        stage('Set Image Tag') {
            steps {
                script {
                    env.IMAGE_TAG = bat(
                        returnStdout: true,
                        script: '@git rev-parse --short HEAD'
                    ).trim()

                    echo "Using IMAGE_TAG=${env.IMAGE_TAG}"
                }
            }
        }

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

        // ------------------------------------------------------------
        // BUILD & TEST APPLICATIONS
        // ------------------------------------------------------------
        stage('Build & Test Applications') {
            steps {

                // ----- BANK API (JAVA / MAVEN) -----
                dir(API_APP_DIR) {
                    bat 'mvnw.cmd clean install -DskipTests'
                    bat 'mvnw.cmd test'
                }

                // ----- BANK WEB (REACT) -----
                dir(WEB_APP_DIR) {
                    bat 'npm install'
                    bat 'npm run build'
                }
            }
        }

        // ------------------------------------------------------------
        // BUILD & PUSH DOCKER IMAGES
        // ------------------------------------------------------------
        stage('Containerize & Push Images') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'docker-registry-creds',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {

                    // FIX (minor): quote vars to avoid weird parsing
                    bat 'docker login -u "%DOCKER_USERNAME%" -p "%DOCKER_PASSWORD%" "%DOCKER_DOMAIN%"'

                    // ----- BANK API IMAGE -----
                    dir(API_APP_DIR) {
                        bat "docker build -t %DOCKER_REGISTRY%/bank-api:%IMAGE_TAG% ."
                        bat "docker push %DOCKER_REGISTRY%/bank-api:%IMAGE_TAG%"
                        bat "docker tag %DOCKER_REGISTRY%/bank-api:%IMAGE_TAG% %DOCKER_REGISTRY%/bank-api:latest"
                        bat "docker push %DOCKER_REGISTRY%/bank-api:latest"
                    }

                    // ----- BANK WEB IMAGE -----
                    dir(WEB_APP_DIR) {
                        bat "docker build -t %DOCKER_REGISTRY%/bank-web:%IMAGE_TAG% ."
                        bat "docker push %DOCKER_REGISTRY%/bank-web:%IMAGE_TAG%"
                        bat "docker tag %DOCKER_REGISTRY%/bank-web:%IMAGE_TAG% %DOCKER_REGISTRY%/bank-web:latest"
                        bat "docker push %DOCKER_REGISTRY%/bank-web:latest"
                    }
                }
            }
        }

        // ------------------------------------------------------------
        // NEW STAGE: DEPLOYMENT USING DOCKER COMPOSE
        // ------------------------------------------------------------
        stage('Deploy (Docker Compose)') {
            steps {
                bat 'docker-compose pull'
                bat 'docker-compose up -d --remove-orphans'
            }
        }

        // ------------------------------------------------------------
        // DEPLOY TO KUBERNETES (DOCKER DESKTOP)
        // ------------------------------------------------------------
        stage('Deploy to Kubernetes') {
            steps {
                // Ensure correct context
                bat 'kubectl config use-context docker-desktop'

                bat 'kubectl apply -f k8/namespace.yaml --validate=false'
                bat 'kubectl apply -f k8/bankapi.yaml --validate=false'
                bat 'kubectl apply -f k8/bankweb.yaml --validate=false'
            }
        }

        // ------------------------------------------------------------
        // POST-DEPLOYMENT VERIFICATION
        // ------------------------------------------------------------
        stage('Post-Deployment Verification') {
            steps {
                bat 'kubectl rollout status deployment/bank-api -n bank'
                bat 'kubectl rollout status deployment/bank-web -n bank'
                bat 'kubectl get pods -n bank'
                bat 'kubectl get svc -n bank'
            }
        }
    }

    // ------------------------------------------------------------
    // POST ACTIONS
    // ------------------------------------------------------------
    post {
        success {
            echo 'Deployment successful'
            echo 'Docker Compose: http://localhost:3000 (web), http://localhost:9090 (api)'
            echo 'Kubernetes: use kubectl port-forward if needed'
            echo 'Use port-forward in a terminal:'
            echo '  kubectl port-forward svc/bank-web -n bank 3000:80'
            echo '  kubectl port-forward svc/bank-api -n bank 9090:9090'
        }
        failure {
            echo 'Deployment failed check console output'
        }
        always {
            bat 'docker ps || exit /b 0'
        }
    }
}


