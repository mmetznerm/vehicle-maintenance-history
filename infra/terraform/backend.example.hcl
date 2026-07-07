bucket         = "autolog-terraform-state-REPLACE_WITH_AWS_ACCOUNT_ID"
key            = "vehicle-maintenance-history/aws-foundation/terraform.tfstate"
region         = "us-east-1"
dynamodb_table = "autolog-terraform-locks"
encrypt        = true
