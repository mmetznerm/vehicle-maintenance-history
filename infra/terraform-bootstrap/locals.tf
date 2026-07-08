locals {
  common_tags = {
    Application             = var.app_name
    Brand                   = var.deployment_name
    Environment             = "shared"
    ManagedBy               = "terraform"
    Repository              = var.github_repository
    TerraformStack          = "vehicle-maintenance-history-terraform-bootstrap"
    OfficialApplicationName = "vehicle-maintenance-history"
  }
}
