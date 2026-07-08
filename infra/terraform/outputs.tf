output "aws_region" {
  description = "AWS region configured for this Terraform stack."
  value       = var.aws_region
}

output "ecr_repository_names" {
  description = "Application ECR repository names."
  value       = local.ecr_repository_names
}

output "ecr_repository_urls" {
  description = "Application ECR repository URLs for CI image tags."
  value = {
    backend  = aws_ecr_repository.backend.repository_url
    frontend = aws_ecr_repository.frontend.repository_url
  }
}

output "github_actions_role_arn" {
  description = "GitHub Actions deployment role ARN when OIDC resources are enabled."
  value       = try(aws_iam_role.github_actions_deploy[0].arn, null)
}

output "github_actions_oidc_provider_arn" {
  description = "GitHub Actions OIDC provider ARN when OIDC resources are enabled."
  value       = try(aws_iam_openid_connect_provider.github_actions[0].arn, null)
}

output "kubernetes_namespace" {
  description = "Expected Kubernetes namespace used by the demo Helm values."
  value       = local.demo_namespace
}

output "public_hosts" {
  description = "Expected public hosts used by demo Helm values and future DNS/TLS work."
  value       = local.demo_hosts
}

output "vpc_id" {
  description = "Demo VPC ID."
  value       = aws_vpc.demo.id
}

output "public_subnet_ids" {
  description = "Public subnet IDs for internet-facing load balancers."
  value       = aws_subnet.public[*].id
}

output "private_app_subnet_ids" {
  description = "Private application subnet IDs for EKS workloads."
  value       = aws_subnet.private_app[*].id
}

output "private_database_subnet_ids" {
  description = "Private database subnet IDs for RDS."
  value       = aws_subnet.private_database[*].id
}

output "eks_security_group_ids" {
  description = "Reserved security group IDs for the future EKS PR."
  value = {
    control_plane = aws_security_group.eks_control_plane.id
    nodes         = aws_security_group.eks_nodes.id
  }
}

output "rds_security_group_id" {
  description = "Reserved security group ID for the future RDS PR."
  value       = aws_security_group.rds.id
}

output "eks_cluster_name" {
  description = "Demo EKS cluster name when enabled."
  value       = try(aws_eks_cluster.demo[0].name, null)
}

output "eks_cluster_endpoint" {
  description = "Demo EKS cluster API endpoint when enabled."
  value       = try(aws_eks_cluster.demo[0].endpoint, null)
}

output "eks_cluster_oidc_provider_arn" {
  description = "Demo EKS OIDC provider ARN when enabled."
  value       = try(aws_iam_openid_connect_provider.eks[0].arn, null)
}

output "eks_node_group_name" {
  description = "Demo EKS managed node group name when enabled."
  value       = try(aws_eks_node_group.demo[0].node_group_name, null)
}
