# Environment configuration
variable "environment" {
  description = "Deployment environment identifier for resource tagging and configuration selection"
  type        = string
  validation {
    condition     = can(regex("^(development|staging|production)$", var.environment))
    error_message = "Environment must be one of: development, staging, production"
  }
}

# Region configuration
variable "region" {
  description = "AWS region for infrastructure deployment with high availability considerations"
  type        = string
  default     = "us-west-2"
  validation {
    condition     = can(regex("^[a-z]{2}-[a-z]+-\\d$", var.region))
    error_message = "Must be a valid AWS region identifier"
  }
}

# Network configuration
variable "vpc_cidr" {
  description = "CIDR block for the VPC network segmentation"
  type        = string
  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "VPC CIDR must be a valid IPv4 CIDR block"
  }
}

# Database configuration
variable "rds_instance_class" {
  description = "RDS instance type for PostgreSQL database with performance considerations"
  type        = string
  validation {
    condition     = can(regex("^db\\.[trmc][3-6][a-z]\\.(micro|small|medium|large|xlarge|[2-9]?xlarge)$", var.rds_instance_class))
    error_message = "Invalid RDS instance class format"
  }
}

# Cache and Queue configuration
variable "redis_node_type" {
  description = "Redis node type for message queue and caching with scalability requirements"
  type        = string
  validation {
    condition     = can(regex("^cache\\.[tmrc][3-6][a-z]\\.(micro|small|medium|large|xlarge|[2-9]?xlarge)$", var.redis_node_type))
    error_message = "Invalid Redis node type format"
  }
}

# Compute configuration
variable "ecs_instance_type" {
  description = "EC2 instance type for ECS tasks with performance requirements"
  type        = string
  validation {
    condition     = can(regex("^[a-z][1-6][a-z]?\\.(nano|micro|small|medium|large|xlarge|[2-9]?xlarge)$", var.ecs_instance_type))
    error_message = "Invalid EC2 instance type format"
  }
}

# Scaling configuration
variable "min_capacity" {
  description = "Minimum number of ECS tasks for the service ensuring high availability"
  type        = number
  validation {
    condition     = var.min_capacity >= 2
    error_message = "Minimum capacity must be at least 2 for high availability"
  }
}

variable "max_capacity" {
  description = "Maximum number of ECS tasks for the service supporting concurrent user load"
  type        = number
  validation {
    condition     = var.max_capacity >= var.min_capacity
    error_message = "Maximum capacity must be greater than or equal to minimum capacity"
  }
}

# Backup configuration
variable "backup_retention_days" {
  description = "Number of days to retain RDS backups for disaster recovery"
  type        = number
  default     = 7
  validation {
    condition     = var.backup_retention_days >= 7
    error_message = "Backup retention must be at least 7 days"
  }
}

# High Availability configuration
variable "multi_az" {
  description = "Enable multi-AZ deployment for RDS and Redis ensuring high availability"
  type        = bool
  default     = true
}