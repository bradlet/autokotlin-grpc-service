terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "6.12.0"
    }
  }

  backend "remote" {
    hostname     = "app.terraform.io"
    organization = "bradlet"

    workspaces {
      name = "autokotlin-grpc-service"
    }
  }
}

provider "google" {
  project = var.project_id
}

resource "google_iam_workload_identity_pool" "github_actions" {
  workload_identity_pool_id = "autokotlin-gha"
  display_name = "Autokotlin GHA"
  description = "Pool used to authenticate Github Action runs for the Autokotlin project."
}

resource "google_iam_workload_identity_pool_provider" "github_actions" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.github_actions.workload_identity_pool_id
  workload_identity_pool_provider_id = "gha-pool"
  display_name                       = "Github Actions Pool"
  description                        = "GitHub Actions identity pool provider for continuous deployment pipeline."
  oidc {
    issuer_uri = "https://token.actions.GitHubusercontent.com/"
  }
  attribute_mapping = <<EOT
  google.subject=assertion.sub,
  attribute.repository=assertion.repository,
  attribute.repository_owner=assertion.repository_owner,
  attribute.branch=assertion.sub.extract('/heads/{branch}/')
EOT
  attribute_condition = "assertion.repository_owner=='bradlet'"
}

// For simplicity, the service account is created as an Owner on the project. Not recommended for security.
resource "google_service_account" "terraform_service_account" {
  account_id   = "gha-terraform-sa-autokotlin"
  display_name = "Terraform Service Account for Autokotlin GHA"
}

// Allow any principal with the correct repository attribute to impersonate the service account
resource "google_service_account_iam_binding" "impersonate_service_account_binding" {
  service_account_id = google_service_account.terraform_service_account.name
  role    = "roles/owner"

  members = [
    "principalSet://iam.googleapis.com/projects/${var.project_number}/locations/global/workloadIdentityPools/GitHub-actions-pool/attribute.repository/${var.repository}"
  ]
}
