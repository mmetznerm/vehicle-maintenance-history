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

output "kubernetes_namespaces" {
  description = "Expected Kubernetes namespaces used by Helm values."
  value = {
    stage = local.stage_namespace
    prod  = local.prod_namespace
  }
}

output "public_hosts" {
  description = "Expected public hosts used by Helm values and future DNS/TLS work."
  value = {
    stage = local.stage_hosts
    prod  = local.prod_hosts
  }
}

output "vpc" {
  description = "VPC details when enable_vpc is true."
  value = local.create_vpc ? {
    id                   = aws_vpc.main[0].id
    cidr_block           = aws_vpc.main[0].cidr_block
    public_subnet_ids    = aws_subnet.public[*].id
    private_subnet_ids   = aws_subnet.private[*].id
    availability_zones   = var.availability_zones
    public_subnet_cidrs  = var.public_subnet_cidrs
    private_subnet_cidrs = var.private_subnet_cidrs
  } : null
}

output "rds" {
  description = "RDS PostgreSQL details when enable_rds and enable_vpc are true."
  value = local.create_rds ? {
    identifier = aws_db_instance.postgres[0].identifier
    address    = aws_db_instance.postgres[0].address
    endpoint   = aws_db_instance.postgres[0].endpoint
    port       = aws_db_instance.postgres[0].port
    db_name    = aws_db_instance.postgres[0].db_name
  } : null
}

output "eks" {
  description = "EKS cluster details when enable_eks and enable_vpc are true."
  value = local.create_eks ? {
    cluster_name    = aws_eks_cluster.main[0].name
    cluster_arn     = aws_eks_cluster.main[0].arn
    endpoint          = aws_eks_cluster.main[0].endpoint
    version           = aws_eks_cluster.main[0].version
    node_group_name   = aws_eks_node_group.default[0].node_group_name
    addons            = var.eks_addons
    oidc_provider_arn = try(aws_iam_openid_connect_provider.eks[0].arn, null)
  } : null
}
