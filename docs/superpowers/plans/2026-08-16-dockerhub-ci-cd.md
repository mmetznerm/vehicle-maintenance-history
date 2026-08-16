# Docker Hub CI/CD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish the existing production Docker image to `mmetznerm/vehicle-maintenance-history` after every successful CI run caused by a push to `main`.

**Architecture:** Rename the current PR checks workflow to a CI/CD workflow and add one publication job that depends on every existing CI job. The publication job remains skipped for pull requests, authenticates with the existing Docker Hub secrets, generates `latest` and short-SHA tags, and builds and pushes the existing multi-stage Dockerfile.

**Tech Stack:** GitHub Actions, Docker Buildx, Docker Hub, Java 21, Maven, React, TypeScript, Vitest, Testcontainers.

## Global Constraints

- Docker Hub image name: `mmetznerm/vehicle-maintenance-history`.
- GitHub Actions secrets: `DOCKER_USERNAME` and `DOCKER_ACCESS_TOKEN`.
- Published tags: `latest` and `sha-<short-commit>`.
- Published platform: `linux/amd64` only.
- Publish only after all existing CI jobs succeed on a push to `main`.
- Never publish from a pull request.
- Use `backend/Dockerfile` with the repository root as build context.
- Keep automatic deployment, multi-platform images, semantic versions, image signing, SBOMs, and attestations out of scope.

---

### Task 1: Integrate Docker Hub publication into CI

**Files:**
- Create: `.github/workflows/ci-cd.yml`
- Delete: `.github/workflows/pr-checks.yml`

**Interfaces:**
- Consumes: the existing `backend-unit-tests`, `backend-integration-tests`, and `frontend-quality` job results; GitHub secrets `DOCKER_USERNAME` and `DOCKER_ACCESS_TOKEN`; `backend/Dockerfile`.
- Produces: Docker Hub tags `mmetznerm/vehicle-maintenance-history:latest` and `mmetznerm/vehicle-maintenance-history:sha-<short-commit>` after successful pushes to `main`.

- [ ] **Step 1: Record the clean baseline**

Run:

```powershell
git status --short
```

Expected: only the implementation-plan file is untracked before implementation begins.

- [ ] **Step 2: Rename the workflow and retain the existing CI jobs**

Create `.github/workflows/ci-cd.yml` from the current
`.github/workflows/pr-checks.yml`, then change these workflow-level values:

```yaml
name: CI/CD

concurrency:
  group: ci-cd-${{ github.ref }}
  cancel-in-progress: true
```

Delete `.github/workflows/pr-checks.yml` after all three existing jobs are
present in the renamed workflow without behavioral changes.

- [ ] **Step 3: Add the Docker publication job**

Append this job to `.github/workflows/ci-cd.yml`:

