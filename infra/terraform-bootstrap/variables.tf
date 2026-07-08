variable "aws_region" {
  description = "AWS region where Terraform state resources will be created."
  type        = string
  default     = "us-east-1"
}

variable "app_name" {
  description = "Official application name used for tags."
  type        = string
  default     = "vehicle-maintenance-history"
}

variable "deployment_name" {
  description = "Public deployment/brand name used for AWS-facing resources."
  type        = string
  default     = "autolog"
}

variable "github_repository" {
  description = "GitHub repository associated with this Terraform state backend, in owner/name format."
  type        = string
  default     = "mmetznerm/vehicle-maintenance-history"
}

variable "state_bucket_name" {
  description = "Globally unique S3 bucket name for Terraform remote state."
  type        = string
}

variable "lock_table_name" {
  description = "DynamoDB table name for Terraform state locking."
  type        = string
  default     = "autolog-terraform-locks"
}
