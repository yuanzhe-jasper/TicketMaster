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

data "aws_s3_object" "layer" {
  bucket = aws_s3_bucket.lambda_artifacts.bucket
  key    = "layers/dependencies/layer.zip"
}

resource "aws_lambda_layer_version" "dependencies" {
  layer_name          = "ticketmaster-dependencies-${var.environment}"
  s3_bucket           = aws_s3_bucket.lambda_artifacts.bucket
  s3_key              = "layers/dependencies/layer.zip"
  compatible_runtimes = ["java21"]
  source_code_hash    = data.aws_s3_object.layer.etag
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

# IAM policy — allows reading from the events table
resource "aws_iam_policy" "dynamodb_events_read" {
  name = "ticketmaster-events-read-${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["dynamodb:Scan", "dynamodb:GetItem", "dynamodb:Query"]
      Resource = aws_dynamodb_table.events.arn
    }]
  })
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

resource "aws_dynamodb_table" "tickets" {
  name         = "ticketmaster-tickets-${var.environment}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "eventId"
    type = "S"
  }

  global_secondary_index {
    name            = "eventId-index"
    hash_key        = "eventId"
    projection_type = "ALL"
  }
}

# IAM policy — allows reading from the tickets table (including GSI)
resource "aws_iam_policy" "dynamodb_tickets_read" {
  name = "ticketmaster-tickets-read-${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = ["dynamodb:Scan", "dynamodb:GetItem", "dynamodb:Query"]
      Resource = [
        aws_dynamodb_table.tickets.arn,
        "${aws_dynamodb_table.tickets.arn}/index/*"
      ]
    }]
  })
}

# IAM policy — allows writing to the tickets table
resource "aws_iam_policy" "dynamodb_tickets_write" {
  name = "ticketmaster-tickets-write-${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["dynamodb:PutItem"]
      Resource = aws_dynamodb_table.tickets.arn
    }]
  })
}

# IAM policy — allows deleting from the tickets table
resource "aws_iam_policy" "dynamodb_tickets_delete" {
  name = "ticketmaster-tickets-delete-${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["dynamodb:DeleteItem"]
      Resource = aws_dynamodb_table.tickets.arn
    }]
  })
}

resource "aws_dynamodb_table" "orders" {
  name         = "ticketmaster-orders-${var.environment}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "userId"
    type = "S"
  }

  global_secondary_index {
    name            = "userId-index"
    hash_key        = "userId"
    projection_type = "ALL"
  }
}

# IAM policy — allows reading from the orders table (including GSI)
resource "aws_iam_policy" "dynamodb_orders_read" {
  name = "ticketmaster-orders-read-${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = ["dynamodb:GetItem", "dynamodb:Query"]
      Resource = [
        aws_dynamodb_table.orders.arn,
        "${aws_dynamodb_table.orders.arn}/index/*"
      ]
    }]
  })
}

# IAM policy — allows writing to the orders table
resource "aws_iam_policy" "dynamodb_orders_write" {
  name = "ticketmaster-orders-write-${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["dynamodb:PutItem", "dynamodb:UpdateItem"]
      Resource = aws_dynamodb_table.orders.arn
    }]
  })
}

# IAM policy — allows transactional writes across orders and tickets tables
resource "aws_iam_policy" "dynamodb_orders_transact" {
  name = "ticketmaster-orders-transact-${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = ["dynamodb:PutItem", "dynamodb:UpdateItem"]
      Resource = [
        aws_dynamodb_table.orders.arn,
        aws_dynamodb_table.tickets.arn
      ]
    }]
  })
}

