# Core Terraform configuration with staging-specific backend
terraform {
  required_version = ">= 1.0"
  
  # Configure S3 backend for state management with encryption and locking
  backend "s3" {
    bucket         = "delayed-messaging-terraform-state-staging"
    key            = "staging/terraform.tfstate"
    region         = "us-west-2"
    encrypt        = true
    dynamodb_table = "terraform-state-lock-staging"
  }
}

# Single-AZ VPC and networking infrastructure for staging
module "networking" {
  source = "../../modules/networking"

  environment        = "staging"
  vpc_cidr          = "10.1.0.0/16"
  region            = "us-west-2"
  # Single AZ deployment for staging to optimize costs
  availability_zones = ["us-west-2a"]
  enable_nat_gateway = true

  tags = {
    Environment = "staging"
    Purpose     = "pre-production-verification"
  }
}

# Cost-optimized ECS cluster configuration for staging
module "ecs" {
  source = "../../modules/ecs"

  environment         = "staging"
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  
  # Use t3.small instances for cost optimization in staging
  instance_type      = "t3.small"
  # Reduced capacity range for staging environment
  min_capacity       = 1
  max_capacity       = 4

  # Auto-scaling configuration for staging workloads
  scaling_rules = {
    cpu_threshold    = 70
    memory_threshold = 80
    scale_in_cooldown  = 300
    scale_out_cooldown = 180
  }

  tags = {
    Environment = "staging"
    Service     = "delayed-messaging"
  }
}

# Single-instance PostgreSQL database for staging
module "rds" {
  source = "../../modules/rds"

  environment          = "staging"
  vpc_id              = module.networking.vpc_id
  database_subnet_ids = module.networking.private_subnet_ids
  
  # Cost-optimized instance class for staging
  instance_class      = "db.t3.small"
  # Single AZ deployment for staging
  multi_az           = false
  
  # Database configuration
  database_name      = "delayed_messaging_staging"
  backup_retention_period = 7
  deletion_protection    = false

  tags = {
    Environment = "staging"
    Service     = "delayed-messaging-db"
  }
}

# Single-node Redis configuration for staging
module "redis" {
  source = "../../modules/redis"

  environment       = "staging"
  vpc_id           = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  
  # Cost-optimized node type for staging
  node_type        = "cache.t3.small"
  # Single node deployment for staging
  num_cache_nodes  = 1
  
  # Redis configuration
  port             = 6379
  parameter_group_family = "redis7"
  
  tags = {
    Environment = "staging"
    Service     = "delayed-messaging-cache"
  }
}

# Output values for reference by other configurations
output "vpc_id" {
  description = "ID of the VPC created for the staging environment"
  value       = module.networking.vpc_id
}

output "rds_endpoint" {
  description = "Endpoint URL for the RDS instance"
  value       = module.rds.db_endpoint
  sensitive   = true
}

output "redis_endpoint" {
  description = "Endpoint URL for the Redis cluster"
  value       = module.redis.redis_endpoint
  sensitive   = true
}