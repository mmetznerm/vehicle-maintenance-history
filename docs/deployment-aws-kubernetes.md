# Vehicle Maintenance History AWS and Kubernetes demo deployment

This document describes the prepared cloud deployment path for Vehicle Maintenance History.

`AutoLog` is used as the public layout/brand and domain naming, while the official application name remains `vehicle-maintenance-history`.

The target is a public AWS demo environment for portfolio usage. Production deployment is intentionally not enabled yet; the pipeline can evolve later to a gated production release flow.

## Target architecture

- Backend: Spring Boot container running on Amazon EKS.
- Frontend: React/Vite static build served by Nginx on Amazon EKS.
- Images: Amazon ECR repositories.
- Database: Amazon RDS PostgreSQL outside Kubernetes.
- Ingress: Kubernetes Ingress reconciled by AWS Load Balancer Controller.
- CI/CD: GitHub Actions using OIDC to assume an AWS IAM role.

## Environments

| Environment | Namespace | Frontend host | API host | Spring profile |
|---|---|---|---|---|
| Local | Docker Compose | `localhost:5173` | `localhost:8080` | `local` |
| Demo | `autolog-demo` | `demo.autolog.com.br` | `api-demo.autolog.com.br` | `demo` |

## Local development

Run the existing local stack:

```bash
docker compose up --build
```

Services:

- Frontend: `http://localhost:5173`
- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Stop the stack:

```bash
docker compose down
```

The local flow uses PostgreSQL from Docker Compose. Kubernetes is not required for day-to-day local development.

## Backend configuration

The backend reads these environment variables:

```text
SPRING_PROFILES_ACTIVE
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
APP_SECURITY_CORS_ALLOWED_ORIGINS
```

Spring profiles:

- `local`: has safe local defaults.
- `demo`: expects RDS/JWT configuration from environment variables.

## Frontend configuration

The containerized frontend image is served by Nginx.

At container startup, Nginx writes `/config.js` from this environment variable:

```text
VITE_API_BASE_URL
```

This keeps a single frontend image reusable across environments.

Expected demo value:

```text
https://api-demo.autolog.com.br
```

## AWS resources

The repository Terraform creates the demo AWS resources after review and explicit apply:

```text
AWS region: us-east-1
ECR repositories:
  autolog-backend
  autolog-frontend
EKS cluster:
  autolog-demo by default
RDS PostgreSQL:
  demo database reachable from EKS
AWS Load Balancer Controller:
  IAM role created by Terraform, Helm installation handled by GitHub Actions
Optional DNS/TLS:
  Route 53 hosted zone and ACM certificate after the domain is purchased
```

RDS connection strings should use JDBC format:

```text
jdbc:postgresql://<rds-endpoint>:5432/autolog
```

## GitHub OIDC and IAM

Terraform creates an IAM role trusted by GitHub Actions OIDC for this repository when `enable_github_actions_oidc = true`.

The role needs permissions for:

- ECR login and image push/pull for `autolog-backend` and `autolog-frontend`.
- EKS cluster access through `aws eks update-kubeconfig`.
- Kubernetes deployment permissions through the EKS access model.

Store the role ARN as a GitHub repository or environment variable:

```text
AWS_GITHUB_ACTIONS_ROLE_ARN
```

No long-lived `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY` is required.

## GitHub variables

Repository or `demo` environment variables:

```text
AWS_GITHUB_ACTIONS_ROLE_ARN
AWS_LOAD_BALANCER_CONTROLLER_ROLE_ARN
AWS_VPC_ID
EKS_CLUSTER_NAME
DEMO_BACKEND_HOST
DEMO_FRONTEND_HOST
DEMO_SPRING_DATASOURCE_URL
DEMO_ACM_CERTIFICATE_ARN
```

`DEMO_ACM_CERTIFICATE_ARN` is optional until HTTPS is enabled.

Recommended `demo` environment secrets:

```text
DEMO_SPRING_DATASOURCE_USERNAME
DEMO_SPRING_DATASOURCE_PASSWORD
DEMO_JWT_SECRET
```

Recommended GitHub Environment:

- `demo`: no manual approval required.

## Kubernetes secrets

The Helm chart expects an existing backend secret by default. The `Deploy demo` workflow creates or updates it from GitHub environment secrets before running Helm:

Demo:

```bash
kubectl create namespace autolog-demo
kubectl -n autolog-demo create secret generic autolog-demo-backend-secrets \
  --from-literal=SPRING_DATASOURCE_USERNAME='<demo-db-user>' \
  --from-literal=SPRING_DATASOURCE_PASSWORD='<demo-db-password>' \
  --from-literal=JWT_SECRET='<strong-demo-jwt-secret>'
```

Do not commit real secret values to git.

## Helm commands

Render locally:

