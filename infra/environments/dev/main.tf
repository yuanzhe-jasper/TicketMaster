terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

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

# ---------------------------------------------------------------------------
# S3 bucket — stores Lambda deployment JARs
# ---------------------------------------------------------------------------

resource "aws_s3_bucket" "lambda_artifacts" {
  bucket = "ticketmaster-lambda-artifacts-${var.environment}"
}

resource "aws_s3_bucket_public_access_block" "lambda_artifacts" {
  bucket                  = aws_s3_bucket.lambda_artifacts.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# ---------------------------------------------------------------------------
# Lambda functions
# ---------------------------------------------------------------------------

module "get_events_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-get-events-${var.environment}"
  handler       = "org.example.handlers.GetEventsHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/get-events/function.jar"
  environment   = var.environment

  environment_variables = {
    ENVIRONMENT = var.environment
  }
}