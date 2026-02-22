output "function_name" {
  description = "Name of the Lambda function"
  value       = aws_lambda_function.this.function_name
}

output "function_arn" {
  description = "ARN of the Lambda function"
  value       = aws_lambda_function.this.arn
}

output "invoke_arn" {
  description = "Invoke ARN used by API Gateway to trigger this Lambda"
  value       = aws_lambda_function.this.invoke_arn
}

output "role_name" {
  description = "Name of the Lambda execution IAM role"
  value       = aws_iam_role.this.name
}

output "role_arn" {
  description = "ARN of the Lambda execution IAM role"
  value       = aws_iam_role.this.arn
}