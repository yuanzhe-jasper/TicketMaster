terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Uncomment and configure once your S3 backend bucket is created
  # backend "s3" {
  #   bucket         = "ticketmaster-tfstate-dev"
  #   key            = "dev/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "ticketmaster-tfstate-lock"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "TicketMaster"
      Environment = "dev"
      ManagedBy   = "Terraform"
    }
  }
}