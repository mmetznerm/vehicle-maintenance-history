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
