# Vehicle Maintenance History AWS and Kubernetes deployment

This document describes the prepared cloud deployment path for Vehicle Maintenance History.

`AutoLog` is used as the public layout/brand and domain naming, while the official application name remains `vehicle-maintenance-history`.

No AWS resources are created by this repository automatically. The workflows assume that ECR, EKS, RDS, IAM/OIDC and Kubernetes secrets already exist or are created later through reviewed infrastructure changes.

## Target architecture

- Backend: Spring Boot container running on Amazon EKS.
- Frontend: React/Vite static build served by Nginx on Amazon EKS.
- Images: Amazon ECR repositories.
- Database: Amazon RDS PostgreSQL outside Kubernetes.
- Ingress: Kubernetes Ingress, prepared for AWS Load Balancer Controller.
- CI/CD: GitHub Actions using OIDC to assume an AWS IAM role.

## Environments

| Environment | Namespace | Frontend host | API host | Spring profile |
|---|---|---|---|---|
| Local | Docker Compose | `localhost:5173` | `localhost:8080` | `local` |
| Stage | `autolog-stage` | `stage.autolog.com.br` | `api-stage.autolog.com.br` | `stage` |
| Production | `autolog-prod` | `www.autolog.com.br` | `api.autolog.com.br` | `prod` |

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

The local flow still uses PostgreSQL from Docker Compose. Kubernetes is not required for day-to-day local development.

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
- `stage`: expects RDS/JWT configuration from environment variables.
- `prod`: expects RDS/JWT configuration from environment variables.

## Frontend configuration

The production frontend image is served by Nginx.

At container startup, Nginx writes `/config.js` from this environment variable:

```text
VITE_API_BASE_URL
```

This keeps a single frontend image reusable across stage and production.

Expected values:

- Stage: `https://api-stage.autolog.com.br`
- Production: `https://api.autolog.com.br`

## AWS resources expected

Create these resources manually or with reviewed Terraform/CloudFormation later:

```text
AWS region: us-east-1
ECR repositories:
  autolog-backend
  autolog-frontend
EKS cluster:
  any chosen cluster name, referenced by GitHub variable EKS_CLUSTER_NAME
RDS PostgreSQL:
  stage database reachable from EKS
  production database reachable from EKS
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

Repository or environment variables:

```text
AWS_GITHUB_ACTIONS_ROLE_ARN
EKS_CLUSTER_NAME
STAGE_SPRING_DATASOURCE_URL
PROD_SPRING_DATASOURCE_URL
```

Recommended GitHub Environments:

- `stage`: no manual approval required.
- `production`: required reviewers enabled for manual approval.

## Kubernetes secrets

The Helm chart expects existing backend secrets by default.

Stage:

```bash
kubectl create namespace autolog-stage
kubectl -n autolog-stage create secret generic autolog-stage-backend-secrets \
  --from-literal=SPRING_DATASOURCE_USERNAME='<stage-db-user>' \
  --from-literal=SPRING_DATASOURCE_PASSWORD='<stage-db-password>' \
  --from-literal=JWT_SECRET='<strong-stage-jwt-secret>'
```

Production:

```bash
kubectl create namespace autolog-prod
kubectl -n autolog-prod create secret generic autolog-prod-backend-secrets \
  --from-literal=SPRING_DATASOURCE_USERNAME='<prod-db-user>' \
  --from-literal=SPRING_DATASOURCE_PASSWORD='<prod-db-password>' \
  --from-literal=JWT_SECRET='<strong-prod-jwt-secret>'
```

Do not commit real secret values to git.

## Helm commands

Render locally:

```bash
helm template vehicle-maintenance-history deploy/helm/autolog
helm template vehicle-maintenance-history deploy/helm/autolog -f deploy/helm/autolog/values-stage.yaml
helm template vehicle-maintenance-history deploy/helm/autolog -f deploy/helm/autolog/values-prod.yaml
```

Install or upgrade stage manually:

```bash
helm upgrade --install vehicle-maintenance-history deploy/helm/autolog \
  --namespace autolog-stage \
  --create-namespace \
  -f deploy/helm/autolog/values-stage.yaml \
  --set-string backend.image.repository='<account-id>.dkr.ecr.us-east-1.amazonaws.com/autolog-backend' \
  --set-string backend.image.tag='<image-tag>' \
  --set-string frontend.image.repository='<account-id>.dkr.ecr.us-east-1.amazonaws.com/autolog-frontend' \
  --set-string frontend.image.tag='<image-tag>' \
  --set-string backend.env.datasourceUrl='<stage-jdbc-url>'
