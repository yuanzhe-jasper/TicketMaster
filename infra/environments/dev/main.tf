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
# Lambda layer — shared runtime dependencies (aws-lambda-java-core, etc.)
# ---------------------------------------------------------------------------

resource "aws_lambda_layer_version" "dependencies" {
  layer_name          = "ticketmaster-dependencies-${var.environment}"
  s3_bucket           = aws_s3_bucket.lambda_artifacts.bucket
  s3_key              = "layers/dependencies/layer.zip"
  compatible_runtimes = ["java21"]
}

# ---------------------------------------------------------------------------
# DynamoDB tables
# ---------------------------------------------------------------------------

resource "aws_dynamodb_table" "events" {
  name         = "ticketmaster-events-${var.environment}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }
}

# IAM policy — allows writing to the events table
resource "aws_iam_policy" "dynamodb_events_write" {
  name = "ticketmaster-events-write-${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["dynamodb:PutItem"]
      Resource = aws_dynamodb_table.events.arn
    }]
  })
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

  layer_arns = [aws_lambda_layer_version.dependencies.arn]

  environment_variables = {
    ENVIRONMENT = var.environment
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "GET /events"
}

module "create_event_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-create-event-${var.environment}"
  handler       = "org.example.handlers.CreateEventHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/create-event/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_events_write.arn]

  environment_variables = {
    ENVIRONMENT  = var.environment
    EVENTS_TABLE = aws_dynamodb_table.events.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "POST /events"
}

# ---------------------------------------------------------------------------
# API Gateway (HTTP API v2) - Rest is v1
# ---------------------------------------------------------------------------

resource "aws_apigatewayv2_api" "this" {
  name          = "ticketmaster-api-${var.environment}"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
    allow_headers = ["Content-Type", "Authorization"]
  }
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.this.id
  name        = "$default"
  auto_deploy = true
}
