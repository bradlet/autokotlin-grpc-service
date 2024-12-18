variable "service_allow_all_ingress" {
  description = "Allow all ingress traffic"
  type        = bool
  default     = true
}

variable "service_image" {
    description = "The image to deploy to Cloud Run"
    type        = string
    default     = "bradlet2/autokotlin-grpc-service"
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

resource "google_cloud_run_v2_service" "main" {
  name     = "autokotlin-service"
  location = "us-central1"
  deletion_protection = false
  ingress = var.service_allow_all_ingress ? "INGRESS_TRAFFIC_ALL" : "INGRESS_TRAFFIC_INTERNAL_ONLY"

  template {
    containers {
      image = var.service_image
      resources {
        limits = {
          cpu    = var.service_cpu
          memory = var.service_memory
        }
      }

      env = [
        for key, value in var.service_env : {
          name  = key
          value = value
        }
      ]
    }
  }
}