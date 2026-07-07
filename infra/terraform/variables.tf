variable "aws_region" {
  description = "AWS region where shared deployment resources will be created."
  type        = string
  default     = "us-east-1"
}

variable "app_name" {
  description = "Official application name used for tags and documentation."
  type        = string
  default     = "vehicle-maintenance-history"
}

variable "deployment_name" {
  description = "Public deployment/brand name used for AWS-facing resources."
  type        = string
  default     = "autolog"
}

variable "environment" {
  description = "Logical Terraform environment label for tags. This foundation stack is shared by stage and production."
  type        = string
  default     = "shared"

  validation {
    condition     = contains(["shared", "stage", "prod"], var.environment)
    error_message = "environment must be one of: shared, stage, prod."
  }
}

variable "github_repository" {
  description = "GitHub repository allowed to assume the deployment role through OIDC, in owner/name format."
  type        = string
  default     = "mmetznerm/vehicle-maintenance-history"
}

variable "enable_github_actions_oidc" {
  description = "Set to true only when you are ready for Terraform to create the GitHub Actions OIDC provider, IAM role and deploy policy."
  type        = bool
  default     = false
}

variable "github_actions_role_name" {
  description = "IAM role name to be assumed by GitHub Actions when OIDC is enabled."
  type        = string
  default     = "autolog-github-actions-deploy"
}

variable "github_actions_allowed_subjects" {
  description = "Allowed GitHub OIDC subject claims for deployment workflows."
  type        = list(string)
  default = [
    "repo:mmetznerm/vehicle-maintenance-history:ref:refs/heads/main",
    "repo:mmetznerm/vehicle-maintenance-history:environment:stage",
    "repo:mmetznerm/vehicle-maintenance-history:environment:production"
  ]
}

variable "enable_vpc" {
  description = "Set to true only after the AWS account, CIDR ranges and network cost boundaries are reviewed."
  type        = bool
  default     = false
}

variable "vpc_cidr" {
  description = "CIDR block for the application VPC when enable_vpc is true."
  type        = string
  default     = "10.40.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones to use for public and private subnets when enable_vpc is true."
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "public_subnet_cidrs" {
  description = "Public subnet CIDR blocks for load balancers and public AWS resources when enable_vpc is true."
  type        = list(string)
  default     = ["10.40.0.0/24", "10.40.1.0/24"]
}

variable "private_subnet_cidrs" {
  description = "Private subnet CIDR blocks for EKS workloads and RDS when enable_vpc is true."
  type        = list(string)
  default     = ["10.40.10.0/24", "10.40.11.0/24"]
}

variable "enable_rds" {
  description = "Set to true only after VPC networking, database size and cost boundaries are reviewed. Requires enable_vpc=true."
  type        = bool
  default     = false
}

variable "rds_database_name" {
  description = "Initial PostgreSQL database name."
  type        = string
  default     = "autolog"
}

variable "rds_master_username" {
  description = "RDS master username. The password is managed by AWS Secrets Manager when RDS is enabled."
  type        = string
  default     = "autolog_admin"
}

variable "rds_engine_version" {
  description = "PostgreSQL engine version for RDS. Leave null to let AWS choose the current default for PostgreSQL."
  type        = string
  default     = null
}

variable "rds_instance_class" {
  description = "RDS instance class. Review cost before enabling RDS."
  type        = string
  default     = "db.t4g.micro"
}

variable "rds_allocated_storage" {
  description = "Initial RDS allocated storage in GiB."
  type        = number
  default     = 20
}

variable "rds_max_allocated_storage" {
  description = "Maximum RDS autoscaled storage in GiB."
  type        = number
  default     = 100
}

variable "rds_backup_retention_period" {
  description = "RDS automated backup retention period in days."
  type        = number
  default     = 7
}
