# Output definitions for the ECS module
# Exposes cluster, service, and task details for monitoring and management

output "cluster_id" {
  description = "ID of the created ECS cluster for the delayed messaging system"
  value       = aws_ecs_cluster.main.id
}

output "cluster_name" {
  description = "Name of the created ECS cluster for service identification and monitoring"
  value       = aws_ecs_cluster.main.name
}

output "cluster_arn" {
  description = "ARN of the created ECS cluster for IAM and API operations"
  value       = aws_ecs_cluster.main.arn
}

output "api_service_id" {
  description = "ID of the API ECS service for monitoring and scaling operations"
  value       = aws_ecs_service.api.id
}

output "api_service_name" {
  description = "Name of the API ECS service for CloudWatch metrics and auto-scaling"
  value       = aws_ecs_service.api.name
}

output "websocket_service_id" {
  description = "ID of the WebSocket ECS service for monitoring and scaling operations"
  value       = aws_ecs_service.websocket.id
}

output "websocket_service_name" {
  description = "Name of the WebSocket ECS service for CloudWatch metrics and auto-scaling"
  value       = aws_ecs_service.websocket.name
}