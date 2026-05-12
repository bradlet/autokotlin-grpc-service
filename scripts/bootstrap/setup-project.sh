#!/bin/bash

################################################################################
# GCP Project Bootstrap Script
################################################################################
#
# This script bootstraps a GCP project with the infrastructure needed to run
# Terraform via GitHub Actions. It should be run ONCE by a project owner after
# manually creating the GCP project.
#
# Prerequisites:
#   - GCP project must already exist
#   - User must have Owner role on the project
#   - gcloud CLI must be installed and authenticated
#
# What this script does:
#   1. Enables required GCP APIs
#   2. Creates a GCS bucket for Terraform state
#   3. Creates Workload Identity Federation resources for GitHub Actions
#   4. Creates a service account with proper IAM bindings for GHA deployments
#
# Usage:
#   ./setup-project.sh PROJECT_ID PROJECT_NAME REPOSITORY USERS
#
# Arguments (all positional, all required in practice — defaults are fake placeholders
# so the script fails clearly if you forget one):
#   PROJECT_ID    - GCP Project ID (e.g. my-app-prod)
#   PROJECT_NAME  - Short name for resource naming (e.g. my-app)
#   REPOSITORY    - GitHub repository in "owner/repo" form (e.g. me/my-app)
#   USERS         - Comma-separated list of users to grant bucket access (e.g. you@example.com)
#
################################################################################

set -euo pipefail  # Exit on error, undefined vars, and pipe failures

# -----------------------------------------------------------------------------
# Configuration & Default Values
# -----------------------------------------------------------------------------

PROJECT_ID="${1:-your-project-id}"
PROJECT_NAME="${2:-your-project-name}"
REPOSITORY="${3:-your-org/your-repo}"
USERS="${4:-you@example.com}"
LOCATION="us-west1"

# Extract repository owner from the full repository name
REPO_OWNER=$(echo "$REPOSITORY" | cut -d'/' -f1)

# Resource names
TF_STATE_BUCKET="${PROJECT_ID}-tf-state"
WIF_POOL_ID="${PROJECT_NAME}-gha"
WIF_PROVIDER_ID="${PROJECT_NAME}-gha-pool"
SERVICE_ACCOUNT_ID="gha-gcloud-sa-${PROJECT_NAME}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# -----------------------------------------------------------------------------
# Helper Functions
# -----------------------------------------------------------------------------

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_section() {
    echo ""
    echo "================================================================================"
    echo "$1"
    echo "================================================================================"
    echo ""
}

# Check if a GCP API is enabled
is_api_enabled() {
    local api=$1
    gcloud services list --enabled --project="$PROJECT_ID" \
        --filter="config.name:$api" --format="value(config.name)" 2>/dev/null | grep -q "$api"
}

# -----------------------------------------------------------------------------
# Validation
# -----------------------------------------------------------------------------

log_section "Validating Prerequisites"

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    log_error "gcloud CLI is not installed. Please install it first."
    exit 1
fi

# Check if user is authenticated
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
    log_error "No active gcloud authentication found. Please run 'gcloud auth login'"
    exit 1
fi

# Verify project exists and user has access
if ! gcloud projects describe "$PROJECT_ID" &> /dev/null; then
    log_error "Project '$PROJECT_ID' does not exist or you don't have access to it."
    exit 1
fi

log_info "Using project: $PROJECT_ID"
log_info "Project name: $PROJECT_NAME"
log_info "Repository: $REPOSITORY"
log_info "Repository owner: $REPO_OWNER"
log_info "Users to grant bucket access: $USERS"

# Set the active project
gcloud config set project "$PROJECT_ID"

# -----------------------------------------------------------------------------
# Enable Required APIs
# -----------------------------------------------------------------------------

log_section "Enabling Required GCP APIs"

# List of APIs required for this infrastructure
REQUIRED_APIS=(
    "cloudresourcemanager.googleapis.com"  # For project management
    "iam.googleapis.com"                   # For IAM and service accounts
    "iamcredentials.googleapis.com"        # For workload identity federation
    "storage.googleapis.com"               # For GCS bucket
)

for api in "${REQUIRED_APIS[@]}"; do
    if is_api_enabled "$api"; then
        log_info "API already enabled: $api"
    else
        log_info "Enabling API: $api"
        gcloud services enable "$api" --project="$PROJECT_ID"
    fi
