# Production Environment Configuration for Delayed Messaging System
# Configured for high availability and production-grade performance

# Environment Identifier
# Used for resource tagging and naming
environment = "production"

# AWS Region Configuration
# Primary region for infrastructure deployment
region = "us-west-2"

# Networking Configuration
# Production VPC with sufficient IP space for scalability
vpc_cidr = "10.0.0.0/16"

# Database Configuration
# Memory-optimized instance for high concurrent connections
rds_instance_class = "db.r6g.xlarge"

# Redis Configuration
# Memory-optimized nodes for message queue and caching
redis_node_type = "cache.r6g.xlarge"

# ECS Configuration
# Compute-optimized ARM instances for efficient processing
ecs_instance_type = "c6g.xlarge"

# Auto-scaling Configuration
# Sized for 10,000 concurrent users with 3x scaling factor
min_capacity = 4
max_capacity = 12

# Backup Configuration
# Extended retention for production data protection
backup_retention_days = 30

# High Availability Configuration
# Multi-AZ deployment for 99.9% uptime target
multi_az = true