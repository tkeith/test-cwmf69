# Configure required providers with version constraints
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }
}

# Configure AWS provider with region and default tags
provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project              = "delayed-messaging"
      Environment          = var.environment
      ManagedBy           = "terraform"
      Service             = "messaging"
      DeploymentType      = "multi-az"
      HighAvailability    = "true"
      BackupEnabled       = "true"
      SecurityCompliance  = "standard"
      CostCenter         = "messaging-platform"
      MaintenanceWindow  = "sun:03:00-sun:05:00"
    }
  }
}

# Configure random provider for consistent resource naming
provider "random" {
  keepers = {
    environment = var.environment
  }
}