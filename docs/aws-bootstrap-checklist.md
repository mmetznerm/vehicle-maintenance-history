# AWS bootstrap checklist

This checklist captures the manual setup needed before Terraform can safely manage the AWS foundation for Vehicle Maintenance History.

`AutoLog` is used for AWS-facing deployment naming, while the official application name remains `vehicle-maintenance-history`.

Do not create these resources until the target AWS account and cost boundaries are confirmed.

## Information to confirm

- AWS account ID.
- AWS region: `us-east-1`.
- Operators who should have Terraform access.
- Whether Terraform will be run only from GitHub Actions or also from local machines.
- VPC CIDR ranges and availability zones if `enable_vpc` will be turned on.
- RDS PostgreSQL size, backup retention and environment strategy if `enable_rds` will be turned on.
- EKS Kubernetes version, endpoint access model, node sizing and cluster access model if `enable_eks` will be turned on.

## Remote state resources

Create an S3 bucket for Terraform state:

```text
autolog-terraform-state-<aws-account-id>
```

Recommended bucket settings:

- Block all public access.
- Enable bucket versioning.
- Enable server-side encryption.
- Deny non-TLS requests with a bucket policy.
- Restrict access to Terraform operators and the Terraform GitHub Actions role.

Create a DynamoDB table for Terraform state locking:

```text
autolog-terraform-locks
```

Recommended table settings:

- Partition key: `LockID`
- Partition key type: string
- Billing mode: on-demand

## Terraform GitHub Actions role

Create or bootstrap a dedicated IAM role for Terraform plans:

```text
autolog-github-actions-terraform
```

Trust policy requirements:

- Federated principal: GitHub Actions OIDC provider for `token.actions.githubusercontent.com`.
- Audience: `sts.amazonaws.com`.
- Subject limited to this repository and reviewed branches/environments.

Start with the least permissions required for the current Terraform scope:

- Read/write access to the Terraform state bucket path.
- Read/write access to the DynamoDB lock table.
- Read/plan permissions for the AWS resources in `infra/terraform`.

Expand permissions in reviewed PRs when VPC, EKS, RDS, DNS or TLS resources are added.

## GitHub repository variables

After the AWS resources and Terraform role exist, configure these GitHub repository variables:

```text
AWS_TERRAFORM_ROLE_ARN=<terraform-role-arn>
TF_STATE_BUCKET=autolog-terraform-state-<aws-account-id>
TF_STATE_DYNAMODB_TABLE=autolog-terraform-locks
```

The `Terraform plan` workflow will remain skipped until all three variables are present.

## Current application deployment variables

These are still required later for application deployment workflows:

```text
AWS_GITHUB_ACTIONS_ROLE_ARN
EKS_CLUSTER_NAME
STAGE_SPRING_DATASOURCE_URL
PROD_SPRING_DATASOURCE_URL
```

Do not configure fake values. The stage deployment workflow intentionally skips itself until real values exist.

## VPC activation notes

Terraform includes an optional VPC foundation behind:

```text
enable_vpc=false
```

Before changing it to `true`, confirm:

- VPC CIDR block, currently defaulted to `10.40.0.0/16`.
- Public subnet CIDRs, currently defaulted to `10.40.0.0/24` and `10.40.1.0/24`.
- Private subnet CIDRs, currently defaulted to `10.40.10.0/24` and `10.40.11.0/24`.
- Availability zones for the AWS account.
- Whether private subnet egress will use NAT gateways, VPC endpoints, or a different pattern.

NAT gateways are intentionally not part of the current Terraform files because they have recurring cost and should be approved separately.

## RDS activation notes

Terraform includes an optional RDS PostgreSQL foundation behind:

```text
enable_rds=false
```

RDS creation also requires:

```text
enable_vpc=true
```

Before changing it to `true`, confirm:

- Whether stage and production use separate Terraform stacks, separate RDS instances, or separate databases.
- PostgreSQL engine version, or whether to leave the Terraform variable null and use the AWS default at creation time.
- Instance class, currently defaulted to `db.t4g.micro`.
- Storage, currently defaulted to 20 GiB with autoscaling up to 100 GiB.
- Backup retention, currently defaulted to 7 days.
- Whether deletion protection should remain enabled.
- How application credentials will be passed from AWS Secrets Manager or RDS output into Kubernetes secrets.

The current RDS foundation uses AWS-managed master password storage and does not commit database passwords.

## EKS activation notes

Terraform includes an optional EKS foundation behind:

```text
enable_eks=false
```

EKS creation also requires:

```text
enable_vpc=true
```

Before changing it to `true`, confirm:

- Kubernetes version, or whether to leave the Terraform variable null and use the AWS default at creation time.
- Whether the cluster API endpoint should remain publicly reachable during bootstrap.
- Default node group instance type, currently defaulted to `t3.small`.
- Desired/min/max node counts, currently defaulted to 2/1/3.
- IAM principal ARNs for `eks_admin_principal_arns` and `eks_deploy_principal_arns`, if access entries should be managed by Terraform.
- Managed add-on names for `eks_addons`, if add-ons should be managed by Terraform.
- Whether to create the EKS IAM OIDC provider now for future IRSA integrations.
- Whether AWS Load Balancer Controller should be added in the same PR or a follow-up PR.
- How the application deploy role will get Kubernetes access.

The current EKS foundation does not install Helm charts, DNS records or TLS certificates. Kubernetes add-ons are installed only if `eks_addons` is populated.
