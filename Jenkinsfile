pipeline {
    agent any

    environment {
        COMPOSE_FILE = 'docker-compose.yml'

        // Repo directories
        TF_DIR      = 'terraform'

        // change this to your kubeconfig path
        // KUBECONFIG = 'C:\\Users\\dongk\\.kube\\config'
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

        stage('Terraform Provision') {
    steps {
        // This block injects the secrets you saved earlier
        withCredentials([
            string(credentialsId: 'AZURE_SUBSCRIPTION_ID', variable: 'ARM_SUBSCRIPTION_ID'),
            string(credentialsId: 'AZURE_TENANT_ID',       variable: 'ARM_TENANT_ID'),
            string(credentialsId: 'AZURE_CLIENT_ID',       variable: 'ARM_CLIENT_ID'),
            string(credentialsId: 'AZURE_CLIENT_SECRET',   variable: 'ARM_CLIENT_SECRET')
        ]) {
            dir(TF_DIR) {
                // We use 'bat' because your agent is Windows 11
                bat 'terraform init'
                // Tip: Use 'plan' first to see what will happen
                bat 'terraform plan' 
                bat 'terraform apply -auto-approve'
            }
        }
    }
}

        // ------------------------------------------------------------
        // BUILD & TEST APPLICATIONS; REMOVE OLD CONTAINERS
        // ------------------------------------------------------------
        stage('Build & Deploy with Docker Compose') {
            steps {
                echo "Stopping and removing any existing containers..."

                bat """
                cd %WORKSPACE%
                docker-compose -f %COMPOSE_FILE% down || exit /b 0
                docker compose down --remove-orphans || exit /b 0
                """

                echo "Building and starting containers with docker-compose..."
                bat """
                cd %WORKSPACE%
                docker-compose -f %COMPOSE_FILE% up -d --build
                """
            }
        }

        stage('Show Running Containers') {
            steps {
                bat "docker ps"
            }
        }

        // ------------------------------------------------------------
        // DEPLOY TO KUBERNETES AND VERIFY (DOCKER DESKTOP)
        // ------------------------------------------------------------
        stage('Deploy to Kubernetes') {
    steps {
        withCredentials([
            string(credentialsId: 'AZURE_SUBSCRIPTION_ID', variable: 'ARM_SUBSCRIPTION_ID'),
            string(credentialsId: 'AZURE_TENANT_ID',       variable: 'ARM_TENANT_ID'),
            string(credentialsId: 'AZURE_CLIENT_ID',       variable: 'ARM_CLIENT_ID'),
            string(credentialsId: 'AZURE_CLIENT_SECRET',   variable: 'ARM_CLIENT_SECRET')
        ]) {
            bat '''
            :: Log in to Azure and get the credentials for the new AKS cluster
            az login --service-principal -u %ARM_CLIENT_ID% -p %ARM_CLIENT_SECRET% --tenant %ARM_TENANT_ID%
            az aks get-credentials --resource-group my-jenkins-project-rg --name my-aks-cluster --overwrite-existing
            
            :: Now run your deployments
            kubectl apply -f k8/namespace.yaml
            kubectl apply -f k8/bank-api.yaml
            kubectl apply -f k8/bank-web.yaml
            '''
        }
    }
}

    post {
        success {
            echo " Deployment successful!"
            echo "- Docker Compose:"
            echo "    Web: http://localhost:3000"
            echo "    API: http://localhost:9090"
        }
        failure {
            echo "Deployment failed check console log for details."
        }
    }
}


