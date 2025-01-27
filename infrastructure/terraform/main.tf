# Core Terraform configuration with state management
terraform {
  required_version = ">= 1.0"
  
  backend "s3" {
    bucket         = "delayed-messaging-terraform-state-${var.environment}"
    key            = "${var.environment}/terraform.tfstate"
    region         = "us-west-2"
    encrypt        = true
    dynamodb_table = "terraform-state-lock-${var.environment}"
    kms_key_id     = "arn:aws:kms:us-west-2:ACCOUNT_ID:key/KEY_ID"
  }
}

# Random string for unique resource naming
resource "random_string" "suffix" {
  length  = 8
  special = false
  upper   = false
}

# Networking module for multi-AZ VPC setup
module "networking" {
  source = "./modules/networking"

  environment         = var.environment
  vpc_cidr           = var.vpc_cidr
  region             = var.region
  availability_zones = ["us-west-2a", "us-west-2b", "us-west-2c"]
  
  enable_vpc_flow_logs = true
  enable_nat_gateway   = true
  single_nat_gateway   = var.environment != "production"

  tags = {
    Name = "delayed-messaging-vpc-${var.environment}"
  }
}

# ECS cluster for container orchestration
module "ecs" {
  source = "./modules/ecs"

  environment         = var.environment
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  instance_type      = var.ecs_instance_type
  
  min_capacity              = var.environment == "production" ? 4 : 2
  max_capacity              = var.environment == "production" ? 10 : 4
  enable_container_insights = true
  enable_execute_command    = var.environment != "production"

  service_discovery_namespace = "delayed-messaging.local"
  
  tags = {
    Name = "delayed-messaging-ecs-${var.environment}"
  }

  depends_on = [module.networking]
}

# RDS module for PostgreSQL database
module "rds" {
  source = "./modules/rds"

  environment          = var.environment
  vpc_id              = module.networking.vpc_id
  database_subnet_ids = module.networking.database_subnet_ids
  instance_class      = var.rds_instance_class
  
  multi_az                    = var.environment == "production"
  backup_retention_period     = var.environment == "production" ? 30 : 7
  enable_performance_insights = true
  storage_encrypted          = true
  
  database_name = "delayed_messaging"
  port          = 5432
  
  tags = {
    Name = "delayed-messaging-rds-${var.environment}"
  }

  depends_on = [module.networking]
}

# Redis module for message queue and caching
module "redis" {
  source = "./modules/redis"

  environment         = var.environment
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  node_type          = var.redis_node_type
  
  num_cache_clusters         = var.environment == "production" ? 3 : 2
  automatic_failover_enabled = var.environment == "production"
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  
  parameter_group_family = "redis6.x"
  port                  = 6379
  
  tags = {
    Name = "delayed-messaging-redis-${var.environment}"
  }

  depends_on = [module.networking]
}

# CloudWatch monitoring and alarms
module "monitoring" {
  source = "./modules/monitoring"

  environment     = var.environment
  ecs_cluster_id  = module.ecs.cluster_id
  rds_identifier = module.rds.db_identifier
  redis_id       = module.redis.cluster_id
  
  enable_detailed_monitoring = var.environment == "production"
  alarm_evaluation_periods  = 3
  alarm_threshold_period    = 300
  
  tags = {
    Name = "delayed-messaging-monitoring-${var.environment}"
  }
}

# Output values for reference
output "vpc_id" {
  description = "VPC identifier for reference by other modules"
  value       = module.networking.vpc_id
}

output "rds_endpoints" {
  description = "RDS endpoints for application configuration"
  value = {
    primary = module.rds.db_endpoint
    replica = module.rds.db_replica_endpoint
  }
  sensitive = true
}

output "redis_endpoints" {
  description = "Redis endpoints for application configuration"
  value = {
    primary  = module.redis.redis_endpoint
    replicas = module.redis.redis_replica_endpoints
  }
  sensitive = true
}

output "ecs_cluster_name" {
  description = "ECS cluster name for deployment reference"
  value       = module.ecs.cluster_name
}

output "service_discovery_namespace" {
  description = "Service discovery namespace for container communication"
  value       = module.ecs.service_discovery_namespace
}