# IAM policy — allows deleting from the events table
resource "aws_iam_policy" "dynamodb_events_delete" {
  name = "ticketmaster-events-delete-${var.environment}"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["dynamodb:DeleteItem"]
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

  additional_policy_arns = [aws_iam_policy.dynamodb_events_read.arn]

  environment_variables = {
    ENVIRONMENT  = var.environment
    EVENTS_TABLE = aws_dynamodb_table.events.name
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

module "get_event_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-get-event-${var.environment}"
  handler       = "org.example.handlers.GetEventHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/get-event/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_events_read.arn]

  environment_variables = {
    ENVIRONMENT  = var.environment
    EVENTS_TABLE = aws_dynamodb_table.events.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "GET /events/{id}"
}

module "update_event_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-update-event-${var.environment}"
  handler       = "org.example.handlers.UpdateEventHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/update-event/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_events_read.arn, aws_iam_policy.dynamodb_events_write.arn]

  environment_variables = {
    ENVIRONMENT  = var.environment
    EVENTS_TABLE = aws_dynamodb_table.events.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "PUT /events/{id}"
}

module "delete_event_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-delete-event-${var.environment}"
  handler       = "org.example.handlers.DeleteEventHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/delete-event/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_events_read.arn, aws_iam_policy.dynamodb_events_delete.arn]

  environment_variables = {
    ENVIRONMENT  = var.environment
    EVENTS_TABLE = aws_dynamodb_table.events.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "DELETE /events/{id}"
}

# ---------------------------------------------------------------------------
# Ticket Lambda functions
# ---------------------------------------------------------------------------

module "get_tickets_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-get-tickets-${var.environment}"
  handler       = "org.example.handlers.GetTicketsHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/get-tickets/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_tickets_read.arn]

  environment_variables = {
    ENVIRONMENT   = var.environment
    TICKETS_TABLE = aws_dynamodb_table.tickets.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "GET /events/{eventId}/tickets"
}

module "create_ticket_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-create-ticket-${var.environment}"
  handler       = "org.example.handlers.CreateTicketHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/create-ticket/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_tickets_write.arn]

  environment_variables = {
    ENVIRONMENT   = var.environment
    TICKETS_TABLE = aws_dynamodb_table.tickets.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "POST /events/{eventId}/tickets"
}

module "get_ticket_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-get-ticket-${var.environment}"
  handler       = "org.example.handlers.GetTicketHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/get-ticket/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_tickets_read.arn]

  environment_variables = {
    ENVIRONMENT   = var.environment
    TICKETS_TABLE = aws_dynamodb_table.tickets.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "GET /tickets/{id}"
}

module "update_ticket_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-update-ticket-${var.environment}"
  handler       = "org.example.handlers.UpdateTicketHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/update-ticket/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_tickets_read.arn, aws_iam_policy.dynamodb_tickets_write.arn]

  environment_variables = {
    ENVIRONMENT   = var.environment
    TICKETS_TABLE = aws_dynamodb_table.tickets.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "PUT /tickets/{id}"
}

module "delete_ticket_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-delete-ticket-${var.environment}"
  handler       = "org.example.handlers.DeleteTicketHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/delete-ticket/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_tickets_read.arn, aws_iam_policy.dynamodb_tickets_delete.arn]

  environment_variables = {
    ENVIRONMENT   = var.environment
    TICKETS_TABLE = aws_dynamodb_table.tickets.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "DELETE /tickets/{id}"
}

# ---------------------------------------------------------------------------
# Order Lambda functions
# ---------------------------------------------------------------------------

module "create_order_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-create-order-${var.environment}"
  handler       = "org.example.handlers.CreateOrderHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/create-order/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_orders_transact.arn, aws_iam_policy.dynamodb_tickets_read.arn]

  environment_variables = {
    ENVIRONMENT   = var.environment
    ORDERS_TABLE  = aws_dynamodb_table.orders.name
    TICKETS_TABLE = aws_dynamodb_table.tickets.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "POST /orders"
}

module "get_order_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-get-order-${var.environment}"
  handler       = "org.example.handlers.GetOrderHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/get-order/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_orders_read.arn]

  environment_variables = {
    ENVIRONMENT   = var.environment
    ORDERS_TABLE  = aws_dynamodb_table.orders.name
    TICKETS_TABLE = aws_dynamodb_table.tickets.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "GET /orders/{id}"
}

module "get_user_orders_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-get-user-orders-${var.environment}"
  handler       = "org.example.handlers.GetUserOrdersHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/get-user-orders/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_orders_read.arn]

  environment_variables = {
    ENVIRONMENT   = var.environment
    ORDERS_TABLE  = aws_dynamodb_table.orders.name
    TICKETS_TABLE = aws_dynamodb_table.tickets.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "GET /users/{userId}/orders"
}

module "cancel_order_lambda" {
  source = "../../modules/lambda"

  function_name = "ticketmaster-cancel-order-${var.environment}"
  handler       = "org.example.handlers.CancelOrderHandler::handleRequest"
  s3_bucket     = aws_s3_bucket.lambda_artifacts.bucket
  s3_key        = "functions/cancel-order/function.jar"
  environment   = var.environment

  layer_arns             = [aws_lambda_layer_version.dependencies.arn]
  additional_policy_arns = [aws_iam_policy.dynamodb_orders_transact.arn, aws_iam_policy.dynamodb_orders_read.arn]

  environment_variables = {
    ENVIRONMENT   = var.environment
    ORDERS_TABLE  = aws_dynamodb_table.orders.name
    TICKETS_TABLE = aws_dynamodb_table.tickets.name
  }

  api_gateway_id            = aws_apigatewayv2_api.this.id
  api_gateway_execution_arn = aws_apigatewayv2_api.this.execution_arn
  api_route_key             = "POST /orders/{id}/cancel"
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
