# Terraform bootstrap

This stack creates the remote state foundation used by the main AWS demo Terraform stack.

It is intentionally separate from `infra/terraform` because Terraform cannot store its own first state file in an S3 backend that does not exist yet.

## Resources

- S3 bucket for Terraform state.
- S3 bucket versioning.
- S3 server-side encryption.
- S3 public access block.
- S3 bucket policy denying insecure transport.
- DynamoDB table for Terraform state locking.

Both the S3 bucket and DynamoDB table use `prevent_destroy` to avoid accidental state loss.

## Usage

Copy the example variables file and choose a globally unique bucket name:

```bash
cp infra/terraform-bootstrap/terraform.tfvars.example infra/terraform-bootstrap/terraform.tfvars
```

Initialize, review and apply:

```bash
terraform -chdir=infra/terraform-bootstrap init
terraform -chdir=infra/terraform-bootstrap plan
terraform -chdir=infra/terraform-bootstrap apply
```

After apply, copy the `backend_config` output values into a local backend config file based on:

```text
infra/terraform/backend.demo.hcl.example
```

Do not commit real `.tfvars`, `.tfstate`, `.terraform/` or local backend config files.