done

log_info "All required APIs are enabled"

# -----------------------------------------------------------------------------
# Create GCS Bucket for Terraform State
# -----------------------------------------------------------------------------

log_section "Creating GCS Bucket for Terraform State"

# Check if bucket already exists
if gsutil ls -p "$PROJECT_ID" "gs://${TF_STATE_BUCKET}" &> /dev/null; then
    log_warn "Bucket gs://${TF_STATE_BUCKET} already exists. Skipping creation."
else
    log_info "Creating bucket: gs://${TF_STATE_BUCKET}"

    # Create bucket with versioning enabled for state file safety
    gsutil mb -p "$PROJECT_ID" -l "$LOCATION" "gs://${TF_STATE_BUCKET}"
    gsutil versioning set on "gs://${TF_STATE_BUCKET}"

    # Enable uniform bucket-level access for better security
    gsutil uniformbucketlevelaccess set on "gs://${TF_STATE_BUCKET}"

    log_info "Bucket created successfully with versioning enabled"
fi

# -----------------------------------------------------------------------------
# Create Workload Identity Pool
# -----------------------------------------------------------------------------

log_section "Creating Workload Identity Pool for GitHub Actions"

# Check if workload identity pool already exists
if gcloud iam workload-identity-pools describe "$WIF_POOL_ID" \
    --location="global" \
    --project="$PROJECT_ID" &> /dev/null; then
    log_warn "Workload identity pool '$WIF_POOL_ID' already exists. Skipping creation."
else
    log_info "Creating workload identity pool: $WIF_POOL_ID"

    gcloud iam workload-identity-pools create "$WIF_POOL_ID" \
        --location="global" \
        --display-name="${PROJECT_NAME} GHA" \
        --description="Pool used to authenticate Github Action runs for the ${PROJECT_NAME} project." \
        --project="$PROJECT_ID"

    log_info "Workload identity pool created successfully"
fi

# Get the full pool resource name
WIF_POOL_NAME=$(gcloud iam workload-identity-pools describe "$WIF_POOL_ID" \
    --location="global" \
    --project="$PROJECT_ID" \
    --format="value(name)")

log_info "Pool resource name: $WIF_POOL_NAME"

# -----------------------------------------------------------------------------
# Create Workload Identity Pool Provider
# -----------------------------------------------------------------------------

log_section "Creating Workload Identity Pool Provider for GitHub Actions"

# Check if provider already exists
if gcloud iam workload-identity-pools providers describe "$WIF_PROVIDER_ID" \
    --workload-identity-pool="$WIF_POOL_ID" \
    --location="global" \
    --project="$PROJECT_ID" &> /dev/null; then
    log_warn "Workload identity pool provider '$WIF_PROVIDER_ID' already exists. Skipping creation."
else
    log_info "Creating workload identity pool provider: $WIF_PROVIDER_ID"

    # Create the OIDC provider for GitHub Actions
    gcloud iam workload-identity-pools providers create-oidc "$WIF_PROVIDER_ID" \
        --workload-identity-pool="$WIF_POOL_ID" \
        --location="global" \
        --issuer-uri="https://token.actions.githubusercontent.com/" \
        --attribute-mapping="google.subject=assertion.sub,attribute.aud=assertion.aud,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner,attribute.branch=assertion.sub.extract('/heads/{branch}/')" \
        --attribute-condition="assertion.repository_owner=='${REPO_OWNER}'" \
        --display-name="Github Actions Pool Provider" \
        --description="GitHub Actions identity pool provider for continuous deployment pipeline." \
        --project="$PROJECT_ID"

    log_info "Workload identity pool provider created successfully"
fi

sleep 5

# Get the full provider resource name
WIF_PROVIDER_NAME=$(gcloud iam workload-identity-pools providers describe "$WIF_PROVIDER_ID" \
    --workload-identity-pool="$WIF_POOL_ID" \
    --location="global" \
    --project="$PROJECT_ID" \
    --format="value(name)")

log_info "Provider resource name: $WIF_PROVIDER_NAME"

# -----------------------------------------------------------------------------
# Create Service Account
# -----------------------------------------------------------------------------

log_section "Creating Service Account for GitHub Actions"

SERVICE_ACCOUNT_EMAIL="${SERVICE_ACCOUNT_ID}@${PROJECT_ID}.iam.gserviceaccount.com"