```bash
helm template vehicle-maintenance-history deploy/helm/autolog
helm template vehicle-maintenance-history deploy/helm/autolog -f deploy/helm/autolog/values-demo.yaml
```

Install or upgrade demo manually:

```bash
helm upgrade --install vehicle-maintenance-history deploy/helm/autolog \
  --namespace autolog-demo \
  --create-namespace \
  -f deploy/helm/autolog/values-demo.yaml \
  --set-string backend.image.repository='<account-id>.dkr.ecr.us-east-1.amazonaws.com/autolog-backend' \
  --set-string backend.image.tag='<image-tag>' \
  --set-string frontend.image.repository='<account-id>.dkr.ecr.us-east-1.amazonaws.com/autolog-frontend' \
  --set-string frontend.image.tag='<image-tag>' \
  --set-string backend.env.datasourceUrl='<demo-jdbc-url>' \
  --set-string backend.env.corsAllowedOrigins='https://demo.autolog.com.br' \
  --set-string frontend.env.apiBaseUrl='https://api-demo.autolog.com.br'
```

## CI/CD flow

Pull requests run:

- backend unit and integration tests: `./mvnw verify -Pintegration-tests`
- frontend build: `npm run build`
- Docker Compose validation
- backend Docker image build
- frontend Docker image build
- Helm lint and template rendering for base and demo values
- Terraform formatting and validation

Merges to `main`:

- validate backend and frontend again
- build backend and frontend images
- push both images to Amazon ECR
- install or upgrade the AWS Load Balancer Controller
- create or update the backend Kubernetes secret
- deploy automatically to `autolog-demo`

## DNS and TLS

DNS and TLS are ready to be enabled when `autolog.com.br` is purchased.

Terraform can create:

- Route 53 hosted zone for `autolog.com.br`.
- ACM certificate for `demo.autolog.com.br` and `api-demo.autolog.com.br`.
- DNS validation records when a hosted zone ID is available.

The GitHub Actions deploy workflow enables HTTPS on the ALB Ingress when `DEMO_ACM_CERTIFICATE_ARN` is configured. Route 53 records for the ALB can be added after the first successful Ingress reconciliation exposes the ALB DNS name.

## Terraform foundation

The repository includes a first Terraform foundation in `infra/terraform`.

Remote Terraform state is bootstrapped separately in:

```text
infra/terraform-bootstrap
```

Bootstrap scope:

- S3 bucket for Terraform state.
- S3 versioning, encryption and public access block.
- Bucket policy denying insecure transport.
- DynamoDB table for state locking.

After bootstrap, initialize the main stack with an S3 backend config based on:

```text
infra/terraform/backend.demo.hcl.example
```

The main Terraform scope is intentionally small and reviewable:

- AWS provider configured for `us-east-1`.
- Demo VPC with public, private application and private database subnets across two availability zones.
- Optional single NAT Gateway for private application subnet egress.
- EKS demo cluster, managed node group and basic managed add-ons.
- EKS OIDC provider for Kubernetes service account IAM roles.
- AWS Load Balancer Controller IAM role and policy for IRSA.
- GitHub Actions cluster access entry when GitHub OIDC is enabled.
- RDS PostgreSQL demo database in isolated private database subnets.
- Optional Route 53 hosted zone and ACM certificate for the demo hosts.
- ECR repositories:
  - `autolog-backend`
  - `autolog-frontend`
- ECR image scanning and lifecycle policies.
- Optional GitHub Actions OIDC/IAM deploy role.

After `terraform apply`, use these outputs to configure GitHub:

```bash
terraform -chdir=infra/terraform output -raw github_actions_role_arn
terraform -chdir=infra/terraform output -raw aws_load_balancer_controller_role_arn
terraform -chdir=infra/terraform output -raw vpc_id
terraform -chdir=infra/terraform output -raw eks_cluster_name
terraform -chdir=infra/terraform output -raw rds_datasource_url
terraform -chdir=infra/terraform output -raw rds_master_username
terraform -chdir=infra/terraform output -raw rds_master_password
terraform -chdir=infra/terraform output -raw acm_certificate_arn
```

Validate Terraform locally:

```bash
terraform -chdir=infra/terraform-bootstrap fmt -recursive
terraform -chdir=infra/terraform-bootstrap init -backend=false
terraform -chdir=infra/terraform-bootstrap validate
terraform -chdir=infra/terraform fmt -recursive
terraform -chdir=infra/terraform init -backend=false
terraform -chdir=infra/terraform validate
```

Preview a future AWS change only after selecting the target AWS account and reviewing variables:

```bash
terraform -chdir=infra/terraform init -backend-config=backend.demo.hcl
terraform -chdir=infra/terraform plan -var-file=foundation.demo.tfvars
```

Do not run `terraform apply` without a reviewed plan and explicit approval.
