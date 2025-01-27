# Output definitions for the networking module
# These outputs expose critical networking information for use by other modules

output "vpc_id" {
  description = "ID of the created VPC for the Delayed Messaging System"
  value       = aws_vpc.main.id
}

output "vpc_cidr" {
  description = "CIDR block of the VPC for network planning and security group configuration"
  value       = aws_vpc.main.cidr_block
}

output "public_subnet_ids" {
  description = "List of public subnet IDs across multiple availability zones for load balancer deployment"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "List of private subnet IDs across multiple availability zones for application deployment"
  value       = aws_subnet.private[*].id
}

output "public_subnet_cidrs" {
  description = "List of public subnet CIDR blocks for network planning and security group rules"
  value       = aws_subnet.public[*].cidr_block
}

output "private_subnet_cidrs" {
  description = "List of private subnet CIDR blocks for network planning and security group rules"
  value       = aws_subnet.private[*].cidr_block
}