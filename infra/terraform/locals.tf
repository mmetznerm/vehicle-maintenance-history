locals {
  ecr_repository_names = {
    backend  = "autolog-backend"
    frontend = "autolog-frontend"
  }

  stage_namespace = "autolog-stage"
  prod_namespace  = "autolog-prod"

  stage_hosts = {
    frontend = "stage.autolog.com.br"
    api      = "api-stage.autolog.com.br"
  }

  prod_hosts = {
    frontend = "www.autolog.com.br"
    api      = "api.autolog.com.br"
  }

  create_vpc = var.enable_vpc
  create_rds = var.enable_vpc && var.enable_rds

  common_tags = {
    Application             = var.app_name
    Brand                   = var.deployment_name
    Environment             = var.environment
    ManagedBy               = "terraform"
    Repository              = var.github_repository
    TerraformStack          = "vehicle-maintenance-history-aws-foundation"
    OfficialApplicationName = "vehicle-maintenance-history"
  }
}
