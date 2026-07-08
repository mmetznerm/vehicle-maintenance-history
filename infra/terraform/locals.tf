locals {
  ecr_repository_names = {
    backend  = "autolog-backend"
    frontend = "autolog-frontend"
  }

  availability_zone_count = 2

  demo_namespace = "autolog-demo"

  common_tags = {
    Application             = var.app_name
    Brand                   = var.deployment_name
    Environment             = var.environment
    ManagedBy               = "terraform"
    Repository              = var.github_repository
    TerraformStack          = "vehicle-maintenance-history-aws-foundation"
    OfficialApplicationName = "vehicle-maintenance-history"
  }

  public_subnet_tags = {
    "kubernetes.io/role/elb"                        = "1"
    "kubernetes.io/cluster/${var.eks_cluster_name}" = "shared"
    "elbv2.k8s.aws/cluster"                         = var.eks_cluster_name
  }

  private_app_subnet_tags = {
    "kubernetes.io/role/internal-elb"               = "1"
    "kubernetes.io/cluster/${var.eks_cluster_name}" = "shared"
    "elbv2.k8s.aws/cluster"                         = var.eks_cluster_name
  }
}
