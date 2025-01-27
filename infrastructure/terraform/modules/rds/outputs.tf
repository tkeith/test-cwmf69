# Database connection information
output "db_endpoint" {
  description = "The endpoint URL for connecting to the PostgreSQL database"
  value       = aws_db_instance.main.endpoint
  sensitive   = true
}

output "db_address" {
  description = "The DNS address of the RDS instance"
  value       = aws_db_instance.main.address
  sensitive   = true
}

output "db_port" {
  description = "The port number for PostgreSQL connections (default: 5432)"
  value       = aws_db_instance.main.port
}

output "db_name" {
  description = "The name of the default PostgreSQL database"
  value       = aws_db_instance.main.db_name
}

# Security and network configuration
output "db_security_group_id" {
  description = "The ID of the security group controlling access to the RDS instance"
  value       = aws_security_group.rds.id
}

# Resource identifiers
output "db_instance_id" {
  description = "The unique identifier of the RDS instance"
  value       = aws_db_instance.main.id
}

output "db_arn" {
  description = "The Amazon Resource Name (ARN) of the RDS instance"
  value       = aws_db_instance.main.arn
}

# High availability information
output "db_availability_zone" {
  description = "The availability zone where the RDS instance is deployed"
  value       = aws_db_instance.main.availability_zone
}

output "db_multi_az" {
  description = "Whether the RDS instance is deployed in multiple availability zones"
  value       = aws_db_instance.main.multi_az
}

# Performance and monitoring
output "db_monitoring_role_arn" {
  description = "The ARN of the IAM role used for enhanced monitoring"
  value       = aws_iam_role.rds_monitoring.arn
}

output "db_performance_insights_enabled" {
  description = "Whether Performance Insights is enabled for the RDS instance"
  value       = aws_db_instance.main.performance_insights_enabled
}

# Backup and maintenance
output "db_backup_retention_period" {
  description = "The number of days automated backups are retained"
  value       = aws_db_instance.main.backup_retention_period
}

output "db_maintenance_window" {
  description = "The maintenance window for the RDS instance"
  value       = aws_db_instance.main.maintenance_window
}

# Storage configuration
output "db_allocated_storage" {
  description = "The amount of storage allocated to the RDS instance in gigabytes"
  value       = aws_db_instance.main.allocated_storage
}

output "db_max_allocated_storage" {
  description = "The maximum storage that can be allocated to the RDS instance in gigabytes"
  value       = aws_db_instance.main.max_allocated_storage
}

# Resource tags
output "db_tags" {
  description = "The tags assigned to the RDS instance"
  value       = aws_db_instance.main.tags
}