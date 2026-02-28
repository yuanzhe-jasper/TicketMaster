output "lambda_artifacts_bucket" {
  description = "S3 bucket used to store Lambda deployment JARs"
  value       = aws_s3_bucket.lambda_artifacts.bucket
}

output "get_events_lambda_name" {
  description = "Name of the get-events Lambda function"
  value       = module.get_events_lambda.function_name
}

output "get_events_lambda_arn" {
  description = "ARN of the get-events Lambda function"
  value       = module.get_events_lambda.function_arn
}

output "api_endpoint" {
  description = "Base URL of the HTTP API Gateway"
  value       = aws_apigatewayv2_api.this.api_endpoint
}

output "get_events_url" {
  description = "Full URL for GET /events"
  value       = "${aws_apigatewayv2_api.this.api_endpoint}/events"
}
