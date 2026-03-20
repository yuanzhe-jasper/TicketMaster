# IAM role assumed by the Lambda function
resource "aws_iam_role" "this" {
  name = "${var.function_name}-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "lambda.amazonaws.com" }
        Action    = "sts:AssumeRole"
      }
    ]
  })
}

# Grants Lambda permission to write logs to CloudWatch
resource "aws_iam_role_policy_attachment" "basic_execution" {
  role       = aws_iam_role.this.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# Any extra policies (e.g. DynamoDB read/write) passed in by the caller
resource "aws_iam_role_policy_attachment" "additional" {
  count      = length(var.additional_policy_arns)
  role       = aws_iam_role.this.name
  policy_arn = var.additional_policy_arns[count.index]
}

# Pre-create the log group so we control the retention period
resource "aws_cloudwatch_log_group" "this" {
  name              = "/aws/lambda/${var.function_name}"
  retention_in_days = 14
}

resource "aws_lambda_function" "this" {
  function_name = var.function_name
  role          = aws_iam_role.this.arn
  handler       = var.handler
  runtime       = var.runtime
  memory_size   = var.memory_size
  timeout       = var.timeout

  # Deployment package is a JAR uploaded to S3
  s3_bucket = var.s3_bucket
  s3_key    = var.s3_key

  layers = var.layer_arns

  dynamic "environment" {
    for_each = length(var.environment_variables) > 0 ? [1] : []
    content {
      variables = var.environment_variables
    }
  }

  depends_on = [
    aws_iam_role_policy_attachment.basic_execution,
    aws_cloudwatch_log_group.this,
  ]
}

# ---------------------------------------------------------------------------
# Optional API Gateway integration
# ---------------------------------------------------------------------------

resource "aws_apigatewayv2_integration" "this" {
  count = var.api_gateway_id != null ? 1 : 0

  api_id                 = var.api_gateway_id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.this.invoke_arn
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "this" {
  count = var.api_gateway_id != null ? 1 : 0

  api_id    = var.api_gateway_id
  route_key = var.api_route_key
  target    = "integrations/${aws_apigatewayv2_integration.this[0].id}"
}

resource "aws_lambda_permission" "api_gateway" {
  count = var.api_gateway_id != null ? 1 : 0

  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.this.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${var.api_gateway_execution_arn}/*/*"
}
