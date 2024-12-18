# autokotlin-grpc-service
Practice repository for creating a Kotlin GRPC service template

## Tutorial / Articles followed
- [For Workload Identity Federation](https://cloud.google.com/blog/products/identity-security/secure-your-use-of-third-party-tools-with-identity-federation)
- [For Cloud Run Deployments via GHA](https://cloud.google.com/blog/products/devops-sre/deploy-to-cloud-run-with-github-actions/)
- [For WIF between Terraform Cloud and GCP](https://developer.hashicorp.com/terraform/cloud-docs/workspaces/dynamic-provider-credentials/gcp-configuration)

## Setup
It's assumed that you have created a workspace for this project in Terraform Cloud (uses a TFC remote backend).
The article `For WIF between Terraform Cloud and GCP` should be followed to set up TFC to use Workload Identity 
Federation for authentication with Google Cloud. Additionally, you should have made a team within a TFC org, created a 
team API token, and should have added it to the GitHub repo as a secret.

Some values are hardcoded, especially in the `infra` directory and GitHub workflows. As a precursor, it's assumed that
you know which would need to be replaced with your own values.

## Infrastructure Bootstrapping
This project requires some initial bootstrapping before Workload Identity Federation can be used for auth.
1. Login to gcloud cli `gcloud auth application-default login --scopes=https://www.googleapis.com/auth/cloud-platform`.
2. Get a temporary oauth token `gcloud auth print-access-token`.
3. Add the token as a workspace environment variable called `GOOGLE_OAUTH_ACCESS_TOKEN` in Terraform Cloud.
4. Login to Terraform Cloud with `terraform login`.
5. Apply the terraform configuration in the `infra` directory manually once (`gradle tfa`).

## Manually Enabled GCP APIs
- IAM Service Account Credentials API
- Identity and Access Management (IAM) API
- Cloud Resource Manager API
- Cloud Run Admin API