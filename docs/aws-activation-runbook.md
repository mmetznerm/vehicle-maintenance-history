# AWS activation runbook

This runbook describes the safe order for activating AWS infrastructure for Vehicle Maintenance History.

`AutoLog` is used for AWS-facing deployment naming, while the official application name remains `vehicle-maintenance-history`.

No step in this document should be run as `terraform apply` without a reviewed plan and explicit approval.

## Current default state

The Terraform foundation is intentionally safe by default:

```text
enable_github_actions_oidc = false
enable_vpc = false
enable_rds = false
enable_eks = false
enable_eks_oidc_provider = false
eks_addons = []
eks_admin_principal_arns = []
eks_deploy_principal_arns = []
```

With defaults, the foundation manages only the initial shared resources that are already represented in Terraform, such as ECR repositories. Optional networking, database and Kubernetes resources remain disabled until reviewed.

## Phase 1: Bootstrap state and Terraform role

Manual AWS/GitHub setup required:

- Create S3 state bucket.
- Create DynamoDB lock table.
- Create or bootstrap a dedicated GitHub Actions Terraform role.
- Configure GitHub repository variables:
  - `AWS_TERRAFORM_ROLE_ARN`
  - `TF_STATE_BUCKET`
  - `TF_STATE_DYNAMODB_TABLE`

After this, the manual `Terraform plan` workflow can run plans against remote state.

## Phase 2: First reviewed Terraform plan

Run the manual `Terraform plan` workflow from GitHub Actions.

Review that the plan is limited to the intended resources.

Do not apply from CI. The current workflow only plans.

## Phase 3: Optional VPC activation

Before enabling:

- Confirm AWS account ID.
- Confirm VPC CIDR.
- Confirm public/private subnet CIDRs.
- Confirm availability zones.
- Confirm whether private subnet egress uses NAT gateways, VPC endpoints or another pattern.

Terraform variable changes:

```text
enable_vpc = true
```

Expected resources:

- VPC.
- Public subnets.
- Private subnets.
- Internet Gateway.
- Public route table.

Not included yet:

- NAT gateways.
- VPC endpoints.
- Transit/VPN/direct connectivity.

## Phase 4: Optional RDS activation

Before enabling:

- Confirm stage/production database topology.
- Confirm instance class.
- Confirm storage.
- Confirm backup retention.
- Confirm deletion protection.
- Confirm how application credentials flow into Kubernetes secrets.

Terraform variable changes:

```text
enable_vpc = true
enable_rds = true
```

Expected resources:

- RDS subnet group.
- RDS security group.
- Private RDS PostgreSQL instance.
- AWS-managed master password.

## Phase 5: Optional EKS activation

Before enabling:

- Confirm Kubernetes version.
- Confirm public/private endpoint strategy.
- Confirm node instance type and scaling limits.
- Confirm admin and deploy IAM principal ARNs.
- Confirm add-ons to manage with Terraform.
- Confirm whether IRSA is needed immediately.

Terraform variable changes:

```text
enable_vpc = true
enable_eks = true
```

Optional Terraform variables:

```text
enable_eks_oidc_provider = true
eks_admin_principal_arns = ["<operator-or-admin-role-arn>"]
eks_deploy_principal_arns = ["<github-actions-deploy-role-arn>"]
eks_addons = ["vpc-cni", "coredns", "kube-proxy"]
```

Expected resources:

- EKS cluster.
- Default managed node group.
- EKS IAM roles.
- Optional EKS access entries.
- Optional managed add-ons.
- Optional EKS IAM OIDC provider.

Not included yet:

- AWS Load Balancer Controller.
- External DNS.
- Route 53 records.
- ACM certificates.
- Kubernetes secrets.

## Phase 6: Application deployment wiring

After EKS, RDS and ECR are ready:

- Configure `AWS_GITHUB_ACTIONS_ROLE_ARN`.
- Configure `EKS_CLUSTER_NAME`.
- Configure `STAGE_SPRING_DATASOURCE_URL`.
- Configure `PROD_SPRING_DATASOURCE_URL`.
- Create Kubernetes namespaces and backend secrets.
- Run stage deploy.
- Promote a reviewed image tag to production through the manual production workflow.

## Stop points

Stop and review before:

- Creating any AWS resource.
- Enabling NAT gateways.
- Enabling RDS.
- Enabling EKS.
- Granting EKS admin/deploy access.
- Adding public DNS or TLS.
- Running any `terraform apply`.
