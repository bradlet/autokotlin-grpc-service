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

/*
  Setup Github Action <-> GCP Workload Identity Federation
  This will enable cloud run deployments in our pipeline.
*/


resource "google_iam_workload_identity_pool" "github_actions" {
  workload_identity_pool_id = "autokotlin-gha"
  display_name              = "Autokotlin GHA"
  description               = "Pool used to authenticate Github Action runs for the Autokotlin project."
}

resource "google_iam_workload_identity_pool_provider" "github_actions" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.github_actions.workload_identity_pool_id
  workload_identity_pool_provider_id = "gha-pool"
  display_name                       = "Github Actions Pool Provider"
  description                        = "GitHub Actions identity pool provider for continuous deployment pipeline."
  oidc {
    issuer_uri = "https://token.actions.GitHubusercontent.com/"
  }
  attribute_mapping = {
    "google.subject"             = "assertion.sub",
    "attribute.repository"       = "assertion.repository",
    "attribute.repository_owner" = "assertion.repository_owner",
    "attribute.branch"           = "assertion.sub.extract('/heads/{branch}/')"
  }
  attribute_condition = "assertion.repository_owner=='bradlet'"
}

// For simplicity, the service account is created as an Owner on the project. Not recommended for security.
resource "google_service_account" "gha_service_account" {
  account_id   = "gha-gcloud-sa-autokotlin"
  display_name = "Terraform-Provisioned Service Account for Autokotlin GHA"
}

// Allow any principal with the correct repository attribute to impersonate the service account
resource "google_service_account_iam_binding" "impersonate_gha_service_account_binding" {
  service_account_id = google_service_account.gha_service_account.name
  role               = "roles/owner"

  members = [
    "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github_actions.name}/attribute.repository/${var.repository}"
  ]

  depends_on = [google_iam_workload_identity_pool_provider.github_actions]
}

/*
  Setup Terraform Cloud <-> GCP Workload Identity Federation
  This will enable Terraform Cloud interactions (plan / apply) in our pipeline.
*/

resource "google_iam_workload_identity_pool" "terraform_cloud" {
  workload_identity_pool_id = "autokotlin-tfc"
  display_name              = "Autokotlin TFC"
  description               = "Pool used to authenticate Terraform Cloud on the Autokotlin project."
}

resource "google_iam_workload_identity_pool_provider" "terraform_cloud" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.terraform_cloud.workload_identity_pool_id
  workload_identity_pool_provider_id = "tfc-pool"
  display_name                       = "Terraform Cloud Pool Provider"
  description                        = "Terraform Cloud identity pool provider for continuous deployment pipeline."

  attribute_mapping = {
    "attribute.tfc_organization_id" = "assertion.terraform_organization_id"
    "attribute.tfc_project_id"      = "assertion.terraform_project_id"
    "attribute.tfc_project_name"    = "assertion.terraform_project_name"
    "google.subject"                = "assertion.terraform_workspace_id"
    "attribute.tfc_workspace_name"  = "assertion.terraform_workspace_name"
    # Note: This env attribute doesn't work for my naming convention but leaving it here for reference.
    "attribute.tfc_workspace_env"   = "assertion.terraform_workspace_name.split('-')[assertion.terraform_workspace_name.split('-').size() -1]"
  }

  oidc {
    issuer_uri = "https://app.terraform.io"
  }

  attribute_condition = "attribute.tfc_organization_id == '${var.tfc_organization_id}'"
  # If we want to make this prod only:
#   attribute_condition = "attribute.tfc_organization_id == 'org-7u9DjkKnvG5oU5cy' && attribute.tfc_workspace_env.startsWith ( 'prod')'"
}

resource "google_service_account" "tfc_service_account" {
  account_id = "tfc-terraform-sa-autokotlin"
  display_name = "Terraform-Provisioned Service Account for Autokotlin TFC"
}
resource "google_service_account_iam_binding" "sa_tf_banking_dev_iam" {
  service_account_id = google_service_account.tfc_service_account.name
  role               = "roles/owner"
  members = [
    // Notice workspace id was mapped to google.subject above
    "principal://iam.googleapis.com/${google_iam_workload_identity_pool.terraform_cloud.name}/subject/${var.tfc_workspace_id}",
  ]

  depends_on = [google_iam_workload_identity_pool_provider.terraform_cloud]
}

output "gha_workload_identity_provider" {
  value       = google_iam_workload_identity_pool_provider.github_actions.id
  description = "The ID of the workload identity pool provider for Github Actions."
}

output "gha_service_account_email" {
  value       = google_service_account.gha_service_account.email
  description = "The email address of the service account used by Github Actions."
}

output "tfc_workload_identity_provider" {
  value       = google_iam_workload_identity_pool_provider.terraform_cloud.id
  description = "The ID of the workload identity pool provider for Terraform Cloud."
}

output "tfc_service_account_email" {
  value       = google_service_account.tfc_service_account.email
  description = "The email address of the service account used by Terraform Cloud."
}
