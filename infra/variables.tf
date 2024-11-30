variable "project_id" {
  description = "The GCP Project ID"
  type        = string
  default     = "thompson-projects"
}

variable "project_number" {
  description = "The GCP Project Number"
  type        = string
  default     = "194504085959"
}

variable "repository" {
    description = "The fully-qualified repository name (org + repo)"
    type        = string
    default     = "bradlet/autokotlin-grpc-service"
}
