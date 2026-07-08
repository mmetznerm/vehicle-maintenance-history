locals {
  ecr_repository_names = {
    backend  = "autolog-backend"
    frontend = "autolog-frontend"
  }

  demo_namespace = "autolog-demo"

  demo_hosts = {
    frontend = "demo.autolog.com.br"
    api      = "api-demo.autolog.com.br"
  }

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
