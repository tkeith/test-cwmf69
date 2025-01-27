# Environment Configuration
variable "environment" {
  description = "Environment name (e.g., staging, production)"
  type        = string
  validation {
    condition     = can(regex("^(staging|production)$", var.environment))
    error_message = "Environment must be either staging or production"
  }
}

# Cluster Configuration
variable "enable_container_insights" {
  description = "Enable CloudWatch Container Insights for the ECS cluster"
  type        = bool
  default     = true
}

# API Task Configuration
variable "api_cpu" {
  description = "CPU units for API tasks (1 CPU = 1024 units)"
  type        = number
  default     = 1024
  validation {
    condition     = var.api_cpu >= 512 && var.api_cpu <= 4096
    error_message = "API CPU units must be between 512 and 4096"
  }
}

variable "api_memory" {
  description = "Memory (in MiB) for API tasks"
  type        = number
  default     = 2048
  validation {
    condition     = var.api_memory >= 1024 && var.api_memory <= 8192
    error_message = "API memory must be between 1024 and 8192 MiB"
  }
}

# WebSocket Task Configuration
variable "websocket_cpu" {
  description = "CPU units for WebSocket tasks (1 CPU = 1024 units)"
  type        = number
  default     = 512
  validation {
    condition     = var.websocket_cpu >= 256 && var.websocket_cpu <= 2048
    error_message = "WebSocket CPU units must be between 256 and 2048"
  }
}

variable "websocket_memory" {
  description = "Memory (in MiB) for WebSocket tasks"
  type        = number
  default     = 1024
  validation {
    condition     = var.websocket_memory >= 512 && var.websocket_memory <= 4096
    error_message = "WebSocket memory must be between 512 and 4096 MiB"
  }
}

# Service Scaling Configuration
variable "api_min_tasks" {
  description = "Minimum number of API tasks to run"
  type        = number
  default     = 2
  validation {
    condition     = var.api_min_tasks >= 2
    error_message = "Minimum API tasks must be at least 2 for high availability"
  }
}

variable "api_max_tasks" {
  description = "Maximum number of API tasks to run"
  type        = number
  default     = 10
  validation {
    condition     = var.api_max_tasks >= var.api_min_tasks
    error_message = "Maximum API tasks must be greater than or equal to minimum tasks"
  }
}

variable "websocket_min_tasks" {
  description = "Minimum number of WebSocket tasks to run"
  type        = number
  default     = 2
  validation {
    condition     = var.websocket_min_tasks >= 2
    error_message = "Minimum WebSocket tasks must be at least 2 for high availability"
  }
}

variable "websocket_max_tasks" {
  description = "Maximum number of WebSocket tasks to run"
  type        = number
  default     = 8
  validation {
    condition     = var.websocket_max_tasks >= var.websocket_min_tasks
    error_message = "Maximum WebSocket tasks must be greater than or equal to minimum tasks"
  }
}

# Health Check Configuration
variable "health_check_grace_period" {
  description = "Grace period (in seconds) for health checks"
  type        = number
  default     = 60
  validation {
    condition     = var.health_check_grace_period >= 30 && var.health_check_grace_period <= 300
    error_message = "Health check grace period must be between 30 and 300 seconds"
  }
}

# Network Configuration
variable "vpc_id" {
  description = "ID of the VPC where ECS resources will be deployed"
  type        = string
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs for ECS tasks"
  type        = list(string)
}

# Resource Tagging
variable "tags" {
  description = "Additional tags for ECS resources"
  type        = map(string)
  default     = {}
}