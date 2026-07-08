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
  description = "EKS cluster name used by Terraform resources and Kubernetes subnet discovery tags."
  type        = string
  default     = "autolog-demo"
}

variable "enable_eks_cluster" {
  description = "Create the demo EKS cluster and managed node group."
  type        = bool
  default     = false
}

variable "eks_kubernetes_version" {
  description = "Kubernetes version for the demo EKS cluster."
  type        = string
  default     = "1.36"
}

variable "eks_endpoint_public_access" {
  description = "Enable public API endpoint access for the demo EKS cluster."
  type        = bool
  default     = true
}

variable "eks_endpoint_private_access" {
  description = "Enable private API endpoint access inside the VPC."
  type        = bool
  default     = true
}

variable "eks_public_access_cidrs" {
  description = "CIDR blocks allowed to reach the public EKS API endpoint."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "eks_node_subnet_tier" {
  description = "Subnet tier used by the managed node group. Use private_app for the production-like demo path or public to avoid NAT cost in experiments."
  type        = string
  default     = "private_app"

  validation {
    condition     = contains(["private_app", "public"], var.eks_node_subnet_tier)
    error_message = "eks_node_subnet_tier must be one of: private_app, public."
  }
}

variable "eks_node_instance_types" {
  description = "Instance types used by the demo EKS managed node group."
  type        = list(string)
  default     = ["t3.small"]
}

variable "eks_node_capacity_type" {
  description = "Capacity type for the demo EKS managed node group."
  type        = string
  default     = "ON_DEMAND"

  validation {
    condition     = contains(["ON_DEMAND", "SPOT"], var.eks_node_capacity_type)
    error_message = "eks_node_capacity_type must be ON_DEMAND or SPOT."
  }
}

variable "eks_node_desired_size" {
  description = "Desired number of nodes in the demo EKS managed node group."
  type        = number
  default     = 1
}

variable "eks_node_min_size" {
  description = "Minimum number of nodes in the demo EKS managed node group."
  type        = number
  default     = 1
}

variable "eks_node_max_size" {
  description = "Maximum number of nodes in the demo EKS managed node group."
  type        = number
  default     = 2
}

variable "eks_node_disk_size" {
  description = "Disk size in GiB for demo EKS managed node group instances."
  type        = number
  default     = 30
}

variable "eks_addon_names" {
  description = "EKS add-ons installed after the cluster is created."
  type        = list(string)
  default = [
    "vpc-cni",
    "kube-proxy",
    "coredns",
    "eks-pod-identity-agent"
  ]
}
