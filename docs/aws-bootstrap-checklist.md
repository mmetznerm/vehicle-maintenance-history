# AWS bootstrap checklist

This checklist captures the manual setup needed before Terraform can safely manage the AWS foundation for Vehicle Maintenance History.

`AutoLog` is used for AWS-facing deployment naming, while the official application name remains `vehicle-maintenance-history`.

Do not create these resources until the target AWS account and cost boundaries are confirmed.

## Information to confirm

- AWS account ID.
- AWS region: `us-east-1`.
- Operators who should have Terraform access.
- Whether Terraform will be run only from GitHub Actions or also from local machines.

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
