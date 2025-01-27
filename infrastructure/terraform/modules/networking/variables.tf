variable "project" {
  description = "Project identifier for resource naming"
  type        = string
  default     = "delayed-messaging"

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.project))
    error_message = "Project name must contain only lowercase letters, numbers, and hyphens"
  }
}

variable "environment" {
  description = "Deployment environment (staging/production)"
  type        = string

  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "Environment must be either staging or production"
  }
}

variable "region" {
  description = "AWS region for network resources"
  type        = string
  default     = "us-west-2"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"

  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "VPC CIDR must be a valid IPv4 CIDR block"
  }
}

variable "availability_zones" {
  description = "List of availability zone suffixes (a, b, c) for multi-AZ deployment"
  type        = list(string)
  default     = ["a", "b", "c"]

  validation {
    condition     = length(var.availability_zones) >= 2
    error_message = "At least two availability zones required for high availability"
  }
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (one per AZ)"
  type        = list(string)

  validation {
    condition     = length(var.public_subnet_cidrs) == length(var.availability_zones)
    error_message = "Number of public subnet CIDRs must match number of availability zones"
  }
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (one per AZ)"
  type        = list(string)

  validation {
    condition     = length(var.private_subnet_cidrs) == length(var.availability_zones)
    error_message = "Number of private subnet CIDRs must match number of availability zones"
  }
}

variable "enable_nat_gateway" {
  description = "Flag to enable NAT Gateway for private subnet internet access"
  type        = bool
  default     = true
}

variable "enable_vpn_gateway" {
  description = "Flag to enable VPN Gateway for VPC"
  type        = bool
  default     = false
}

variable "tags" {
  description = "Additional tags for networking resources"
  type        = map(string)
  default     = {}
}