```yaml
  publish-docker-image:
    name: Publish Docker Image
    needs:
      - backend-unit-tests
      - backend-integration-tests
      - frontend-quality
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v4

      - name: Log in to Docker Hub
        uses: docker/login-action@v4
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_ACCESS_TOKEN }}

      - name: Generate Docker metadata
        id: metadata
        uses: docker/metadata-action@v6
        with:
          images: mmetznerm/vehicle-maintenance-history
          tags: |
            type=raw,value=latest
            type=sha,prefix=sha-,format=short

      - name: Build and push Docker image
        uses: docker/build-push-action@v7
        with:
          context: .
          file: ./backend/Dockerfile
          platforms: linux/amd64
          push: true
          tags: ${{ steps.metadata.outputs.tags }}
          labels: ${{ steps.metadata.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

- [ ] **Step 4: Validate workflow syntax and behavior statically**

Run:

```powershell
docker run --rm -v "D:/Repositories/vehicle-maintenance-history:/repo" -w /repo rhysd/actionlint:latest
```

Expected: exit code `0` and no actionlint findings.

Inspect the workflow and confirm:

```text
pull_request -> three CI jobs; publish job skipped
push main   -> three CI jobs; publish job waits for all and then runs
```

- [ ] **Step 5: Commit the workflow change**

```powershell
git add .github/workflows/ci-cd.yml .github/workflows/pr-checks.yml
git commit -m "ci: publish Docker image after successful checks"
```

### Task 2: Document Docker Hub delivery

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: the workflow filename, Docker Hub image name, and tag policy from Task 1.
- Produces: user-facing commands and an accurate CI/CD badge and explanation.

- [ ] **Step 1: Update the workflow badge**

Replace the current PR Checks badge with:

```markdown
[![CI/CD](https://github.com/mmetznerm/vehicle-maintenance-history/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/mmetznerm/vehicle-maintenance-history/actions/workflows/ci-cd.yml)
```

- [ ] **Step 2: Document automatic publication and image tags**

Add a `Docker Hub` section after the local Docker instructions containing:

````markdown
## Docker Hub

Successful pushes to `main` publish the application image to Docker Hub after
all CI checks pass.

Pull the latest successful build:

```bash
docker pull mmetznerm/vehicle-maintenance-history:latest
```

Published tags:

- `latest` points to the newest successful build from `main`.
- `sha-<commit>` identifies the exact source commit used for the image.
````

In `CI Quality Gates`, state that a successful push to `main` publishes the
Docker image after the listed checks.

- [ ] **Step 3: Check documentation references**

Run:

```powershell
rg -n "PR Checks|pr-checks.yml|CI/CD|ci-cd.yml|mmetznerm/vehicle-maintenance-history" README.md .github
```

Expected: no stale `PR Checks` or `pr-checks.yml` references; the new badge,
workflow name, image name, and pull command are present.

- [ ] **Step 4: Commit the documentation and plan**

```powershell
git add README.md docs/superpowers/plans/2026-08-16-dockerhub-ci-cd.md
git commit -m "docs: document Docker Hub delivery"
```

### Task 3: Verify the complete delivery configuration

**Files:**
- Verify: `.github/workflows/ci-cd.yml`
- Verify: `backend/Dockerfile`
- Verify: `docker-compose.yml`
- Verify: `README.md`

**Interfaces:**
- Consumes: the completed workflow and documentation from Tasks 1 and 2.
- Produces: local evidence that the codebase, production image, workflow syntax, and Compose configuration remain valid.

- [ ] **Step 1: Run backend unit and integration verification**

Run from `backend`:

```powershell
.\mvnw.cmd -B verify -Pintegration-tests
```

Expected: unit/controller tests, integration tests, and the JaCoCo coverage gate pass with `BUILD SUCCESS`.

- [ ] **Step 2: Run frontend quality checks**

Run from `frontend`:

```powershell
npm.cmd run lint
npm.cmd run test:coverage
npm.cmd run build
```

Expected: lint exits successfully, all Vitest tests pass, the coverage gate is met, and Vite produces the production build.

- [ ] **Step 3: Build the production Docker image**

Run from the repository root:

```powershell
docker build -f backend/Dockerfile -t vehicle-maintenance-history:ci-local .
```

Expected: the multi-stage frontend and backend build completes and creates
`vehicle-maintenance-history:ci-local`.

- [ ] **Step 4: Validate Docker Compose and final diff**

Run:

```powershell
docker compose config
git diff --check HEAD~2
git status --short
```

Expected: Compose resolves both `postgres` and `app`, the diff has no whitespace
errors, and the worktree is clean.

- [ ] **Step 5: Verify the remote publication after push**

After the two implementation commits are pushed to `main`, open the `CI/CD`
workflow run and verify that `Publish Docker Image` succeeds. Confirm Docker Hub
contains both tags for the same digest:

```text
latest
sha-<short-commit>
```

This remote step is not performed locally because the Docker Hub credentials
exist only as GitHub Actions secrets.
