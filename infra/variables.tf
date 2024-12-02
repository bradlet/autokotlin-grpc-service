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

variable "tfc_workspace_id" { // not name!
    description = "The Terraform Cloud workspace ID"
    type        = string
    default     = "ws-H5RiDyUAgVZfYhxe"
}

variable "tfc_organization_id" {
    description = "The Terraform Cloud organization ID"
    type        = string
    default     = "org-7u9DjkKnvG5oU5cy"
}
