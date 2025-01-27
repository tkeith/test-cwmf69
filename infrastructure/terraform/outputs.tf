# VPC and Networking Outputs
output "vpc_id" {
  description = "ID of the VPC hosting the Delayed Messaging System"
  value       = module.vpc.vpc_id
}

output "private_subnet_ids" {
  description = "List of private subnet IDs for application resources"
  value       = module.vpc.private_subnet_ids
}

output "public_subnet_ids" {
  description = "List of public subnet IDs for load balancers"
  value       = module.vpc.public_subnet_ids
}

# ECS Cluster and Service Outputs
output "ecs_cluster_arn" {
  description = "ARN of the ECS cluster running application services"
  value       = module.ecs.cluster_arn
}

output "ecs_service_arns" {
  description = "Map of service names to their ARNs in ECS"
  value = {
    api      = module.ecs.api_service_arn
    worker   = module.ecs.worker_service_arn
    websocket = module.ecs.websocket_service_arn
  }
}

# Load Balancer Outputs
output "alb_dns_name" {
  description = "DNS name of the application load balancer"
  value       = module.alb.dns_name
}

# Database Outputs
output "rds_endpoint" {
  description = "Primary endpoint of the RDS PostgreSQL instance"
  value       = module.rds.primary_endpoint
}

output "rds_read_replica_endpoints" {
  description = "List of read replica endpoints for RDS PostgreSQL"
  value       = module.rds.read_replica_endpoints
}

# Redis Outputs
output "redis_endpoint" {
  description = "Primary endpoint of the Redis cluster"
  value       = module.redis.primary_endpoint
}

output "redis_port" {
  description = "Port number for Redis cluster access"
  value       = module.redis.port
}

# CDN and Security Outputs
output "cloudfront_distribution_id" {
  description = "ID of the CloudFront distribution"
  value       = module.cdn.distribution_id
}

output "cloudfront_domain_name" {
  description = "Domain name of the CloudFront distribution"
  value       = module.cdn.domain_name
}

output "waf_web_acl_arn" {
  description = "ARN of the WAF web ACL protecting the application"
  value       = module.security.waf_web_acl_arn
}

output "certificate_arn" {
  description = "ARN of the SSL/TLS certificate for HTTPS"
  value       = module.security.certificate_arn
}

# Monitoring and Logging Outputs
output "cloudwatch_log_groups" {
  description = "Map of service names to their CloudWatch log group ARNs"
  value = {
    api       = module.monitoring.api_log_group_arn
    worker    = module.monitoring.worker_log_group_arn
    websocket = module.monitoring.websocket_log_group_arn
    database  = module.monitoring.database_log_group_arn
    redis     = module.monitoring.redis_log_group_arn
  }
}

# Security Group Outputs
output "security_group_ids" {
  description = "Map of resource types to their security group IDs"
  value = {
    alb      = module.security.alb_security_group_id
    ecs      = module.security.ecs_security_group_id
    database = module.security.database_security_group_id
    redis    = module.security.redis_security_group_id
  }
}