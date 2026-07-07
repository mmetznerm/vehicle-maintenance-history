# Terraform AWS foundation

This directory contains the first Terraform foundation for Vehicle Maintenance History.

`AutoLog` is used for public deployment naming, while the official application name remains `vehicle-maintenance-history`.

The initial stack is intentionally small:

- AWS provider configuration for `us-east-1`.
- TLS provider support for the optional GitHub Actions OIDC provider.
- S3 backend block with example backend configuration.
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

Use `init -backend=false` for validation because the remote state backend must be bootstrapped before normal initialization.

The example backend config is `backend.example.hcl`. Copy it to an untracked `backend.hcl` only after the state bucket and lock table exist.

## What a future apply would create

With the default variables, a future reviewed `terraform apply` would create:

- ECR repository `autolog-backend`.
- ECR repository `autolog-frontend`.
- Lifecycle policies for both repositories.

If `enable_github_actions_oidc = true`, it would also create:

- GitHub Actions OIDC provider for `token.actions.githubusercontent.com`.
- IAM role `autolog-github-actions-deploy`.
- Inline IAM policy allowing image push/read to the two ECR repositories and `eks:DescribeCluster`.

The EKS cluster, RDS PostgreSQL databases, VPC, Route 53 records and ACM certificates are not created by this first Terraform foundation.

## Variables

Important defaults:

```text
aws_region = "us-east-1"
app_name = "vehicle-maintenance-history"
deployment_name = "autolog"
github_repository = "mmetznerm/vehicle-maintenance-history"
enable_github_actions_oidc = false
```

Before enabling OIDC, review the generated Terraform plan and confirm the allowed subjects match the workflow environments and branches.

## State

The Terraform root declares an S3 backend, but concrete backend settings are intentionally not committed.

Use `backend.example.hcl` as the reviewed template for remote state. The expected backend uses an S3 state bucket with DynamoDB locking in `us-east-1`.

Do not commit local state files, `.tfvars` files or `.terraform/` directories.

See `docs/terraform-state.md` for the full remote state bootstrap and initialization notes.

## GitHub Actions plan workflow

The repository includes a manual `Terraform plan` workflow.

It stays skipped until these GitHub repository variables are configured:

```text
AWS_TERRAFORM_ROLE_ARN
TF_STATE_BUCKET
TF_STATE_DYNAMODB_TABLE
```

The workflow runs `terraform plan` only. It does not run `terraform apply`.
