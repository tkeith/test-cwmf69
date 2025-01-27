# AWS Provider configuration
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "${var.environment}-delayed-messaging-cluster"

  setting {
    name  = "containerInsights"
    value = var.enable_container_insights ? "enabled" : "disabled"
  }

  tags = {
    Environment = var.environment
    Project     = "DelayedMessaging"
    ManagedBy   = "Terraform"
  }
}

# API Task Definition
resource "aws_ecs_task_definition" "api" {
  family                   = "${var.environment}-api"
  network_mode            = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                     = "512"
  memory                  = "1024"
  execution_role_arn      = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn           = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([{
    name         = "api"
    image        = var.api_image
    essential    = true
    cpu          = 512
    memory       = 1024
    portMappings = [{
      containerPort = 3000
      protocol      = "tcp"
    }]
    healthCheck = {
      command     = ["CMD-SHELL", "curl -f http://localhost:3000/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = "/ecs/${var.environment}-api"
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "api"
      }
    }
  }])

  tags = {
    Environment = var.environment
    Service     = "API"
    ManagedBy   = "Terraform"
  }
}

# WebSocket Task Definition
resource "aws_ecs_task_definition" "websocket" {
  family                   = "${var.environment}-websocket"
  network_mode            = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                     = "512"
  memory                  = "1024"
  execution_role_arn      = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn           = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([{
    name         = "websocket"
    image        = var.websocket_image
    essential    = true
    cpu          = 512
    memory       = 1024
    portMappings = [{
      containerPort = 3001
      protocol      = "tcp"
    }]
    healthCheck = {
      command     = ["CMD-SHELL", "nc -z localhost 3001 || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 60
    }
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = "/ecs/${var.environment}-websocket"
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "websocket"
      }
    }
  }])

  tags = {
    Environment = var.environment
    Service     = "WebSocket"
    ManagedBy   = "Terraform"
  }
}

# API Service
resource "aws_ecs_service" "api" {
  name                              = "${var.environment}-api-service"
  cluster                          = aws_ecs_cluster.main.id
  task_definition                  = aws_ecs_task_definition.api.arn
  desired_count                    = 2
  launch_type                      = "FARGATE"
  platform_version                 = "LATEST"
  deployment_maximum_percent       = 200
  deployment_minimum_healthy_percent = 100
  health_check_grace_period_seconds = 60

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.api.arn
  }

  tags = {
    Environment = var.environment
    Service     = "API"
    ManagedBy   = "Terraform"
  }
}

# WebSocket Service
resource "aws_ecs_service" "websocket" {
  name                              = "${var.environment}-websocket-service"
  cluster                          = aws_ecs_cluster.main.id
  task_definition                  = aws_ecs_task_definition.websocket.arn
  desired_count                    = 2
  launch_type                      = "FARGATE"
  platform_version                 = "LATEST"
  deployment_maximum_percent       = 200
  deployment_minimum_healthy_percent = 100
  health_check_grace_period_seconds = 60

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.websocket.arn
  }

  tags = {
    Environment = var.environment
    Service     = "WebSocket"
    ManagedBy   = "Terraform"
  }
}

# Auto Scaling Target for API
resource "aws_appautoscaling_target" "api" {
  max_capacity       = 10
  min_capacity       = 2
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.api.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

# Auto Scaling Target for WebSocket
resource "aws_appautoscaling_target" "websocket" {
  max_capacity       = 8
  min_capacity       = 2
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.websocket.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

# CPU-based Auto Scaling Policy for API
resource "aws_appautoscaling_policy" "api_cpu" {
  name               = "${var.environment}-api-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.api.resource_id
  scalable_dimension = aws_appautoscaling_target.api.scalable_dimension
  service_namespace  = aws_appautoscaling_target.api.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value       = 70.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}

# Connection-based Auto Scaling Policy for WebSocket
resource "aws_appautoscaling_policy" "websocket_connections" {
  name               = "${var.environment}-websocket-connection-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.websocket.resource_id
  scalable_dimension = aws_appautoscaling_target.websocket.scalable_dimension
  service_namespace  = aws_appautoscaling_target.websocket.service_namespace

  target_tracking_scaling_policy_configuration {
    target_value       = 1000.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60

    customized_metric_specification {
      metric_name = "ConnectionCount"
      namespace   = "AWS/ECS"
      statistic   = "Average"
      unit        = "Count"
    }
  }
}

# Security Group for ECS Tasks
resource "aws_security_group" "ecs_tasks" {
  name        = "${var.environment}-ecs-tasks-sg"
  description = "Security group for ECS tasks"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 3000
    to_port         = 3000
    protocol        = "tcp"
    security_groups = [var.alb_security_group_id]
  }

  ingress {
    from_port       = 3001
    to_port         = 3001
    protocol        = "tcp"
    security_groups = [var.alb_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# Service Discovery Private DNS Namespace
resource "aws_service_discovery_private_dns_namespace" "main" {
  name        = "delayed-messaging.local"
  vpc         = var.vpc_id
  description = "Service discovery namespace for Delayed Messaging System"
}

# Service Discovery Service for API
resource "aws_service_discovery_service" "api" {
  name = "${var.environment}-api"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    
    dns_records {
      ttl  = 10
      type = "A"
    }
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

# Service Discovery Service for WebSocket
resource "aws_service_discovery_service" "websocket" {
  name = "${var.environment}-websocket"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    
    dns_records {
      ttl  = 10
      type = "A"
    }
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}