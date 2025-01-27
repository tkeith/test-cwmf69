# Terraform variable definitions for Redis infrastructure module
# Supporting message queuing and caching functionality with high availability

# Environment name variable with validation
variable "environment" {
  type        = string
  description = "Environment name (e.g., staging, production)"
  
  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "Environment must be either staging or production."
  }
}

# VPC ID for Redis deployment
variable "vpc_id" {
  type        = string
  description = "ID of the VPC where Redis cluster will be deployed"
}

# Private subnet IDs with high availability validation
variable "private_subnet_ids" {
  type        = list(string)
  description = "List of private subnet IDs for Redis subnet group"
  
  validation {
    condition     = length(var.private_subnet_ids) >= 2
    error_message = "At least two private subnets are required for high availability."
  }
}

# Application security group ID
variable "app_security_group_id" {
  type        = string
  description = "Security group ID of the application that will access Redis"
}

# Redis node instance type with validation
variable "redis_node_type" {
  type        = string
  description = "Redis node instance type"
  default     = "cache.t4g.medium"
  
  validation {
    condition     = can(regex("^cache\\.(t4g|r6g|r6gd)\\.", var.redis_node_type))
    error_message = "Redis node type must be a valid AWS ElastiCache instance type."
  }
}

# Number of Redis cache clusters with HA validation
variable "redis_num_cache_clusters" {
  type        = number
  description = "Number of cache clusters (nodes) in the replication group"
  default     = 2
  
  validation {
    condition     = var.redis_num_cache_clusters >= 2
    error_message = "At least two cache clusters are required for high availability."
  }
}

# Redis engine version with validation
variable "redis_engine_version" {
  type        = string
  description = "Redis engine version"
  default     = "7.0"
  
  validation {
    condition     = can(regex("^7\\.0", var.redis_engine_version))
    error_message = "Redis engine version must be 7.0.x for required features."
  }
}