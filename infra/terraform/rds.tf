resource "aws_security_group" "rds" {
  count = local.create_rds ? 1 : 0

  name_prefix = "${var.deployment_name}-${var.environment}-rds-"
  description = "PostgreSQL access for ${var.app_name}"
  vpc_id      = aws_vpc.main[0].id

  ingress {
    description = "PostgreSQL from application VPC"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "Allow outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${var.deployment_name}-${var.environment}-rds-sg"
  })
}

resource "aws_db_subnet_group" "postgres" {
  count = local.create_rds ? 1 : 0

  name       = "${var.deployment_name}-${var.environment}-postgres"
  subnet_ids = aws_subnet.private[*].id

  tags = merge(local.common_tags, {
    Name = "${var.deployment_name}-${var.environment}-postgres"
  })
}

resource "aws_db_instance" "postgres" {
  count = local.create_rds ? 1 : 0

  identifier = "${var.deployment_name}-${var.environment}-postgres"

  engine         = "postgres"
  engine_version = var.rds_engine_version
  instance_class = var.rds_instance_class

  allocated_storage     = var.rds_allocated_storage
  max_allocated_storage = var.rds_max_allocated_storage
  storage_encrypted     = true
  storage_type          = "gp3"

  db_name                     = var.rds_database_name
  username                    = var.rds_master_username
  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.postgres[0].name
  vpc_security_group_ids = [aws_security_group.rds[0].id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period   = var.rds_backup_retention_period
  deletion_protection       = true
  skip_final_snapshot       = false
  final_snapshot_identifier = "${var.deployment_name}-${var.environment}-postgres-final"

  auto_minor_version_upgrade = true
  copy_tags_to_snapshot      = true

  tags = merge(local.common_tags, {
    Name = "${var.deployment_name}-${var.environment}-postgres"
  })
}