```

Install or upgrade production manually:

```bash
helm upgrade --install vehicle-maintenance-history deploy/helm/autolog \
  --namespace autolog-prod \
  --create-namespace \
  -f deploy/helm/autolog/values-prod.yaml \
  --set-string backend.image.repository='<account-id>.dkr.ecr.us-east-1.amazonaws.com/autolog-backend' \
  --set-string backend.image.tag='<image-tag>' \
  --set-string frontend.image.repository='<account-id>.dkr.ecr.us-east-1.amazonaws.com/autolog-frontend' \
  --set-string frontend.image.tag='<image-tag>' \
  --set-string backend.env.datasourceUrl='<prod-jdbc-url>'
```

## CI/CD flow

Pull requests run:

- backend tests: `./mvnw test`
- frontend build: `npm run build`
- Docker Compose validation
- backend Docker image build
- frontend Docker image build
- Helm lint and template rendering for base, stage and prod values

Merges to `main`:

- build backend and frontend images
- push both images to Amazon ECR
- deploy automatically to `autolog-stage`

Production:

- run the `Deploy production` workflow manually
- provide the image tag, ideally the commit SHA already deployed to stage
- approve the `production` GitHub Environment when prompted

## DNS and TLS notes

The chart currently prepares HTTP ingress hosts.

Before public production use, add:

- Route 53 records pointing the four hosts to the load balancer.
- ACM certificates for `stage.autolog.com.br`, `api-stage.autolog.com.br`, `www.autolog.com.br` and `api.autolog.com.br`.
- HTTPS listener annotations or TLS configuration in `values-stage.yaml` and `values-prod.yaml`.

This is intentionally left as a follow-up because the repository must not create real AWS resources yet.

## Terraform foundation

The repository includes a first Terraform foundation in `infra/terraform`.

The initial Terraform scope is intentionally small and reviewable:

- AWS provider configured for `us-east-1`.
- ECR repositories:
  - `autolog-backend`
  - `autolog-frontend`
- ECR image scanning and lifecycle policies.
- Optional GitHub Actions OIDC/IAM deploy role, disabled by default.
- S3 remote state backend scaffold, with backend settings supplied outside committed Terraform.
- Optional VPC foundation, disabled by default.
- Optional RDS PostgreSQL foundation, disabled by default.
- Optional EKS foundation, disabled by default.
- Documented placeholders for future EKS access hardening, add-ons, DNS, TLS, private subnet egress and production database hardening.

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

Remote Terraform state is scaffolded but not bootstrapped. Use `init -backend=false` for local validation until the S3 state bucket and DynamoDB lock table are reviewed and created.

See `docs/terraform-state.md` for the planned backend resources and initialization commands.
See `docs/aws-bootstrap-checklist.md` for the manual AWS and GitHub setup checklist.
See `docs/aws-activation-runbook.md` for the staged activation sequence.

The pull request checks run Terraform formatting and validation without AWS credentials:

```bash
terraform -chdir=infra/terraform fmt -check -recursive
terraform -chdir=infra/terraform init -backend=false
terraform -chdir=infra/terraform validate
```

The manual `Terraform plan` workflow stays skipped until remote state and `AWS_TERRAFORM_ROLE_ARN` are configured. It runs `plan` only; it does not run `apply`.

VPC creation is controlled by `enable_vpc` and remains disabled by default. Confirm the AWS account, CIDR ranges, availability zones and networking cost boundaries before enabling it.

RDS creation is controlled by `enable_rds` and remains disabled by default. It requires `enable_vpc = true` and should only be enabled after reviewing database cost, backup retention, deletion protection and stage/production topology.

EKS creation is controlled by `enable_eks` and remains disabled by default. It requires `enable_vpc = true` and can create the base cluster, IAM roles, a default managed node group, optional managed add-ons, optional access entries and an optional cluster OIDC provider for IRSA. AWS Load Balancer Controller, DNS and TLS remain follow-up work.
