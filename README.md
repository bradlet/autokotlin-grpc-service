# autokotlin-grpc-service
Practice repository for creating a Kotlin GRPC service template

## Tutorial / Articles followed
- [For Workload Identity Federation](https://cloud.google.com/blog/products/identity-security/secure-your-use-of-third-party-tools-with-identity-federation)
- [For Cloud Run Deployments via GHA](https://cloud.google.com/blog/products/devops-sre/deploy-to-cloud-run-with-github-actions/)

## Setup
It's assumed that you have created a workspace for this project in Terraform Cloud (uses a TFC remote backend).


## Infrastructure Bootstrapping
This project requires some initial bootstrapping before Workload Identity Federation can be used for auth.
1. Login to gcloud cli `gcloud auth login`.
2. Get a temporary oauth token `gcloud auth print-identity-token`.
3. Add the token as an environment variable called `GOOGLE_OAUTH_ACCESS_TOKEN` in Terraform Cloud.
4. Apply the terraform configuration in the `infra` directory manually once (`gradle tfa`).