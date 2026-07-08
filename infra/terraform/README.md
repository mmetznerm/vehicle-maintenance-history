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
- Demo VPC with DNS support.
- Public subnets across two availability zones for internet-facing load balancers.
- Private application subnets across two availability zones for future EKS workloads.
- Isolated private database subnets across two availability zones for future RDS.
- Route tables for public, private application and private database tiers.
- Optional single NAT Gateway for private application egress.
- Reserved security groups for future EKS and RDS resources.
- ECR repositories:
  - `autolog-backend`
  - `autolog-frontend`
- ECR image scanning and lifecycle policies.
- Optional GitHub Actions OIDC/IAM deploy role.

The EKS cluster, RDS PostgreSQL database, Route 53 records and ACM certificates are intentionally left for later reviewed PRs.

## Networking

Default CIDR layout:

| Tier | Purpose | CIDRs |
|---|---|---|
| VPC | Demo network | `10.40.0.0/16` |
| Public | AWS Load Balancer Controller / internet-facing ALB | `10.40.0.0/24`, `10.40.1.0/24` |
| Private app | Future EKS workloads | `10.40.10.0/24`, `10.40.11.0/24` |
| Private database | Future RDS subnet group | `10.40.20.0/24`, `10.40.21.0/24` |

Subnets include Kubernetes discovery tags for the future `autolog-demo` EKS cluster.

NAT Gateway is disabled by default:

```text
enable_nat_gateway = false
```

Enable it only when private EKS nodes need outbound internet access. Keeping it disabled reduces demo cost while the cluster is not deployed.

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
