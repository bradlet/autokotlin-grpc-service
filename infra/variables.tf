# Project-identity vars. No defaults — the deploy workflow writes these into
# infra/terraform.auto.tfvars from its top-level env block. For local runs,
# create infra/terraform.tfvars (gitignored) with your own values.

variable "project_id" {
  description = "The GCP Project ID"
  type        = string
}

variable "project_number" {
  description = "The GCP Project Number"
  type        = string
}

# -----------------------------------------------------------------------------
# Cloud Run service configuration
# -----------------------------------------------------------------------------

variable "service_allow_all_ingress" {
  description = "Allow all ingress traffic"
  type        = bool
  default     = true
}

variable "service_image" {
  description = "The image to deploy to Cloud Run. CI sets this via terraform.auto.tfvars from the jib build output."
  type        = string
  # REPLACE_ME: only used for laptop-side `terraform plan` runs. CI always overrides.
  default     = "REPLACE_ME-docker.pkg.dev/REPLACE_ME-project/REPLACE_ME-repo/ktor-app:latest"
}

variable "service_cpu" {
  description = "The CPU limit for the service: 1, 2, 4 & 8 supported"
  type        = string
  default     = "1"
}

variable "service_memory" {
  description = "The memory limit for the service"
  type        = string
  default     = "512Mi"
}

variable "service_cpu_boost" {
  description = "Whether CPU should be boosted on startup of a new container."
  type        = bool
  default     = false
}

variable "service_env" {
  description = "The environment variables to set for the service"
  type        = map(string)
  default     = {}
}

variable "service_min_instances" {
  description = "The minimum number of instances to run"
  type        = number
  default     = 0
}

variable "service_max_instances" {
  description = "The maximum number of instances to run"
  type        = number
  default     = 1
}

variable "service_annotations" {
  description = "The annotations to set for the service. Does not support: run.googleapis.com, cloud.googleapis.com, serving.knative.dev, or autoscaling.knative.dev"
  type        = map(string)
  default     = {}
}

variable "service_port" {
  description = "The port to expose the service on. Matches the ktor-app's default in src/main/resources/application.conf."
  type        = number
  default     = 8080
}
