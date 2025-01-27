# Output definitions for Redis infrastructure module
# Exposes essential Redis cluster endpoints and security group information
# Version: Terraform ~> 1.0

# Primary endpoint for Redis write operations
# Used for message queue and cache write operations
output "redis_primary_endpoint" {
  description = "Primary endpoint address for Redis write operations in the replication group"
  value       = aws_elasticache_replication_group.redis_cluster.primary_endpoint_address
  sensitive   = false
}

# Reader endpoint for Redis read operations
# Used for high-availability read operations across replica nodes
output "redis_reader_endpoint" {
  description = "Reader endpoint address for Redis read operations across replica nodes"
  value       = aws_elasticache_replication_group.redis_cluster.reader_endpoint_address
  sensitive   = false
}

# Security group ID for Redis cluster access configuration
# Required for configuring application security group rules
output "redis_security_group_id" {
  description = "ID of the security group controlling Redis cluster access"
  value       = aws_security_group.redis_sg.id
  sensitive   = false
}