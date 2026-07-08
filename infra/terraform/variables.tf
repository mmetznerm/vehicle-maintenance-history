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
  description = "Logical Terraform environment label for tags. This foundation stack supports the public demo environment."
  type        = string
  default     = "shared"

  validation {
    condition     = contains(["shared", "demo"], var.environment)
    error_message = "environment must be one of: shared, demo."
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
    "repo:mmetznerm/vehicle-maintenance-history:environment:demo"
  ]
}

variable "vpc_cidr" {
  description = "CIDR block for the demo VPC."
  type        = string
  default     = "10.40.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets used by internet-facing load balancers."
  type        = list(string)
  default     = ["10.40.0.0/24", "10.40.1.0/24"]

  validation {
    condition     = length(var.public_subnet_cidrs) >= 2
    error_message = "public_subnet_cidrs must contain at least two CIDR blocks."
  }
}

variable "private_app_subnet_cidrs" {
  description = "CIDR blocks for private application subnets used by EKS workloads."
  type        = list(string)
  default     = ["10.40.10.0/24", "10.40.11.0/24"]

  validation {
    condition     = length(var.private_app_subnet_cidrs) >= 2
    error_message = "private_app_subnet_cidrs must contain at least two CIDR blocks."
  }
}

variable "private_database_subnet_cidrs" {
  description = "CIDR blocks for isolated private database subnets used by RDS."
  type        = list(string)
  default     = ["10.40.20.0/24", "10.40.21.0/24"]

  validation {
    condition     = length(var.private_database_subnet_cidrs) >= 2
    error_message = "private_database_subnet_cidrs must contain at least two CIDR blocks."
  }
}

variable "enable_nat_gateway" {
  description = "Create a single NAT Gateway for private app subnet outbound internet access. Enable for private EKS nodes that need internet egress."
  type        = bool
  default     = false
}

variable "eks_cluster_name" {
  description = "Expected future EKS cluster name, used only for subnet discovery tags in this networking PR."
  type        = string
  default     = "autolog-demo"
}
