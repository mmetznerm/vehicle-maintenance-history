# Terraform remote state

This document describes the planned remote state setup for the Vehicle Maintenance History AWS Terraform foundation.

`AutoLog` is used for AWS-facing deployment naming, while the official application name remains `vehicle-maintenance-history`.

No AWS resources are created by this document. Do not run `terraform apply` or migrate state without an explicit reviewed plan.

## Target state backend

The Terraform root in `infra/terraform` includes an S3 backend block with settings supplied through a backend config file.

Proposed backend resources:

- S3 bucket: `autolog-terraform-state-<aws-account-id>`
- S3 object key: `vehicle-maintenance-history/aws-foundation/terraform.tfstate`
- AWS region: `us-east-1`
- DynamoDB lock table: `autolog-terraform-locks`

The example backend config is stored in:

```text
infra/terraform/backend.example.hcl
```

Copy it to a local, untracked file before real use:

```bash
cp infra/terraform/backend.example.hcl infra/terraform/backend.hcl
```

Then replace `REPLACE_WITH_AWS_ACCOUNT_ID` with the target AWS account ID.

`backend.hcl` must not be committed if it contains account-specific values.

## GitHub Actions variables

The manual `Terraform plan` workflow is intentionally skipped until these GitHub repository variables exist:

```text
AWS_TERRAFORM_ROLE_ARN
TF_STATE_BUCKET
TF_STATE_DYNAMODB_TABLE
```

Recommended values:

```text
TF_STATE_BUCKET=autolog-terraform-state-<aws-account-id>
TF_STATE_DYNAMODB_TABLE=autolog-terraform-locks
```

Use a dedicated Terraform role for `AWS_TERRAFORM_ROLE_ARN`. Do not reuse the application deploy role unless its permissions are intentionally expanded and reviewed.

## Bootstrap resources

Create the backend resources only after review and explicit approval.

The state bucket should use:

- S3 Block Public Access enabled.
- Versioning enabled.
- Server-side encryption enabled.
- A bucket policy that denies insecure transport.
- Access limited to the operators and CI roles that manage Terraform.

The DynamoDB lock table should use:

- Table name `autolog-terraform-locks`.
- Partition key `LockID` as a string.
- On-demand billing unless a different capacity model is approved.

The Terraform role should be trusted by GitHub Actions OIDC for this repository and should have only the permissions required to manage the reviewed Terraform scope. Start narrow and expand per PR as infrastructure grows.

See `docs/aws-bootstrap-checklist.md` for the manual console checklist that must happen before the workflow can use remote state.

## Validation without remote state

Pull requests should keep using backend-disabled validation:

```bash
terraform -chdir=infra/terraform fmt -check -recursive
terraform -chdir=infra/terraform init -backend=false
terraform -chdir=infra/terraform validate
```

This does not require AWS credentials and does not contact the remote backend.

The manual GitHub Actions workflow uses remote state only after the required repository variables are configured.

## Initialization with remote state

After the S3 bucket and DynamoDB table exist, initialize Terraform with:

```bash
terraform -chdir=infra/terraform init -backend-config=backend.hcl
```

If local state already exists and must be moved to the remote backend, use:

```bash
terraform -chdir=infra/terraform init -backend-config=backend.hcl -migrate-state
```

Review the prompt carefully before accepting a migration.

## Planning

After initialization, preview changes with:

```bash
terraform -chdir=infra/terraform plan
```

Do not run:

```bash
terraform -chdir=infra/terraform apply
```

unless the plan has been reviewed and approved.
