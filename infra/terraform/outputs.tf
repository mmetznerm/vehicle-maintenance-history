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
  value = {
    frontend = var.demo_frontend_host
    backend  = var.demo_backend_host
  }
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
  description = "Security group IDs used by the demo EKS cluster."
  value = {
    control_plane = aws_security_group.eks_control_plane.id
    nodes         = aws_security_group.eks_nodes.id
  }
}

output "rds_security_group_id" {
  description = "Security group ID used by the demo RDS PostgreSQL database."
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

output "aws_load_balancer_controller_role_arn" {
  description = "IAM role ARN used by the AWS Load Balancer Controller service account when enabled."
  value       = try(aws_iam_role.aws_load_balancer_controller[0].arn, null)
}

output "rds_endpoint" {
  description = "Demo RDS PostgreSQL endpoint when enabled."
  value       = try(aws_db_instance.demo[0].address, null)
}

output "rds_datasource_url" {
  description = "JDBC URL for the demo backend when RDS is enabled."
  value       = try("jdbc:postgresql://${aws_db_instance.demo[0].address}:${aws_db_instance.demo[0].port}/${var.rds_database_name}", null)
}

output "rds_master_username" {
  description = "Demo RDS PostgreSQL master username when enabled."
  value       = var.enable_rds_database ? var.rds_master_username : null
}

output "rds_master_password" {
  description = "Generated demo RDS PostgreSQL master password when enabled."
  value       = try(random_password.rds_master[0].result, null)
  sensitive   = true
}

output "route53_zone_id" {
  description = "Route 53 hosted zone ID when the zone is managed by this stack or provided as input."
  value       = local.selected_route53_zone_id
}

output "route53_name_servers" {
  description = "Route 53 name servers to configure at the domain registrar when the hosted zone is managed by this stack."
  value       = try(aws_route53_zone.demo[0].name_servers, null)
}

output "acm_certificate_arn" {
  description = "ACM certificate ARN for the demo hosts when enabled."
  value       = try(aws_acm_certificate.demo[0].arn, null)
}
