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

# GET /events
resource "aws_apigatewayv2_integration" "get_events" {
  api_id                 = aws_apigatewayv2_api.this.id
  integration_type       = "AWS_PROXY"
  integration_uri        = module.get_events_lambda.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "get_events" {
  api_id    = aws_apigatewayv2_api.this.id
  route_key = "GET /events"
  target    = "integrations/${aws_apigatewayv2_integration.get_events.id}"
}

# Allow API Gateway to invoke the Lambda
resource "aws_lambda_permission" "get_events" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = module.get_events_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.this.execution_arn}/*/*"
}