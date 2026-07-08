# Terraform AWS foundation

This directory contains the main Terraform foundation for the Vehicle Maintenance History public AWS demo environment.

`AutoLog` is used for public deployment naming, while the official application name remains `vehicle-maintenance-history`.

## Remote state

This stack is configured to use an S3 backend:

```hcl
terraform {
  backend "s3" {}
}
```

Create the remote state resources first with:

```text
infra/terraform-bootstrap
```

Then create a local backend config file from:

```text
infra/terraform/backend.demo.hcl.example
```

Example initialization:

```bash
terraform -chdir=infra/terraform init -backend-config=backend.demo.hcl
```

Do not commit real backend config files, `.tfvars`, local state files or `.terraform/` directories.

## Scope

The current stack creates:

- AWS provider configuration for `us-east-1`.
- ECR repositories:
  - `autolog-backend`
  - `autolog-frontend`
- ECR image scanning and lifecycle policies.
- Optional GitHub Actions OIDC/IAM deploy role.

The EKS cluster, RDS PostgreSQL database, VPC, Route 53 records and ACM certificates are intentionally left for later reviewed PRs.

## Local validation

From the repository root:

```bash
terraform -chdir=infra/terraform fmt -recursive
terraform -chdir=infra/terraform init -backend=false
terraform -chdir=infra/terraform validate
```

Use `init -backend=false` for local validation when the remote backend has not been initialized on the machine.

## Planning and apply

Copy the example variables file and review values:

```bash
cp infra/terraform/foundation.demo.tfvars.example infra/terraform/foundation.demo.tfvars
```

Initialize with the backend config:

```bash
terraform -chdir=infra/terraform init -backend-config=backend.demo.hcl
```

Preview the AWS changes:

```bash
terraform -chdir=infra/terraform plan -var-file=foundation.demo.tfvars
```

Apply only after reviewing the plan:

```bash
terraform -chdir=infra/terraform apply -var-file=foundation.demo.tfvars
```

## GitHub OIDC

The example demo variables enable GitHub Actions OIDC:

```text
enable_github_actions_oidc = true
```

Before applying, confirm:

- `github_repository` matches the real GitHub repository.
- The allowed OIDC subjects match the `main` branch and `demo` GitHub Environment.
- The target AWS account does not already manage the GitHub OIDC provider in another Terraform stack.

The deploy role currently allows ECR image push/read and `eks:DescribeCluster`. Kubernetes access is completed in the future EKS PR through EKS access entries or the cluster auth model chosen there.
