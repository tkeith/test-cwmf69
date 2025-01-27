# Environment and Region Configuration
environment           = "staging"
region               = "us-west-2"

# Network Configuration
vpc_cidr             = "10.1.0.0/16"

# Database Configuration - Sized for pre-production workload
rds_instance_class   = "db.t3.medium"  # 2 vCPU, 4GB RAM for moderate workload
backup_retention_days = 7               # 7 day retention for staging backups

# Cache and Queue Configuration - Balanced for message processing
redis_node_type      = "cache.t3.medium"  # 2 vCPU, 3.09GB RAM for message queue

# Compute Configuration - Optimized for staging workload
ecs_instance_type    = "t3.medium"  # 2 vCPU, 4GB RAM for application containers
min_capacity         = 2            # Minimum 2 tasks for basic HA
max_capacity         = 4            # Scale up to 4 tasks for load testing

# High Availability Configuration
multi_az            = false  # Single AZ deployment for staging environment