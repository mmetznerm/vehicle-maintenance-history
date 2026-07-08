resource "random_password" "rds_master" {
  count = var.enable_rds_database ? 1 : 0

  length  = 32
  special = false
}

resource "aws_db_subnet_group" "demo" {
  count = var.enable_rds_database ? 1 : 0

  name       = "${var.deployment_name}-${var.environment}-postgres"
  subnet_ids = aws_subnet.private_database[*].id

  tags = {
    Name = "${var.deployment_name}-${var.environment}-postgres"
  }
}

resource "aws_db_instance" "demo" {
  count = var.enable_rds_database ? 1 : 0

  identifier = "${var.deployment_name}-${var.environment}-postgres"

  engine         = "postgres"
  engine_version = var.rds_engine_version
  instance_class = var.rds_instance_class

  allocated_storage     = var.rds_allocated_storage
  max_allocated_storage = var.rds_max_allocated_storage
  storage_encrypted     = true
  storage_type          = "gp3"

  db_name  = var.rds_database_name
  username = var.rds_master_username
  password = random_password.rds_master[0].result

  db_subnet_group_name   = aws_db_subnet_group.demo[0].name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period = var.rds_backup_retention_period
  deletion_protection     = false
  skip_final_snapshot     = true
  apply_immediately       = true

  auto_minor_version_upgrade = true

  tags = {
    Name = "${var.deployment_name}-${var.environment}-postgres"
  }
}
