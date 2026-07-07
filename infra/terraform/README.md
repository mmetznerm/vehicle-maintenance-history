# Terraform AWS foundation

This directory contains the first Terraform foundation for Vehicle Maintenance History.

`AutoLog` is used for public deployment naming, while the official application name remains `vehicle-maintenance-history`.

The initial stack is intentionally small:

- AWS provider configuration for `us-east-1`.
- TLS provider support for the optional GitHub Actions OIDC provider.
- S3 backend block with example backend configuration.
- Optional VPC foundation, disabled by default.
- Optional RDS PostgreSQL foundation, disabled by default.
- ECR repositories:
  - `autolog-backend`
  - `autolog-frontend`
- ECR image scanning and lifecycle policies.
- Optional GitHub Actions OIDC/IAM deploy role, disabled by default.
- Documented placeholders for future EKS, DNS, TLS, private subnet egress and production database hardening.

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

Use `terraform.tfvars.example` as a starting point for reviewed local variables. Do not commit real `.tfvars` files.

## What a future apply would create

With the default variables, a future reviewed `terraform apply` would create:

- ECR repository `autolog-backend`.
- ECR repository `autolog-frontend`.
- Lifecycle policies for both repositories.

If `enable_github_actions_oidc = true`, it would also create:

- GitHub Actions OIDC provider for `token.actions.githubusercontent.com`.
- IAM role `autolog-github-actions-deploy`.
- Inline IAM policy allowing image push/read to the two ECR repositories and `eks:DescribeCluster`.

If `enable_vpc = true`, it would also create:

- One VPC using `vpc_cidr`.
- Public subnets tagged for Kubernetes external load balancers.
- Private subnets tagged for Kubernetes internal load balancers.
- Internet Gateway and public route table.

If `enable_vpc = true` and `enable_rds = true`, it would also create:

- RDS PostgreSQL subnet group using private subnets.
- RDS security group scoped to the VPC CIDR.
- Private RDS PostgreSQL instance with encrypted storage, deletion protection and AWS-managed master password.

NAT gateways, VPC endpoints, the EKS cluster, Route 53 records, ACM certificates, production RDS sizing and separate stage/prod database topology are not created by this Terraform foundation.

## Variables

Important defaults:

```text
aws_region = "us-east-1"
app_name = "vehicle-maintenance-history"
deployment_name = "autolog"
github_repository = "mmetznerm/vehicle-maintenance-history"
enable_github_actions_oidc = false
enable_vpc = false
vpc_cidr = "10.40.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b"]
public_subnet_cidrs = ["10.40.0.0/24", "10.40.1.0/24"]
private_subnet_cidrs = ["10.40.10.0/24", "10.40.11.0/24"]
enable_rds = false
rds_database_name = "autolog"
rds_master_username = "autolog_admin"
rds_engine_version = null
rds_instance_class = "db.t4g.micro"
```

Before enabling OIDC, review the generated Terraform plan and confirm the allowed subjects match the workflow environments and branches.

Before enabling VPC creation, confirm the target AWS account, final CIDR ranges and availability zones. The number of public/private subnet CIDRs must not exceed the number of availability zones.

Before enabling RDS creation, confirm `enable_vpc = true`, database size, backup retention, instance class, and whether stage and production should be separate stacks or separate databases.

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
