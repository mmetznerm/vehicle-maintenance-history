# Docker Hub CI/CD Design

## Goal

Publish the production application image to Docker Hub after all continuous
integration checks succeed on the `main` branch.

The published image is:

```text
mmetznerm/vehicle-maintenance-history
```

## Scope

This change adds continuous delivery of the existing multi-stage Docker image.
It does not deploy or restart the application on a hosting provider or server.

## Workflow Architecture

Rename `.github/workflows/pr-checks.yml` to `.github/workflows/ci-cd.yml` and
change its displayed name from `PR Checks` to `CI/CD`.

The workflow keeps its current triggers:

- Pull requests targeting `main` run continuous integration only.
- Pushes to `main` run continuous integration and, after it succeeds, publish
  the Docker image.

The existing jobs remain responsible for:

- Backend unit, controller, and coverage checks.
- Backend integration tests with Testcontainers and PostgreSQL.
- Frontend lint, coverage checks, and production build.

A new `publish-docker-image` job depends on all three existing jobs through
`needs`. It runs only for a `push` to `main`, so pull requests never receive
Docker Hub credentials and never publish images.

## Image Build and Publication

The publication job performs these steps:

1. Check out the commit that triggered the successful workflow.
2. Configure Docker Buildx.
3. Authenticate to Docker Hub with the existing GitHub Actions secrets
   `DOCKER_USERNAME` and `DOCKER_ACCESS_TOKEN`.
4. Generate image tags and standard Open Container Initiative labels.
5. Build `backend/Dockerfile` with the repository root as its context.
6. Push the resulting `linux/amd64` image to Docker Hub.

The workflow uses the maintained major versions of Docker's official actions:

- `docker/setup-buildx-action@v4`
- `docker/login-action@v4`
- `docker/metadata-action@v6`
- `docker/build-push-action@v7`

GitHub Actions cache storage is used for Docker build layers through
`cache-from: type=gha` and `cache-to: type=gha,mode=max`.

## Image Tags

Every successful publication produces two tags for the same image:

- `latest`, which tracks the newest successful build from `main`.
- `sha-<short-commit>`, which permanently identifies the source commit used
  to build the image.

For example:

```text
mmetznerm/vehicle-maintenance-history:latest
mmetznerm/vehicle-maintenance-history:sha-a1b2c3d
```

## Failure and Security Behavior

- A failed backend, integration, frontend, authentication, Docker build, or
  Docker push step fails the workflow.
- The publication job does not start when any required CI job fails.
- Docker Hub credentials remain in GitHub Actions secrets and are passed only
  to the Docker login action.
- The workflow grants only `contents: read`; publishing to Docker Hub does not
  require GitHub package write permission.
- Concurrent runs for the same Git reference continue to cancel superseded
  executions through the existing concurrency configuration.

## Documentation

Update `README.md` to:

- Rename the `PR Checks` badge to `CI/CD` and point it to the renamed workflow.
- State that successful pushes to `main` publish the Docker image.
- Document `docker pull mmetznerm/vehicle-maintenance-history:latest`.
- Explain the `latest` and `sha-<commit>` tags.

## Verification

Before completion:

- Validate the workflow YAML structure.
- Run backend unit and integration tests with the coverage gate.
- Run frontend lint, coverage tests, and production build.
- Build the production image locally with `backend/Dockerfile`.
- Validate Docker Compose configuration.

The first successful push to `main` is the end-to-end verification of Docker
Hub authentication and publication because repository secrets are available
only inside GitHub Actions.

## Out of Scope

- Automatic deployment to a server or hosting provider.
- Multi-platform `linux/arm64` images and QEMU.
- Semantic version or GitHub Release tags.
- Docker Scout, image signing, SBOM publication, or provenance attestations.
- Changes to application behavior, database configuration, or runtime secrets.

## Success Criteria

- Pull requests run all CI checks without publishing an image.
- A push to `main` publishes only after every CI job succeeds.
- Docker Hub receives matching `latest` and `sha-<short-commit>` tags.
- The published image is built from the existing production Dockerfile and can
  be pulled with the documented command.
