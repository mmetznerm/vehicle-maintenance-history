data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "demo" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${var.deployment_name}-${var.environment}-vpc"
  }
}

resource "aws_internet_gateway" "demo" {
  vpc_id = aws_vpc.demo.id

  tags = {
    Name = "${var.deployment_name}-${var.environment}-igw"
  }
}

resource "aws_subnet" "public" {
  count = local.availability_zone_count

  vpc_id                  = aws_vpc.demo.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = merge(local.public_subnet_tags, {
    Name = "${var.deployment_name}-${var.environment}-public-${count.index + 1}"
    Tier = "public"
  })
}

resource "aws_subnet" "private_app" {
  count = local.availability_zone_count

  vpc_id            = aws_vpc.demo.id
  cidr_block        = var.private_app_subnet_cidrs[count.index]
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = merge(local.private_app_subnet_tags, {
    Name = "${var.deployment_name}-${var.environment}-private-app-${count.index + 1}"
    Tier = "private-app"
  })
}

resource "aws_subnet" "private_database" {
  count = local.availability_zone_count

  vpc_id            = aws_vpc.demo.id
  cidr_block        = var.private_database_subnet_cidrs[count.index]
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "${var.deployment_name}-${var.environment}-private-db-${count.index + 1}"
    Tier = "private-database"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.demo.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.demo.id
  }

  tags = {
    Name = "${var.deployment_name}-${var.environment}-public-rt"
  }
}

resource "aws_route_table_association" "public" {
  count = local.availability_zone_count

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_eip" "nat" {
  count = var.enable_nat_gateway ? 1 : 0

  domain = "vpc"

  tags = {
    Name = "${var.deployment_name}-${var.environment}-nat-eip"
  }
}

resource "aws_nat_gateway" "demo" {
  count = var.enable_nat_gateway ? 1 : 0

  allocation_id = aws_eip.nat[0].id
  subnet_id     = aws_subnet.public[0].id

  tags = {
    Name = "${var.deployment_name}-${var.environment}-nat"
  }

  depends_on = [aws_internet_gateway.demo]
}

resource "aws_route_table" "private_app" {
  vpc_id = aws_vpc.demo.id

  dynamic "route" {
    for_each = var.enable_nat_gateway ? [aws_nat_gateway.demo[0].id] : []

    content {
      cidr_block     = "0.0.0.0/0"
      nat_gateway_id = route.value
    }
  }

  tags = {
    Name = "${var.deployment_name}-${var.environment}-private-app-rt"
  }
}

resource "aws_route_table_association" "private_app" {
  count = local.availability_zone_count

  subnet_id      = aws_subnet.private_app[count.index].id
  route_table_id = aws_route_table.private_app.id
}

resource "aws_route_table" "private_database" {
  vpc_id = aws_vpc.demo.id

  tags = {
    Name = "${var.deployment_name}-${var.environment}-private-db-rt"
  }
}

resource "aws_route_table_association" "private_database" {
  count = local.availability_zone_count

  subnet_id      = aws_subnet.private_database[count.index].id
  route_table_id = aws_route_table.private_database.id
}

resource "aws_security_group" "eks_control_plane" {
  name        = "${var.deployment_name}-${var.environment}-eks-control-plane"
  description = "Security group for the EKS control plane."
  vpc_id      = aws_vpc.demo.id

  egress {
    description = "Allow outbound traffic from the EKS control plane."
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.deployment_name}-${var.environment}-eks-control-plane"
  }
}

resource "aws_security_group" "eks_nodes" {
  name        = "${var.deployment_name}-${var.environment}-eks-nodes"
  description = "Security group for EKS worker nodes."
  vpc_id      = aws_vpc.demo.id

  ingress {
    description = "Allow node-to-node traffic."
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    self        = true
  }

  ingress {
    description     = "Allow kubelet traffic from the EKS control plane."
    from_port       = 10250
    to_port         = 10250
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_control_plane.id]
  }

  ingress {
    description     = "Allow webhook traffic from the EKS control plane."
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_control_plane.id]
  }

  egress {
    description = "Allow outbound traffic from worker nodes."
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.deployment_name}-${var.environment}-eks-nodes"
  }
}

resource "aws_security_group" "rds" {
  name        = "${var.deployment_name}-${var.environment}-rds"
  description = "Security group for the future RDS PostgreSQL instance."
  vpc_id      = aws_vpc.demo.id

  ingress {
    description     = "Allow PostgreSQL from future EKS nodes."
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_nodes.id]
  }

  tags = {
    Name = "${var.deployment_name}-${var.environment}-rds"
  }
}
