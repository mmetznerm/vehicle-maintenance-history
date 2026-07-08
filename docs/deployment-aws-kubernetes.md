# Vehicle Maintenance History AWS and Kubernetes demo deployment

This document describes the prepared cloud deployment path for Vehicle Maintenance History.

`AutoLog` is used as the public layout/brand and domain naming, while the official application name remains `vehicle-maintenance-history`.

The target is a public AWS demo environment for portfolio usage. Production deployment is intentionally not enabled yet; the pipeline can evolve later to a gated production release flow.

## Target architecture

- Backend: Spring Boot container running on Amazon EKS.
- Frontend: React/Vite static build served by Nginx on Amazon EKS.
- Images: Amazon ECR repositories.
- Database: Amazon RDS PostgreSQL outside Kubernetes.
- Ingress: Kubernetes Ingress prepared for AWS Load Balancer Controller.
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

## AWS resources expected

Create these resources manually or with reviewed Terraform changes:

```text
AWS region: us-east-1
ECR repositories:
  autolog-backend
  autolog-frontend
EKS cluster:
  any chosen cluster name, referenced by GitHub variable EKS_CLUSTER_NAME
RDS PostgreSQL:
  demo database reachable from EKS
AWS Load Balancer Controller:
  installed in the EKS cluster if using ingressClassName: alb
```

RDS connection strings should use JDBC format:

```text
jdbc:postgresql://<rds-endpoint>:5432/autolog
```

## GitHub OIDC and IAM

Create an IAM role trusted by GitHub Actions OIDC for this repository.

The role needs permissions for:

- ECR login and image push/pull for `autolog-backend` and `autolog-frontend`.
- EKS cluster access through `aws eks update-kubeconfig`.
- Kubernetes deployment permissions through the EKS access model or `aws-auth`, depending on how the cluster is managed.

Store the role ARN as a GitHub repository or environment variable:

```text
AWS_GITHUB_ACTIONS_ROLE_ARN
```

No long-lived `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY` is required.

## GitHub variables

Repository or `demo` environment variables:

```text
AWS_GITHUB_ACTIONS_ROLE_ARN
EKS_CLUSTER_NAME
DEMO_SPRING_DATASOURCE_URL
```

Recommended GitHub Environment:

- `demo`: no manual approval required.

## Kubernetes secrets

The Helm chart expects an existing backend secret by default.

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
  --set-string backend.env.datasourceUrl='<demo-jdbc-url>'
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
- deploy automatically to `autolog-demo`

## DNS and TLS notes

The chart currently prepares HTTP ingress hosts.

Before making the demo public, add:

- Route 53 records pointing `demo.autolog.com.br` and `api-demo.autolog.com.br` to the load balancer.
- ACM certificates for `demo.autolog.com.br` and `api-demo.autolog.com.br`.
- HTTPS listener annotations or TLS configuration in `values-demo.yaml`.

## Terraform foundation

The repository includes a first Terraform foundation in `infra/terraform`.

The initial Terraform scope is intentionally small and reviewable:

- AWS provider configured for `us-east-1`.
- ECR repositories:
  - `autolog-backend`
  - `autolog-frontend`
- ECR image scanning and lifecycle policies.
- Optional GitHub Actions OIDC/IAM deploy role, disabled by default.
- Documented placeholders for future VPC, EKS, RDS, DNS and TLS work.

Initialize and validate Terraform locally:

```bash
terraform -chdir=infra/terraform fmt -recursive
terraform -chdir=infra/terraform init -backend=false
terraform -chdir=infra/terraform validate
```

Preview a future AWS change only after selecting the target AWS account and reviewing variables:

```bash
terraform -chdir=infra/terraform plan
```

Do not run `terraform apply` without a reviewed plan and explicit approval.

No remote Terraform state backend is configured yet. Use `init -backend=false` for local validation until a backend, such as S3 with DynamoDB locking, is reviewed and added.
