terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "6.12.0"
    }
  }

  # State lives in the GCS bucket created by scripts/bootstrap/setup-project.sh.
  # REPLACE_ME: set this to the bucket name printed at the end of the bootstrap script
  # (format: "<PROJECT_ID>-tf-state").
  backend "gcs" {
    bucket = "REPLACE_ME-tf-state"
    prefix = "terraform/state"
  }
}

provider "google" {
  project = var.project_id
}

# Service account that the Cloud Run service itself runs as (NOT the GHA deploy SA;
# that one is provisioned by setup-project.sh).
resource "google_service_account" "cloud_run_service_account" {
  account_id   = "autokotlin-cloud-run-sa"
  display_name = "Service account that runs the Autokotlin Cloud Run service."
}

resource "google_project_iam_member" "cloud_run_sa_admin" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${google_service_account.cloud_run_service_account.email}"
}
