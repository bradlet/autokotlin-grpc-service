# autokotlin-service

Template repo for a JVM service on GCP Cloud Run. Brings together: a Ktor HTTP app with OpenAPI-driven codegen and Swagger UI, a gRPC app, a shared library module, Jib container builds, a GCS-backed Terraform deploy, and a one-shot bootstrap script that provisions Workload Identity Federation for GitHub Actions.

## Modules

| Module | Purpose |
| --- | --- |
| [lib/](lib/) | Shared utilities + test fixtures consumed by both apps. |
| [ktor-app/](ktor-app/) | HTTP service (Ktor + Netty, port 8080). Generates Kotlin models from [openapi/api.yaml](openapi/api.yaml) at build time and serves Swagger UI at `/docs`. **Default deploy target.** |
| [grpc-app/](grpc-app/) | gRPC service. Alternate deploy target — set `TARGET_APP` in the workflow to switch. |

The top-level [openapi/](openapi/) directory is the single source of truth for the HTTP API contract. Edit `api.yaml` directly; `compileKotlin` re-runs codegen, and Jib bakes the spec into the deployed container at `/app/resources/openapi/` so the running app can serve it from `/docs` and `/openapi.yaml`.

## Prerequisites

- A GCP project you own (and billing enabled on it).
- `gcloud` CLI, authenticated (`gcloud auth login`).
- Terraform ≥ 1.14.3 (the deploy workflow pins this).
- JDK 22 (Foojay auto-downloads via the Gradle toolchain — no manual install required).
- Docker — for local `docker compose` runs.

## First-time setup

1. **Clone & rename.** Update `rootProject.name` in [settings.gradle.kts](settings.gradle.kts#L6). Optionally rename the `com.autokotlin` package across the source tree (and update `projectName` in [ktor-app/build.gradle.kts](ktor-app/build.gradle.kts) to match — it drives the OpenAPI-generated package).

2. **Create the GCP project** (skip if it already exists):
   ```bash
   gcloud projects create <PROJECT_ID> --name="<display name>"
   gcloud beta billing projects link <PROJECT_ID> --billing-account=<BILLING_ACCOUNT_ID>
   ```

3. **Run the bootstrap script.** This enables required APIs, creates the GCS state bucket, provisions the Workload Identity Federation pool/provider for GitHub Actions, and creates the deploy service account:
   ```bash
   ./scripts/bootstrap/setup-project.sh \
     <PROJECT_ID> <PROJECT_NAME> <github-org/repo> <your-email>
   ```
   Note the final block of output — you'll need the **state bucket name**, **WIF provider name**, and **service-account email** in the next step.

4. **Create an Artifact Registry Docker repository** (the bootstrap script does NOT do this — pick a name and location of your choice):
   ```bash
   gcloud artifacts repositories create <repo-name> \
     --repository-format=docker \
     --location=<REGION> \
     --project=<PROJECT_ID>
   ```
   Your full registry host path is `<REGION>-docker.pkg.dev/<PROJECT_ID>/<repo-name>`.

5. **Replace `REPLACE_ME` placeholders.** A `grep -rn REPLACE_ME .` will surface every one, but here's the explicit checklist:
   - [.github/workflows/deploy.yml](.github/workflows/deploy.yml) — `REGION`, `PROJECT_ID`, `PROJECT_NUMBER`, `WORKLOAD_IDENTITY_PROVIDER`, `SERVICE_ACCOUNT`, `GAR_REPO`.
   - [infra/main.tf](infra/main.tf) — `backend "gcs"` bucket name.
   - [infra/variables.tf](infra/variables.tf) — `service_image` default (only matters if you run `terraform plan` from your laptop; CI overrides it).
   - [ktor-app/build.gradle.kts](ktor-app/build.gradle.kts) — fallback `to.image` host in the `jib { }` block.
   - [docker-compose.yml](docker-compose.yml) — `image:` line (match what the jib config produces).

6. **Push to `main`.** The `Build & Deploy` workflow runs Gradle tests, builds + pushes the image via Jib, then runs `terraform apply` to provision/update the Cloud Run service. PRs run the same flow up to `terraform plan` and post the plan as a comment; add the `terraform:apply` label on a PR to apply from a PR run.

## Local development

The fastest inner loop bypasses Artifact Registry entirely — Jib can build straight into your local Docker daemon:

1. **Build the image locally:**
   ```bash
   ./gradlew :ktor-app:jibDockerBuild
   ```
   This compiles the app (including OpenAPI codegen), packages it as a container image, and loads it into your Docker daemon under the `to.image:latest` tag from [ktor-app/build.gradle.kts](ktor-app/build.gradle.kts). No network registry needed.

2. **Run it:**
   ```bash
   docker compose up
   ```
   The app is now on `http://localhost:8080`:
   - `GET /` → `Hello World!`
   - `GET /hello` → `{"message":"Hello World!"}` (served from the generated `Hello` model)
   - `GET /docs` → Swagger UI rendered from `openapi/api.yaml`
   - `GET /openapi.yaml` → the raw spec

3. **Iterate.** After source changes, re-run `./gradlew :ktor-app:jibDockerBuild`, then `docker compose up --force-recreate`. Editing `openapi/api.yaml` re-triggers codegen as part of `compileKotlin`.

Alternatively, `./gradlew :ktor-app:run` runs the app on the host JVM (no Docker, no compose) — useful when you want a live-reload-ish workflow under Gradle's `--continuous` flag.

## Switching the deployed app

The deploy workflow defaults to `TARGET_APP: ktor-app`. To deploy `grpc-app` instead:

1. Change `TARGET_APP` in [.github/workflows/deploy.yml](.github/workflows/deploy.yml).
2. Keep the path-filter globs in sync (GitHub Actions path filters can't read `${{ env.* }}`).
3. Ensure the chosen subproject has a jib block in its `build.gradle.kts`. Today only [ktor-app/build.gradle.kts](ktor-app/build.gradle.kts) has one — porting the same jib block to [grpc-app/build.gradle.kts](grpc-app/build.gradle.kts) is a one-time copy.

## References

- [Workload Identity Federation for GHA → GCP](https://cloud.google.com/blog/products/identity-security/secure-your-use-of-third-party-tools-with-identity-federation)
- [Deploy to Cloud Run with GitHub Actions](https://cloud.google.com/blog/products/devops-sre/deploy-to-cloud-run-with-github-actions/)

## Appendix: manually enabled GCP APIs

`scripts/bootstrap/setup-project.sh` enables the APIs needed at bootstrap time (`iam`, `iamcredentials`, `cloudresourcemanager`, `storage`). The following are also required at runtime; the script does not enable them, so enable them manually (console or `gcloud services enable …`) on first setup:

- Cloud Run Admin API (`run.googleapis.com`)
- Artifact Registry API (`artifactregistry.googleapis.com`)
