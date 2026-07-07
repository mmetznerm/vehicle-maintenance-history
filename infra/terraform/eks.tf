data "aws_iam_policy_document" "eks_cluster_assume_role" {
  count = local.create_eks ? 1 : 0

  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["eks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "eks_cluster" {
  count = local.create_eks ? 1 : 0

  name               = "${var.deployment_name}-${var.environment}-eks-cluster"
  assume_role_policy = data.aws_iam_policy_document.eks_cluster_assume_role[0].json

  tags = merge(local.common_tags, {
    Name = "${var.deployment_name}-${var.environment}-eks-cluster"
  })
}

resource "aws_iam_role_policy_attachment" "eks_cluster_policy" {
  count = local.create_eks ? 1 : 0

  role       = aws_iam_role.eks_cluster[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_eks_cluster" "main" {
  count = local.create_eks ? 1 : 0

  name     = var.eks_cluster_name
  role_arn = aws_iam_role.eks_cluster[0].arn
  version  = var.eks_cluster_version

  vpc_config {
    subnet_ids              = concat(aws_subnet.private[*].id, aws_subnet.public[*].id)
    endpoint_private_access = var.eks_endpoint_private_access
    endpoint_public_access  = var.eks_endpoint_public_access
  }

  tags = merge(local.common_tags, {
    Name = var.eks_cluster_name
  })

  depends_on = [
    aws_iam_role_policy_attachment.eks_cluster_policy
  ]
}

data "aws_iam_policy_document" "eks_node_assume_role" {
  count = local.create_eks ? 1 : 0

  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "eks_node" {
  count = local.create_eks ? 1 : 0

  name               = "${var.deployment_name}-${var.environment}-eks-node"
  assume_role_policy = data.aws_iam_policy_document.eks_node_assume_role[0].json

  tags = merge(local.common_tags, {
    Name = "${var.deployment_name}-${var.environment}-eks-node"
  })
}

resource "aws_iam_role_policy_attachment" "eks_node_worker" {
  count = local.create_eks ? 1 : 0

  role       = aws_iam_role.eks_node[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}

resource "aws_iam_role_policy_attachment" "eks_node_cni" {
  count = local.create_eks ? 1 : 0

  role       = aws_iam_role.eks_node[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

resource "aws_iam_role_policy_attachment" "eks_node_ecr_readonly" {
  count = local.create_eks ? 1 : 0

  role       = aws_iam_role.eks_node[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_eks_node_group" "default" {
  count = local.create_eks ? 1 : 0

  cluster_name    = aws_eks_cluster.main[0].name
  node_group_name = "${var.deployment_name}-${var.environment}-default"
  node_role_arn   = aws_iam_role.eks_node[0].arn
  subnet_ids      = aws_subnet.private[*].id

  instance_types = var.eks_node_instance_types
  disk_size      = var.eks_node_disk_size

  scaling_config {
    desired_size = var.eks_node_desired_size
    min_size     = var.eks_node_min_size
    max_size     = var.eks_node_max_size
  }

  update_config {
    max_unavailable = 1
  }

  tags = merge(local.common_tags, {
    Name = "${var.deployment_name}-${var.environment}-default"
  })

  depends_on = [
    aws_iam_role_policy_attachment.eks_node_worker,
    aws_iam_role_policy_attachment.eks_node_cni,
    aws_iam_role_policy_attachment.eks_node_ecr_readonly
  ]
}

resource "aws_eks_addon" "managed" {
  for_each = local.create_eks ? toset(var.eks_addons) : toset([])

  cluster_name                = aws_eks_cluster.main[0].name
  addon_name                  = each.value
  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"

  tags = merge(local.common_tags, {
    Name = "${var.eks_cluster_name}-${each.value}"
  })

  depends_on = [
    aws_eks_node_group.default
  ]
}

resource "aws_eks_access_entry" "admin" {
  for_each = local.create_eks ? toset(var.eks_admin_principal_arns) : toset([])

  cluster_name  = aws_eks_cluster.main[0].name
  principal_arn = each.value
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "admin" {
  for_each = local.create_eks ? toset(var.eks_admin_principal_arns) : toset([])

  cluster_name  = aws_eks_cluster.main[0].name
  principal_arn = each.value
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }

  depends_on = [
    aws_eks_access_entry.admin
  ]
}

resource "aws_eks_access_entry" "deploy" {
  for_each = local.create_eks ? toset(var.eks_deploy_principal_arns) : toset([])

  cluster_name  = aws_eks_cluster.main[0].name
  principal_arn = each.value
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "deploy" {
  for_each = local.create_eks ? toset(var.eks_deploy_principal_arns) : toset([])

  cluster_name  = aws_eks_cluster.main[0].name
  principal_arn = each.value
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSEditPolicy"

  access_scope {
    type = "cluster"
  }

  depends_on = [
    aws_eks_access_entry.deploy
  ]
}

data "tls_certificate" "eks_oidc" {
  count = local.create_eks_oidc_provider ? 1 : 0

  url = aws_eks_cluster.main[0].identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "eks" {
  count = local.create_eks_oidc_provider ? 1 : 0

  url             = aws_eks_cluster.main[0].identity[0].oidc[0].issuer
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks_oidc[0].certificates[0].sha1_fingerprint]

  tags = merge(local.common_tags, {
    Name = "${var.eks_cluster_name}-oidc"
  })
}
