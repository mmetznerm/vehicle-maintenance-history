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
- EKS demo cluster, managed node group and basic managed add-ons.
- EKS OIDC provider for Kubernetes service account IAM roles.
- AWS Load Balancer Controller IAM role and policy for IRSA.
- GitHub Actions EKS access entry when GitHub OIDC is enabled.
- RDS PostgreSQL demo database in isolated private database subnets.
- Optional Route 53 hosted zone and ACM certificate for the demo hosts.
- ECR repositories:
  - `autolog-backend`
  - `autolog-frontend`
- ECR image scanning and lifecycle policies.
- Optional GitHub Actions OIDC/IAM deploy role.

The stack stays demo-focused. Production promotion, high availability hardening and advanced observability are intentionally outside this portfolio environment.

## Networking

Default CIDR layout:

| Tier | Purpose | CIDRs |
|---|---|---|
| VPC | Demo network | `10.40.0.0/16` |
| Public | AWS Load Balancer Controller / internet-facing ALB | `10.40.0.0/24`, `10.40.1.0/24` |
| Private app | EKS workloads | `10.40.10.0/24`, `10.40.11.0/24` |
| Private database | RDS subnet group | `10.40.20.0/24`, `10.40.21.0/24` |

Subnets include Kubernetes discovery tags for the `autolog-demo` EKS cluster.

NAT Gateway is disabled by default:

```text
enable_nat_gateway = false
```

Enable it only when private EKS nodes need outbound internet access. Keeping it disabled reduces demo cost while the cluster is not deployed.

## EKS

The demo cluster is controlled by:

```text
enable_eks_cluster = true
```

The example variables file enables EKS with a small managed node group:

```text
eks_node_instance_types = ["t3.small"]
eks_node_desired_size   = 1
eks_node_min_size       = 1
eks_node_max_size       = 2
eks_node_subnet_tier    = "private_app"
```

Because the example uses private nodes, it also enables:

```text
enable_nat_gateway = true
```

This lets nodes pull container images from ECR and reach AWS APIs. To experiment with lower networking cost, set `eks_node_subnet_tier = "public"` and review the security trade-off before applying.

GitHub Actions receives cluster admin access through the EKS access API when `enable_github_actions_oidc = true`. This is intentionally broad for the initial demo deployment path and should be narrowed in a later hardening PR.

The AWS Load Balancer Controller uses IRSA when:

```text
enable_aws_load_balancer_controller_irsa = true
```

After applying Terraform, store `aws_load_balancer_controller_role_arn` as the GitHub variable `AWS_LOAD_BALANCER_CONTROLLER_ROLE_ARN`.

## RDS

The demo PostgreSQL database is controlled by:

```text
enable_rds_database = true
```

Terraform creates the database in isolated private database subnets and only allows PostgreSQL traffic from the EKS node security group.

After applying Terraform, use these outputs for the GitHub demo environment:

```bash
terraform -chdir=infra/terraform output -raw rds_datasource_url
terraform -chdir=infra/terraform output -raw rds_master_username
terraform -chdir=infra/terraform output -raw rds_master_password
```

Store the datasource URL as `DEMO_SPRING_DATASOURCE_URL`, the username as `DEMO_SPRING_DATASOURCE_USERNAME` and the password as `DEMO_SPRING_DATASOURCE_PASSWORD`.

## DNS and TLS

DNS and TLS are optional until the domain is purchased:

```text
enable_route53_zone    = false
enable_acm_certificate = false
```

When the domain is ready, enable Route 53 and ACM or provide an existing hosted zone ID:

```text
enable_route53_zone    = true
enable_acm_certificate = true
demo_root_domain       = "autolog.com.br"
demo_frontend_host     = "demo.autolog.com.br"
demo_backend_host      = "api-demo.autolog.com.br"
```

If Terraform creates the hosted zone, configure the registrar with the `route53_name_servers` output. Store `acm_certificate_arn` as the optional GitHub variable `DEMO_ACM_CERTIFICATE_ARN` to make the ALB Ingress serve HTTPS.

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

The deploy role allows ECR image push/read and `eks:DescribeCluster`. Kubernetes authorization is granted by the EKS access entry created for the GitHub Actions deploy role.
