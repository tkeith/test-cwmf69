# AWS Provider configuration for Redis infrastructure
# Provider version: ~> 5.0
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Redis subnet group for cluster deployment
resource "aws_elasticache_subnet_group" "redis_subnet_group" {
  name        = "${var.environment}-redis-subnet-group"
  subnet_ids  = var.private_subnet_ids
  description = "Subnet group for Redis cluster in ${var.environment} environment"

  tags = {
    Name        = "${var.environment}-redis-subnet-group"
    Environment = var.environment
  }
}

# Security group for Redis cluster
resource "aws_security_group" "redis_sg" {
  name        = "${var.environment}-redis-sg"
  description = "Security group for Redis cluster"
  vpc_id      = var.vpc_id

  # Allow inbound Redis traffic from application security group
  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [var.app_security_group_id]
    description     = "Allow Redis traffic from application"
  }

  # Allow all outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound traffic"
  }

  tags = {
    Name        = "${var.environment}-redis-sg"
    Environment = var.environment
  }
}

# Redis parameter group configuration
resource "aws_elasticache_parameter_group" "redis_params" {
  family      = "redis7.0"
  name        = "${var.environment}-redis-params"
  description = "Redis parameter group for ${var.environment}"

  # Performance and reliability parameters
  parameter {
    name  = "maxmemory-policy"
    value = "volatile-lru"
  }

  parameter {
    name  = "notify-keyspace-events"
    value = "Ex"
  }

  parameter {
    name  = "timeout"
    value = "300"
  }

  parameter {
    name  = "tcp-keepalive"
    value = "300"
  }

  tags = {
    Name        = "${var.environment}-redis-params"
    Environment = var.environment
  }
}

# Redis replication group for high availability
resource "aws_elasticache_replication_group" "redis_cluster" {
  replication_group_id = "${var.environment}-redis"
  description         = "Redis cluster for delayed messaging system"
  node_type           = var.redis_node_type
  port                = 6379

  # Network configuration
  parameter_group_name = aws_elasticache_parameter_group.redis_params.name
  subnet_group_name    = aws_elasticache_subnet_group.redis_subnet_group.name
  security_group_ids   = [aws_security_group.redis_sg.id]

  # High availability configuration
  automatic_failover_enabled = true
  multi_az_enabled          = true
  num_cache_clusters        = var.redis_num_cache_clusters

  # Engine configuration
  engine         = "redis"
  engine_version = var.redis_engine_version

  # Security configuration
  at_rest_encryption_enabled  = true
  transit_encryption_enabled  = true
  auth_token                 = var.redis_auth_token

  # Maintenance configuration
  maintenance_window        = "sun:05:00-sun:09:00"
  snapshot_window          = "00:00-05:00"
  snapshot_retention_limit = 7
  auto_minor_version_upgrade = true
  apply_immediately         = false

  tags = {
    Name        = "${var.environment}-redis"
    Environment = var.environment
  }
}

# Output the Redis endpoints and security group ID
output "redis_primary_endpoint" {
  description = "Primary endpoint for Redis write operations"
  value       = aws_elasticache_replication_group.redis_cluster.primary_endpoint_address
}

output "redis_reader_endpoint" {
  description = "Reader endpoint for Redis read operations"
  value       = aws_elasticache_replication_group.redis_cluster.reader_endpoint_address
}

output "redis_security_group_id" {
  description = "Security group ID for Redis cluster"
  value       = aws_security_group.redis_sg.id
}