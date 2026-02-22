variable "function_name" {
  description = "Name of the Lambda function"
  type        = string
}

variable "handler" {
  description = "Lambda function handler (e.g. org.example.Handler::handleRequest)"
  type        = string
}

variable "runtime" {
  description = "Lambda runtime"
  type        = string
  default     = "java21"
}

variable "memory_size" {
  description = "Amount of memory in MB"
  type        = number
  default     = 512
}

variable "timeout" {
  description = "Timeout in seconds"
  type        = number
  default     = 30
}

variable "s3_bucket" {
  description = "S3 bucket that holds the Lambda deployment package"
  type        = string
}

variable "s3_key" {
  description = "S3 object key for the Lambda deployment package (JAR)"
  type        = string
}

variable "environment_variables" {
  description = "Environment variables to pass to the Lambda function"
  type        = map(string)
  default     = {}
}

variable "additional_policy_arns" {
  description = "Extra IAM policy ARNs to attach to the Lambda execution role (e.g. DynamoDB access)"
  type        = list(string)
  default     = []
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
}
