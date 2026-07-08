locals {
  eks_node_subnet_ids = var.eks_node_subnet_tier == "public" ? aws_subnet.public[*].id : aws_subnet.private_app[*].id
}

resource "aws_iam_role" "eks_cluster" {
  count = var.enable_eks_cluster ? 1 : 0

  name = "${var.deployment_name}-${var.environment}-eks-cluster"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "eks.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eks_cluster" {
  count = var.enable_eks_cluster ? 1 : 0

  role       = aws_iam_role.eks_cluster[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_iam_role" "eks_nodes" {
  count = var.enable_eks_cluster ? 1 : 0

  name = "${var.deployment_name}-${var.environment}-eks-nodes"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eks_nodes_worker" {
  count = var.enable_eks_cluster ? 1 : 0

  role       = aws_iam_role.eks_nodes[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}

resource "aws_iam_role_policy_attachment" "eks_nodes_cni" {
  count = var.enable_eks_cluster ? 1 : 0

  role       = aws_iam_role.eks_nodes[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

resource "aws_iam_role_policy_attachment" "eks_nodes_ecr" {
  count = var.enable_eks_cluster ? 1 : 0

  role       = aws_iam_role.eks_nodes[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_launch_template" "eks_nodes" {
  count = var.enable_eks_cluster ? 1 : 0

  name_prefix            = "${var.deployment_name}-${var.environment}-eks-nodes-"
  update_default_version = true
  vpc_security_group_ids = [aws_security_group.eks_nodes.id]

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      encrypted   = true
      volume_size = var.eks_node_disk_size
      volume_type = "gp3"
    }
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "${var.deployment_name}-${var.environment}-eks-node"
    }
  }
}

resource "aws_eks_cluster" "demo" {
  count = var.enable_eks_cluster ? 1 : 0

  name     = var.eks_cluster_name
  role_arn = aws_iam_role.eks_cluster[0].arn
  version  = var.eks_kubernetes_version

  vpc_config {
    endpoint_private_access = var.eks_endpoint_private_access
    endpoint_public_access  = var.eks_endpoint_public_access
    public_access_cidrs     = var.eks_public_access_cidrs
    security_group_ids      = [aws_security_group.eks_control_plane.id]
    subnet_ids              = aws_subnet.private_app[*].id
  }

  depends_on = [
    aws_iam_role_policy_attachment.eks_cluster
  ]
}

resource "aws_eks_node_group" "demo" {
  count = var.enable_eks_cluster ? 1 : 0

  cluster_name    = aws_eks_cluster.demo[0].name
  node_group_name = "${var.deployment_name}-${var.environment}-default"
  node_role_arn   = aws_iam_role.eks_nodes[0].arn
  subnet_ids      = local.eks_node_subnet_ids
  capacity_type   = var.eks_node_capacity_type
  instance_types  = var.eks_node_instance_types
  version         = var.eks_kubernetes_version

  launch_template {
    id      = aws_launch_template.eks_nodes[0].id
    version = aws_launch_template.eks_nodes[0].latest_version
  }

  scaling_config {
    desired_size = var.eks_node_desired_size
    max_size     = var.eks_node_max_size
    min_size     = var.eks_node_min_size
  }

  update_config {
    max_unavailable = 1
  }

  depends_on = [
    aws_iam_role_policy_attachment.eks_nodes_worker,
    aws_iam_role_policy_attachment.eks_nodes_cni,
    aws_iam_role_policy_attachment.eks_nodes_ecr
  ]
}

resource "aws_eks_addon" "demo" {
  for_each = toset(var.enable_eks_cluster ? var.eks_addon_names : [])

  cluster_name                = aws_eks_cluster.demo[0].name
  addon_name                  = each.key
  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"

  depends_on = [aws_eks_node_group.demo]
}

data "tls_certificate" "eks" {
  count = var.enable_eks_cluster ? 1 : 0

  url = aws_eks_cluster.demo[0].identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "eks" {
  count = var.enable_eks_cluster ? 1 : 0

  url             = aws_eks_cluster.demo[0].identity[0].oidc[0].issuer
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks[0].certificates[0].sha1_fingerprint]
}

resource "aws_eks_access_entry" "github_actions" {
  count = var.enable_eks_cluster && var.enable_github_actions_oidc ? 1 : 0

  cluster_name  = aws_eks_cluster.demo[0].name
  principal_arn = aws_iam_role.github_actions_deploy[0].arn
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "github_actions_admin" {
  count = var.enable_eks_cluster && var.enable_github_actions_oidc ? 1 : 0

  cluster_name  = aws_eks_cluster.demo[0].name
  principal_arn = aws_iam_role.github_actions_deploy[0].arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }

  depends_on = [aws_eks_access_entry.github_actions]
}
