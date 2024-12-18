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
  description = "The port to expose the service on"
  type        = number
  default     = 6565
}

resource "google_cloud_run_v2_service" "main" {
  name                = "autokotlin-service"
  location            = "us-central1"
  deletion_protection = false
  ingress             = var.service_allow_all_ingress ? "INGRESS_TRAFFIC_ALL" : "INGRESS_TRAFFIC_INTERNAL_ONLY"

  template {
    service_account = google_service_account.cloud_run_service_account.email

    scaling {
      min_instance_count = var.service_min_instances
      max_instance_count = var.service_max_instances
    }

    containers {
      image = var.service_image

      ports {
        container_port = var.service_port
      }

      resources {
        limits = {
          cpu    = var.service_cpu
          memory = var.service_memory
        }
      }

      dynamic "env" {
        for_each = var.service_env
        content {
          name  = env.key
          value = env.value
        }
      }
    }
  }

  depends_on = [google_project_iam_member.cloud_run_sa_admin]
}