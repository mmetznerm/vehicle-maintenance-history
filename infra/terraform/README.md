# Terraform AWS foundation

This directory contains the first Terraform foundation for Vehicle Maintenance History.

`AutoLog` is used for public deployment naming, while the official application name remains `vehicle-maintenance-history`.

The initial stack is intentionally small and focused on a public AWS demo environment:

- AWS provider configuration for `us-east-1`.
- TLS provider support for the optional GitHub Actions OIDC provider.
- ECR repositories:
  - `autolog-backend`
  - `autolog-frontend`
- ECR image scanning and lifecycle policies.
- Optional GitHub Actions OIDC/IAM deploy role, disabled by default.
- Documented placeholders for future VPC, EKS, RDS, DNS and TLS work.

Do not run `terraform apply` without a reviewed plan and explicit approval.

## Local commands

From the repository root:

```bash
terraform -chdir=infra/terraform fmt -recursive
terraform -chdir=infra/terraform init -backend=false
terraform -chdir=infra/terraform validate
terraform -chdir=infra/terraform plan
```

Use `init -backend=false` for validation because no remote state backend has been chosen yet.

## What a future apply would create

With the default variables, a future reviewed `terraform apply` would create:

- ECR repository `autolog-backend`.
- ECR repository `autolog-frontend`.
- Lifecycle policies for both repositories.

If `enable_github_actions_oidc = true`, it would also create:

- GitHub Actions OIDC provider for `token.actions.githubusercontent.com`.
- IAM role `autolog-github-actions-deploy`.
- Inline IAM policy allowing image push/read to the two ECR repositories and `eks:DescribeCluster`.

The EKS cluster, RDS PostgreSQL database, VPC, Route 53 records and ACM certificates are not created by this first Terraform foundation.

## Variables

Important defaults:

```text
aws_region = "us-east-1"
app_name = "vehicle-maintenance-history"
deployment_name = "autolog"
github_repository = "mmetznerm/vehicle-maintenance-history"
enable_github_actions_oidc = false
```

Before enabling OIDC, review the generated Terraform plan and confirm the allowed subjects match the `main` branch and the `demo` GitHub Environment.

## State

No remote Terraform state backend is configured yet.

Choose and review a backend before any long-lived AWS usage. A typical follow-up is an S3 backend with DynamoDB locking, created manually once or bootstrapped in a separate reviewed step.

Do not commit local state files, `.tfvars` files or `.terraform/` directories.