# Check if service account already exists
if gcloud iam service-accounts describe "$SERVICE_ACCOUNT_EMAIL" \
    --project="$PROJECT_ID" &> /dev/null; then
    log_warn "Service account '$SERVICE_ACCOUNT_EMAIL' already exists. Skipping creation."
else
    log_info "Creating service account: $SERVICE_ACCOUNT_EMAIL"

    gcloud iam service-accounts create "$SERVICE_ACCOUNT_ID" \
        --display-name="Terraform-Provisioned Service Account for ${PROJECT_NAME} GHA" \
        --project="$PROJECT_ID"

    log_info "Service account created successfully"
fi

# -----------------------------------------------------------------------------
# Configure IAM Bindings
# -----------------------------------------------------------------------------

log_section "Configuring IAM Bindings"

# Grant workloadIdentityUser role to allow the workload identity pool to impersonate the service account
log_info "Granting workloadIdentityUser role to workload identity pool..."

gcloud iam service-accounts add-iam-policy-binding "$SERVICE_ACCOUNT_EMAIL" \
    --role="roles/iam.workloadIdentityUser" \
    --member="principalSet://iam.googleapis.com/${WIF_POOL_NAME}/*" \
    --project="$PROJECT_ID" \
    --condition=None

log_info "Workload identity binding configured"

# Grant Owner role to the service account on the project
# NOTE: This is for simplicity and is admittedly not a security best practice
log_warn "Granting Owner role to service account..."

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/owner" \
    --condition=None

log_info "Owner role granted to service account"

# Grant Storage Admin role to the service account on the state bucket
# This is necessary because UBLA (Uniform Bucket-Level Access) is enabled
log_info "Granting Storage Admin role to service account on Terraform state bucket..."

gcloud storage buckets add-iam-policy-binding "gs://${TF_STATE_BUCKET}" \
    --member="serviceAccount:${SERVICE_ACCOUNT_EMAIL}" \
    --role="roles/storage.admin" \
    --project="$PROJECT_ID"

log_info "Service account granted full access to state bucket"

# Parse comma-separated users list and grant Storage Admin role to each
IFS=',' read -ra USER_ARRAY <<< "$USERS"

log_info "Granting Storage Admin role to ${#USER_ARRAY[@]} user(s) on Terraform state bucket..."

for user in "${USER_ARRAY[@]}"; do
    # Trim whitespace
    user=$(echo "$user" | xargs)

    if [[ -n "$user" ]]; then
        log_info "Granting access to user: $user"
        gcloud storage buckets add-iam-policy-binding "gs://${TF_STATE_BUCKET}" \
            --member="user:${user}" \
            --role="roles/storage.admin" \
            --project="$PROJECT_ID"
    fi
done

log_info "Bucket IAM permissions configured successfully"

# -----------------------------------------------------------------------------
# Output Summary
# -----------------------------------------------------------------------------

log_section "Bootstrap Complete!"

echo "Project ID:                 $PROJECT_ID"
echo "Project Name:               $PROJECT_NAME"
echo "Terraform State Bucket:     gs://${TF_STATE_BUCKET}"
echo ""
echo "Workload Identity Pool:     $WIF_POOL_ID"
echo "Pool Provider:              $WIF_PROVIDER_ID"
echo "Service Account:            $SERVICE_ACCOUNT_EMAIL"
echo ""
echo "Provider ID (for GHA):      $WIF_PROVIDER_NAME"
echo ""

log_info "Use the following values in your GitHub Actions workflow:"
echo ""
echo "  workload_identity_provider: '${WIF_PROVIDER_NAME}'"
echo "  service_account: '${SERVICE_ACCOUNT_EMAIL}'"
echo ""

log_info "Use the following backend configuration in your Terraform:"
echo ""
echo "  terraform {"
echo "    backend \"gcs\" {"
echo "      bucket = \"${TF_STATE_BUCKET}\""
echo "      prefix = \"terraform/state\""
echo "    }"
echo "  }"
echo ""

log_section "Next Steps"
echo "1. Update your GitHub Actions workflow with the workload identity provider and service account"
echo "2. Update your Terraform backend configuration to use the GCS bucket"
echo "3. Run 'terraform init' to setup/migrate state on/to the new backend"
echo ""
