# Core Terraform configuration with production state management
terraform {
  required_version = ">= 1.0"
  
  backend "s3" {
    bucket         = "delayed-messaging-terraform-state-prod"
    key            = "production/terraform.tfstate"
    region         = "us-west-2"
    encrypt        = true
    dynamodb_table = "terraform-state-lock-prod"
    kms_key_id     = "arn:aws:kms:us-west-2:ACCOUNT_ID:key/KEY_ID"
  }
}

# AWS Provider configuration with production-specific tags
provider "aws" {
  region = "us-west-2"
  
  default_tags {
    tags = {
      Environment          = "production"
      Project             = "delayed-messaging"
      ManagedBy           = "terraform"
      Service             = "messaging"
      DeploymentType      = "multi-az"
      HighAvailability    = "true"
      BackupEnabled       = "true"
      SecurityCompliance  = "high"
      CostCenter         = "messaging-platform"
      MaintenanceWindow  = "sun:03:00-sun:05:00"
    }
  }
}

# Production environment infrastructure configuration
module "root" {
  source = "../.."

  # Environment configuration
  environment = "production"
  region      = "us-west-2"
  vpc_cidr    = "10.0.0.0/16"

  # High-availability database configuration
  rds_instance_class       = "db.r6g.xlarge"
  multi_az                 = true
  backup_retention_period  = 30
  enable_deletion_protection = true
  enable_performance_insights = true
  enable_encryption         = true

  # Production-grade Redis configuration
  redis_node_type               = "cache.r6g.xlarge"
  redis_num_cache_clusters      = 3
  redis_automatic_failover      = true
  redis_at_rest_encryption     = true
  redis_transit_encryption     = true

  # ECS cluster configuration for high concurrency
  ecs_instance_type            = "c6g.xlarge"
  min_capacity                 = 4
  max_capacity                 = 20
  enable_container_insights    = true
  enable_execute_command       = false

  # Enhanced security configuration
  enable_cloudtrail           = true
  enable_vpc_flow_logs        = true
  enable_waf                  = true
  enable_shield_advanced      = true
  enable_guardduty            = true

  # Monitoring and alerting configuration
  enable_enhanced_monitoring  = true
  alarm_evaluation_periods    = 3
  alarm_threshold_period      = 300
  enable_detailed_metrics     = true

  # Backup and disaster recovery
  cross_region_backup        = true
  point_in_time_recovery     = true
  snapshot_retention_days    = 30

  # Network security configuration
  private_subnet_cidrs       = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnet_cidrs        = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
  database_subnet_cidrs      = ["10.0.201.0/24", "10.0.202.0/24", "10.0.203.0/24"]
  enable_network_firewall    = true
  enable_transit_gateway     = true

  # SSL/TLS configuration
  ssl_certificate_arn        = "arn:aws:acm:us-west-2:ACCOUNT_ID:certificate/CERT_ID"
  minimum_tls_version       = "TLSv1.2_2021"

  # IAM and access control
  enable_iam_auth           = true
  enable_service_control_policies = true
  enable_permission_boundaries   = true

  # Compliance and audit
  enable_config             = true
  enable_security_hub       = true
  enable_audit_logs         = true
  log_retention_days        = 365

  # Cost optimization
  enable_savings_plan       = true
  enable_cost_allocation_tags = true
  reserved_instance_term    = "1_year"

  # Depends on core infrastructure modules
  depends_on = [
    aws_kms_key.encryption_key,
    aws_config_configuration_recorder.compliance,
    aws_security_hub_account.security
  ]
}

# KMS key for encryption
resource "aws_kms_key" "encryption_key" {
  description             = "KMS key for production environment encryption"
  deletion_window_in_days = 30
  enable_key_rotation     = true
  
  tags = {
    Name = "delayed-messaging-kms-production"
  }
}

# AWS Config for compliance monitoring
resource "aws_config_configuration_recorder" "compliance" {
  name     = "delayed-messaging-config-production"
  role_arn = aws_iam_role.config_role.arn
  
  recording_group {
    all_supported = true
    include_global_resources = true
  }
}

# Security Hub for security monitoring
resource "aws_security_hub_account" "security" {
  enable_default_standards = true
  
  control_finding_generator = "SECURITY_CONTROL"
  auto_enable_controls     = true
}