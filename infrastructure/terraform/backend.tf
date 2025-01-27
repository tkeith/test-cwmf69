terraform {
  backend "s3" {
    # Primary state storage bucket
    bucket = "delayed-messaging-terraform-state"
    key    = "${var.environment}/terraform.tfstate"
    region = "us-west-2"

    # Enable encryption and versioning
    encrypt  = true
    versioning = true

    # DynamoDB table for state locking
    dynamodb_table = "terraform-state-lock"

    # Server-side encryption configuration using AWS KMS
    server_side_encryption_configuration {
      rule {
        apply_server_side_encryption_by_default {
          sse_algorithm = "aws:kms"
        }
      }
    }

    # Cross-region replication configuration for high availability
    replication_configuration {
      role = "arn:aws:iam::ACCOUNT_ID:role/terraform-state-replication-role"
      rules {
        status = "Enabled"
        destination {
          bucket        = "arn:aws:s3:::delayed-messaging-terraform-state-replica"
          storage_class = "STANDARD"
        }
      }
    }

    # Access logging configuration
    logging {
      target_bucket = "delayed-messaging-terraform-logs"
      target_prefix = "state-access-logs/"
    }

    # Additional security configurations
    force_destroy          = false
    acceleration_status    = "Enabled"
    block_public_acls     = true
    block_public_policy   = true
    ignore_public_acls    = true
    restrict_public_buckets = true

    # Lifecycle rules for state file management
    lifecycle_rule {
      enabled = true
      noncurrent_version_expiration {
        days = 90
      }
      noncurrent_version_transition {
        days          = 30
        storage_class = "STANDARD_IA"
      }
    }
  }

  # Required provider version constraints
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }

  # Minimum required Terraform version
  required_version = ">= 1.0.0"
}