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
