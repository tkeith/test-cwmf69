# Provider version constraint
terraform {
  required_version = ">= 1.0"
}

# Environment identifier for resource naming and tagging
variable "environment" {
  type        = string
  description = "Environment identifier for resource naming and tagging"
  
  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "Environment must be either 'staging' or 'production'."
  }
}

# RDS instance type configuration
variable "db_instance_class" {
  type        = string
  description = "The instance type of the RDS instance optimized for 10,000 concurrent users"
  default     = "db.t3.medium"
  
  validation {
    condition     = can(regex("^db\\.", var.db_instance_class))
    error_message = "DB instance class must be a valid RDS instance type starting with 'db.'."
  }
}

# Storage configuration
variable "db_allocated_storage" {
  type        = number
  description = "The allocated storage in gigabytes for message and user data"
  default     = 20
  
  validation {
    condition     = var.db_allocated_storage >= 20 && var.db_allocated_storage <= 16384
    error_message = "Allocated storage must be between 20 and 16384 GB."
  }
}

variable "db_max_allocated_storage" {
  type        = number
  description = "The upper limit in gigabytes for RDS autoscaling to handle growth"
  default     = 100
  
  validation {
    condition     = var.db_max_allocated_storage >= var.db_allocated_storage
    error_message = "Maximum allocated storage must be greater than or equal to allocated storage."
  }
}

# Database name and credentials
variable "db_name" {
  type        = string
  description = "The name of the database to create when the instance is created"
  
  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9_]*$", var.db_name))
    error_message = "Database name must start with a letter and contain only alphanumeric characters and underscores."
  }
}

variable "db_username" {
  type        = string
  description = "Username for the database master user"
  sensitive   = true
  
  validation {
    condition     = can(regex("^[a-zA-Z][a-zA-Z0-9_]*$", var.db_username))
    error_message = "Username must start with a letter and contain only alphanumeric characters and underscores."
  }
}

# High availability configuration
variable "multi_az" {
  type        = bool
  description = "Enable Multi-AZ deployment for high availability"
  default     = true
}

# Network configuration
variable "subnet_ids" {
  type        = list(string)
  description = "List of subnet IDs where RDS can be deployed for high availability"
  
  validation {
    condition     = length(var.subnet_ids) >= 2
    error_message = "At least two subnet IDs are required for high availability."
  }
}

variable "vpc_id" {
  type        = string
  description = "The VPC ID where the RDS instance will be created"
  
  validation {
    condition     = can(regex("^vpc-", var.vpc_id))
    error_message = "VPC ID must be a valid AWS VPC ID starting with 'vpc-'."
  }
}

# Backup configuration
variable "backup_retention_period" {
  type        = number
  description = "The days to retain automated backups for disaster recovery"
  default     = 7
  
  validation {
    condition     = var.backup_retention_period >= 7 && var.backup_retention_period <= 35
    error_message = "Backup retention period must be between 7 and 35 days."
  }
}

# Security configuration
variable "storage_encrypted" {
  type        = bool
  description = "Specifies whether the DB instance storage should be encrypted at rest"
  default     = true
}

variable "deletion_protection" {
  type        = bool
  description = "Prevents accidental deletion of the database instance"
  default     = true
}

variable "allowed_cidr_blocks" {
  type        = list(string)
  description = "List of CIDR blocks that are allowed to access the RDS instance for security"
  
  validation {
    condition     = alltrue([
      for cidr in var.allowed_cidr_blocks : can(cidrhost(cidr, 0))
    ])
    error_message = "All elements must be valid CIDR blocks."
  }
}