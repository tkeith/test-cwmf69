# AWS Provider version constraint
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

# Main VPC Resource
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "${var.project}-${var.environment}-vpc"
    Environment = var.environment
    Purpose     = "Primary VPC for Delayed Messaging System"
    ManagedBy   = "Terraform"
  }
}

# Public Subnets
resource "aws_subnet" "public" {
  count                   = length(var.public_subnet_cidrs)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = "${var.region}${var.availability_zones[count.index]}"
  map_public_ip_on_launch = true

  tags = {
    Name        = "${var.project}-${var.environment}-public-${var.availability_zones[count.index]}"
    Environment = var.environment
    Type        = "Public"
    ManagedBy   = "Terraform"
  }
}

# Private Subnets
resource "aws_subnet" "private" {
  count             = length(var.private_subnet_cidrs)
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = "${var.region}${var.availability_zones[count.index]}"

  tags = {
    Name        = "${var.project}-${var.environment}-private-${var.availability_zones[count.index]}"
    Environment = var.environment
    Type        = "Private"
    ManagedBy   = "Terraform"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name        = "${var.project}-${var.environment}-igw"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# Elastic IPs for NAT Gateways
resource "aws_eip" "nat" {
  count = var.enable_nat_gateway ? length(var.availability_zones) : 0
  vpc   = true

  tags = {
    Name        = "${var.project}-${var.environment}-eip-${var.availability_zones[count.index]}"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }

  depends_on = [aws_internet_gateway.main]
}

# NAT Gateways
resource "aws_nat_gateway" "main" {
  count         = var.enable_nat_gateway ? length(var.availability_zones) : 0
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = {
    Name        = "${var.project}-${var.environment}-nat-${var.availability_zones[count.index]}"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }

  depends_on = [aws_internet_gateway.main]
}

# Route Table for Public Subnets
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name        = "${var.project}-${var.environment}-public-rt"
    Environment = var.environment
    Type        = "Public"
    ManagedBy   = "Terraform"
  }
}

# Route Tables for Private Subnets
resource "aws_route_table" "private" {
  count  = length(var.availability_zones)
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = var.enable_nat_gateway ? aws_nat_gateway.main[count.index].id : null
  }

  tags = {
    Name        = "${var.project}-${var.environment}-private-rt-${var.availability_zones[count.index]}"
    Environment = var.environment
    Type        = "Private"
    ManagedBy   = "Terraform"
  }
}

# Route Table Associations for Public Subnets
resource "aws_route_table_association" "public" {
  count          = length(var.public_subnet_cidrs)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Route Table Associations for Private Subnets
resource "aws_route_table_association" "private" {
  count          = length(var.private_subnet_cidrs)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

# VPC Flow Logs
resource "aws_flow_log" "main" {
  iam_role_arn    = aws_iam_role.flow_log.arn
  log_destination = aws_cloudwatch_log_group.flow_log.arn
  traffic_type    = "ALL"
  vpc_id          = aws_vpc.main.id

  tags = {
    Name        = "${var.project}-${var.environment}-flow-log"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# CloudWatch Log Group for VPC Flow Logs
resource "aws_cloudwatch_log_group" "flow_log" {
  name              = "/aws/vpc/${var.project}-${var.environment}-flow-logs"
  retention_in_days = 30

  tags = {
    Name        = "${var.project}-${var.environment}-flow-log-group"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# IAM Role for VPC Flow Logs
resource "aws_iam_role" "flow_log" {
  name = "${var.project}-${var.environment}-flow-log-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "vpc-flow-logs.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name        = "${var.project}-${var.environment}-flow-log-role"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# IAM Role Policy for VPC Flow Logs
resource "aws_iam_role_policy" "flow_log" {
  name = "${var.project}-${var.environment}-flow-log-policy"
  role = aws_iam_role.flow_log.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogGroups",
          "logs:DescribeLogStreams"
        ]
        Effect = "Allow"
        Resource = "*"
      }
    ]
  })
}

# Outputs
output "vpc_id" {
  description = "ID of the created VPC"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "IDs of the created public subnets"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs of the created private subnets"
  value       = aws_subnet.private[*].id
}

output "nat_gateway_ids" {
  description = "IDs of the created NAT gateways"
  value       = var.enable_nat_gateway ? aws_nat_gateway.main[*].id : []
}