# AWS EC2 Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish each successful `main` commit to Docker Hub and Amazon ECR, then deploy the exact ECR SHA tag to one EC2 instance through Systems Manager while the application uses the private RDS PostgreSQL instance.

**Architecture:** GitHub Actions keeps the existing backend and frontend gates, builds one `linux/amd64` image, pushes identical `latest` and `sha-<short-commit>` tags to both registries, and assumes an AWS role through GitHub OIDC. A gated deploy job sends the immutable ECR image reference to an EC2-hosted deploy script through SSM; the container serves HTTP on port 80 and reaches RDS through security-group-to-security-group access on port 5432.

**Tech Stack:** GitHub Actions, Docker Buildx and Compose, Bash, AWS IAM OIDC, Amazon ECR, Amazon EC2, AWS Systems Manager, SSM Parameter Store, Amazon RDS for PostgreSQL 16, Spring Boot Actuator, Maven, npm, ShellCheck, and actionlint.

## Global Constraints

- Preserve the root `docker-compose.yml`; it remains the local application-plus-PostgreSQL stack.
- Preserve all existing backend, frontend, coverage, integration, and CodeQL gates.
- Build the image once per successful `main` push and send the same Buildx output to Docker Hub and ECR.
- Deploy only an immutable `sha-<7 hexadecimal characters>` tag, never `latest`.
- Grant `id-token: write` only to jobs that authenticate to AWS.
- Do not add AWS access-key secrets to GitHub or SSH access to EC2.
- Never print the RDS master password, application database password, JWT secret, or decrypted environment file.
- Keep `/vmh/prod/rds-master-password` only until the one-time database bootstrap succeeds.
- Keep the first public endpoint HTTP-only and explicitly document that it is for test data and demonstration credentials.
- Do not add Terraform, CloudFormation, ECS, EKS, an ALB, Auto Scaling, Route 53, HTTPS, Multi-AZ, RDS Proxy, or automatic rollback in this change.
- Use the approved AWS region `us-east-2`, account `675244612319`, ECR repository `mmetznerm/vehicle-maintenance-history`, database `vehicle_maintenance_history`, application role `vmh_app`, and RDS master role `vmh_admin`.
- Use current action majors: `actions/checkout@v7`, `actions/setup-node@v7`, `actions/setup-java@v5`, `github/codeql-action@v4`, `aws-actions/configure-aws-credentials@v6`, and `aws-actions/amazon-ecr-login@v2`.

---

## Task 1: Define and test the production Compose contract

**Files:**

- Create: `deploy/compose.prod.yml`
- Create: `deploy/tests/testlib.sh`
- Create: `deploy/tests/compose_prod_test.sh`

- [ ] **Step 1: Create the shared shell-test assertions**

Create `deploy/tests/testlib.sh` with `set -euo pipefail` and these helpers:

```bash
fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

assert_contains() {
  local haystack="$1"
  local needle="$2"
  [[ "$haystack" == *"$needle"* ]] || fail "expected output to contain: $needle"
}

assert_not_contains() {
  local haystack="$1"
  local needle="$2"
  [[ "$haystack" != *"$needle"* ]] || fail "expected output not to contain: $needle"
}

assert_file_mode() {
  local expected="$1"
  local path="$2"
  local actual
  actual="$(stat -c '%a' "$path")"
  [[ "$actual" == "$expected" ]] || fail "expected $path mode $expected, got $actual"
}
```

- [ ] **Step 2: Write the failing Compose contract test**

Create `deploy/tests/compose_prod_test.sh`. Export deterministic non-secret test values, render the Compose model, and assert the production-only requirements:

```bash
#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$TEST_DIR/../.." && pwd)"
source "$TEST_DIR/testlib.sh"

export IMAGE_URI="registry.example.invalid/mmetznerm/vehicle-maintenance-history"
export IMAGE_TAG="sha-0123abc"
export SPRING_DATASOURCE_URL="jdbc:postgresql://db.internal:5432/vehicle_maintenance_history?sslmode=require"
export SPRING_DATASOURCE_USERNAME="vmh_app"
export SPRING_DATASOURCE_PASSWORD="0123456789abcdef0123456789abcdef"
export JWT_SECRET="abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
export JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0"

rendered="$(docker compose -f "$REPO_ROOT/deploy/compose.prod.yml" config)"
assert_contains "$rendered" "registry.example.invalid/mmetznerm/vehicle-maintenance-history:sha-0123abc"
assert_contains "$rendered" "target: 8080"
assert_contains "$rendered" "published: \"80\""
assert_contains "$rendered" "restart: unless-stopped"
assert_contains "$rendered" "mem_limit: \"805306368\""
assert_contains "$rendered" "max-size: 10m"
assert_contains "$rendered" "max-file: \"3\""
assert_contains "$rendered" "curl -fsS http://localhost:8080/actuator/health"
assert_not_contains "$rendered" "postgres:"
printf 'PASS: production Compose contract\n'
```

- [ ] **Step 3: Run the test and confirm it fails for the missing file**

Run:

```bash
bash deploy/tests/compose_prod_test.sh
```

Expected: non-zero exit because `deploy/compose.prod.yml` does not exist.

- [ ] **Step 4: Implement the production Compose file**

Create `deploy/compose.prod.yml` with exactly one `app` service:

```yaml
services:
  app:
    image: "${IMAGE_URI:?IMAGE_URI is required}:${IMAGE_TAG:?IMAGE_TAG is required}"
    restart: unless-stopped
    ports:
      - "80:8080"
    environment:
      SPRING_DATASOURCE_URL: "${SPRING_DATASOURCE_URL:?SPRING_DATASOURCE_URL is required}"
      SPRING_DATASOURCE_USERNAME: "${SPRING_DATASOURCE_USERNAME:?SPRING_DATASOURCE_USERNAME is required}"
      SPRING_DATASOURCE_PASSWORD: "${SPRING_DATASOURCE_PASSWORD:?SPRING_DATASOURCE_PASSWORD is required}"
      JWT_SECRET: "${JWT_SECRET:?JWT_SECRET is required}"
      JAVA_TOOL_OPTIONS: "${JAVA_TOOL_OPTIONS:--XX:MaxRAMPercentage=70.0}"
    healthcheck:
      test: ["CMD", "curl", "-fsS", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 6
      start_period: 30s
    mem_limit: 768m
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
```

- [ ] **Step 5: Run the Compose contract test**

Run:

```bash
bash deploy/tests/compose_prod_test.sh
```

Expected: `PASS: production Compose contract`.

- [ ] **Step 6: Commit the production Compose contract**

```bash
git add deploy/compose.prod.yml deploy/tests/testlib.sh deploy/tests/compose_prod_test.sh
git commit -m "infra: add production compose configuration"
```

---

## Task 2: Add a repeatable, SHA-pinned EC2 deploy script

**Files:**

- Create: `deploy/deploy.sh`
- Create: `deploy/tests/deploy_test.sh`

- [ ] **Step 1: Write deploy-script tests with mocked external commands**

Create `deploy/tests/deploy_test.sh`. Each case must use a new temporary directory, place executable `aws`, `docker`, and `curl` stubs first in `PATH`, set `VMH_APP_DIR` to the temporary application directory, and remove the directory through a trap.

Cover these cases:

1. no arguments exits non-zero and mentions `IMAGE_URI IMAGE_TAG`;
2. tag `latest` exits non-zero before any mocked AWS or Docker call;
3. tag `sha-0123abc` succeeds, requests `/vmh/prod/app-env` with decryption, logs in to the registry, runs Compose `pull` and `up -d`, and writes a mode-`600` `.env` containing the fetched application values plus the exact `IMAGE_URI` and `IMAGE_TAG`;
4. two failed health probes followed by one success exits zero;
5. twelve failed health probes exit non-zero.

Use an AWS stub that returns this exact SecureString value when called with `ssm get-parameter`:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://db.internal:5432/vehicle_maintenance_history?sslmode=require
SPRING_DATASOURCE_USERNAME=vmh_app
SPRING_DATASOURCE_PASSWORD=0123456789abcdef0123456789abcdef
JWT_SECRET=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70.0
```

Set `VMH_HEALTH_ATTEMPTS=3` and `VMH_HEALTH_DELAY_SECONDS=0` in the retry-success test, and `VMH_HEALTH_ATTEMPTS=2` plus zero delay in the permanent-failure test so the suite remains fast.

- [ ] **Step 2: Run the test and confirm the missing script failure**

Run:

```bash
bash deploy/tests/deploy_test.sh
```

Expected: non-zero exit because `deploy/deploy.sh` does not exist.

- [ ] **Step 3: Implement argument validation and safe defaults**

Create `deploy/deploy.sh` with:

```bash
#!/usr/bin/env bash
set -euo pipefail

usage() {
  printf 'Usage: %s IMAGE_URI IMAGE_TAG\n' "$0" >&2
}

[[ "$#" -eq 2 ]] || { usage; exit 2; }
IMAGE_URI="$1"
IMAGE_TAG="$2"
[[ "$IMAGE_TAG" =~ ^sha-[0-9a-f]{7,40}$ ]] || {
  printf 'IMAGE_TAG must match sha- followed by 7 to 40 lowercase hexadecimal characters\n' >&2
  exit 2
}

APP_DIR="${VMH_APP_DIR:-/opt/vehicle-maintenance-history}"
ENV_PARAMETER="${VMH_ENV_PARAMETER:-/vmh/prod/app-env}"
REGION="${AWS_REGION:-us-east-2}"
HEALTH_ATTEMPTS="${VMH_HEALTH_ATTEMPTS:-12}"
HEALTH_DELAY_SECONDS="${VMH_HEALTH_DELAY_SECONDS:-5}"
REGISTRY="${IMAGE_URI%%/*}"
```

- [ ] **Step 4: Implement secret retrieval, atomic environment installation, and ECR login**

The body must:

- create `APP_DIR` without printing secrets;
- create the temporary environment file inside `APP_DIR` so the final move is atomic;
- register a trap that removes the temporary file;
- call `aws ssm get-parameter --name "$ENV_PARAMETER" --with-decryption --query Parameter.Value --output text --region "$REGION"` with stdout redirected to the temporary file;
- append `IMAGE_URI` and `IMAGE_TAG` with `printf`;
- set mode `600`, then move the file to `$APP_DIR/.env`;
- authenticate with `aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"`.

- [ ] **Step 5: Implement Compose rollout and bounded health verification**

Use the same explicit options for both Compose calls:

```bash
compose=(docker compose --env-file "$APP_DIR/.env" -f "$APP_DIR/compose.prod.yml")
"${compose[@]}" pull
"${compose[@]}" up -d --remove-orphans
```

Then retry `curl -fsS http://localhost/actuator/health` up to `HEALTH_ATTEMPTS`, sleeping `HEALTH_DELAY_SECONDS` only between attempts. Port 80 is the EC2 host port; port 8080 is reachable only inside the container. Print only attempt numbers and the final success or failure; do not print the health body or environment file.

- [ ] **Step 6: Run deploy tests and ShellCheck**

Run:

```bash
bash deploy/tests/deploy_test.sh
docker run --rm -v "$PWD:/mnt" koalaman/shellcheck:stable deploy/deploy.sh deploy/tests/testlib.sh deploy/tests/deploy_test.sh
```

Expected: all deploy tests print `PASS`, and ShellCheck exits zero.

- [ ] **Step 7: Commit the deployment script**

```bash
git add deploy/deploy.sh deploy/tests/deploy_test.sh
git commit -m "infra: add EC2 deployment script"
```

---

## Task 3: Bootstrap EC2 and the RDS application database safely

**Files:**

- Create: `deploy/setup-ec2.sh`
- Create: `deploy/tests/setup_ec2_test.sh`
- Modify: `deploy/tests/testlib.sh`

- [ ] **Step 1: Add any file-content and command-log helpers needed by setup tests**

Extend `deploy/tests/testlib.sh` only with generally reusable assertions, such as `assert_file_exists` and `assert_executable`. Keep all fixture creation inside the owning test script.

- [ ] **Step 2: Write the failing setup-script test suite**

Create `deploy/tests/setup_ec2_test.sh` with isolated mocked `aws`, `docker`, `curl`, `sha256sum`, `install`, `dnf`, `systemctl`, `mkswap`, and `swapon` commands. Run database-bootstrap cases with:

```bash
VMH_ALLOW_NON_ROOT=1
VMH_SKIP_HOST_SETUP=1
VMH_APP_DIR="$case_dir/app"
VMH_SOURCE_DIR="$REPO_ROOT/deploy"
AWS_REGION=us-east-2
```

Cover these cases:

1. missing image arguments fail with usage text;
2. non-root execution without `VMH_ALLOW_NON_ROOT=1` fails before external commands;
3. a successful run fetches `/vmh/prod/app-env` and `/vmh/prod/rds-master-password` with decryption, installs `compose.prod.yml` and executable `deploy.sh`, runs a temporary `postgres:16-alpine` client, and invokes the installed deploy script with the exact image URI and SHA tag;
4. captured SQL creates or updates login role `vmh_app`, creates database `vehicle_maintenance_history` only when absent, and never contains the literal master password;
5. simulated database-client failure stops before initial application deployment;
6. a separate host-preparation case, with `VMH_SKIP_HOST_SETUP` unset, installs and starts Docker, installs the verified Compose plugin when `docker compose version` initially fails, and creates swap only when `/swapfile` is absent.

The success fixture uses `RDS_HOST=db.internal`, app user `vmh_app`, and the same hexadecimal application secrets from Task 2. The master-password stub returns `fedcba9876543210fedcba9876543210`.

- [ ] **Step 3: Run the tests and confirm the missing script failure**

Run:

```bash
bash deploy/tests/setup_ec2_test.sh
```

Expected: non-zero exit because `deploy/setup-ec2.sh` does not exist.

- [ ] **Step 4: Implement root checks, inputs, and host preparation**

Create `deploy/setup-ec2.sh` with `set -euo pipefail`, the same image-argument validation as `deploy.sh`, and these defaults:

```bash
APP_DIR="${VMH_APP_DIR:-/opt/vehicle-maintenance-history}"
SOURCE_DIR="${VMH_SOURCE_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"
ENV_PARAMETER="${VMH_ENV_PARAMETER:-/vmh/prod/app-env}"
MASTER_PARAMETER="${VMH_MASTER_PARAMETER:-/vmh/prod/rds-master-password}"
REGION="${AWS_REGION:-us-east-2}"
```

Require effective UID zero unless `VMH_ALLOW_NON_ROOT=1`. Unless `VMH_SKIP_HOST_SETUP=1`, perform repeat-safe host setup:

```bash
dnf install -y docker
systemctl enable --now docker
if [[ ! -f /swapfile ]]; then
  dd if=/dev/zero of=/swapfile bs=1M count=1024 status=none
  chmod 600 /swapfile
  mkswap /swapfile
fi
swapon --show=NAME --noheadings | grep -Fxq /swapfile || swapon /swapfile
grep -Fqx '/swapfile none swap sw 0 0' /etc/fstab || printf '/swapfile none swap sw 0 0\n' >> /etc/fstab
```

Amazon Linux does not guarantee that installing its Docker package also provides the Compose CLI plugin. If `docker compose version` fails, download the pinned x86_64 plugin `v5.4.0` and its `checksums.txt` from the official `docker/compose` GitHub release, select the `docker-compose-linux-x86_64` checksum line, verify it with `sha256sum -c`, and install the binary as `/usr/local/lib/docker/cli-plugins/docker-compose` with mode `0755`. Run `docker compose version` again and abort if the plugin is still unavailable. Use a temporary directory plus cleanup trap; never install an unverified download.

Create `APP_DIR`, copy `compose.prod.yml` and `deploy.sh` from `SOURCE_DIR`, and make only `deploy.sh` executable.

- [ ] **Step 5: Retrieve credentials without leaking them**

Read `/vmh/prod/app-env` into a mode-`600` temporary file and read the master password into a shell variable using AWS CLI output capture. Register a cleanup trap that unsets the password and removes all temporary files.

Extract only the required environment keys from the temporary app file. Reject missing `RDS_HOST`, `SPRING_DATASOURCE_USERNAME`, or `SPRING_DATASOURCE_PASSWORD`, and require `SPRING_DATASOURCE_USERNAME=vmh_app`. Never enable shell tracing.

- [ ] **Step 6: Implement repeat-safe role and database creation through a temporary client**

Run `postgres:16-alpine` with `--rm`, network access through the host, `PGPASSWORD` supplied as an environment variable, and these connection values:

- host from `RDS_HOST`;
- port `5432`;
- database `postgres`;
- user `vmh_admin`;
- `sslmode=require`.

Feed SQL on stdin without interpolating either password into SQL text. Export `PGPASSWORD` and `APP_DB_PASSWORD` only for the `docker run` process, pass them into the container by environment-variable name rather than value, and invoke psql inside the container with the literal command `psql ... --set=app_password="$APP_DB_PASSWORD"`. Use `format('%L', :'app_password')` plus `\gexec`:

```sql
SELECT format('CREATE ROLE vmh_app LOGIN PASSWORD %L', :'app_password')
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'vmh_app')
\gexec

SELECT format('ALTER ROLE vmh_app WITH LOGIN PASSWORD %L', :'app_password')
\gexec

SELECT 'CREATE DATABASE vehicle_maintenance_history OWNER vmh_app'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'vehicle_maintenance_history')
\gexec
```

The Docker invocation must use `psql --set=ON_ERROR_STOP=1 --set=app_password="$APP_DB_PASSWORD"`; any SQL error must abort setup.

- [ ] **Step 7: Finish bootstrap with the first immutable deployment**

After database initialization succeeds, invoke:

```bash
"$APP_DIR/deploy.sh" "$IMAGE_URI" "$IMAGE_TAG"
```

Print a final message instructing the operator to delete `/vmh/prod/rds-master-password`. Do not delete it automatically, because retaining it until the operator confirms the application is healthy makes the bootstrap recoverable.

- [ ] **Step 8: Run setup tests and ShellCheck**

Run:

```bash
bash deploy/tests/setup_ec2_test.sh
docker run --rm -v "$PWD:/mnt" koalaman/shellcheck:stable deploy/setup-ec2.sh deploy/tests/setup_ec2_test.sh
```

Expected: all setup cases print `PASS`, and ShellCheck exits zero.

- [ ] **Step 9: Commit the EC2 bootstrap**

```bash
git add deploy/setup-ec2.sh deploy/tests/setup_ec2_test.sh deploy/tests/testlib.sh
git commit -m "infra: add EC2 bootstrap script"
```

---

## Task 4: Publish one image to Docker Hub and ECR, then deploy through SSM

**Files:**

- Create: `deploy/tests/workflow_contract_test.sh`
- Modify: `.github/workflows/ci-cd.yml`
- Modify: `.github/workflows/codeql.yml`

- [ ] **Step 1: Write a workflow contract test before editing YAML**

Create `deploy/tests/workflow_contract_test.sh`. Read both workflows as text and assert:

- every checkout reference is `actions/checkout@v7`;
- Java setup is `actions/setup-java@v5`;
- Node setup is `actions/setup-node@v7`;
- CodeQL remains `github/codeql-action/*@v4`;
- the publish and deploy jobs each contain `id-token: write`;
- AWS authentication uses `aws-actions/configure-aws-credentials@v6`;
- ECR login uses `aws-actions/amazon-ecr-login@v2`;
- metadata names both `mmetznerm/vehicle-maintenance-history` and `${{ steps.login-ecr.outputs.registry }}/${{ vars.ECR_REPOSITORY }}`;
- the deploy condition contains `vars.AWS_DEPLOY_ENABLED == 'true'`;
- deployment uses `AWS-RunShellScript`, `${{ vars.EC2_INSTANCE_ID }}`, and the ECR repository variable;
- no workflow contains `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY`.

- [ ] **Step 2: Run the contract test and confirm it fails on old action majors and missing AWS jobs**

Run:

```bash
bash deploy/tests/workflow_contract_test.sh
```

Expected: non-zero exit and a focused assertion describing the first missing workflow contract.

- [ ] **Step 3: Upgrade GitHub-maintained action versions without changing quality behavior**

In `.github/workflows/ci-cd.yml`, change checkout to `@v7`, Java setup to `@v5`, and Node setup to `@v7`. In `.github/workflows/codeql.yml`, change checkout to `@v7` and Java setup to `@v5`; leave CodeQL at `@v4`, including its language matrix, schedule, build modes, permissions, and commands.

- [ ] **Step 4: Authenticate and publish to both registries in the existing publish job**

Rename the job display name to `Publish Docker Image to Docker Hub and ECR`. Add job-scoped permissions:

```yaml
permissions:
  contents: read
  id-token: write
```

After checkout and before registry logins, add:

```yaml
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v6
  with:
    role-to-assume: ${{ vars.AWS_ROLE_ARN }}
    aws-region: ${{ vars.AWS_REGION }}
    role-session-name: github-actions-publish-${{ github.run_id }}

- name: Log in to Amazon ECR
  id: login-ecr
  uses: aws-actions/amazon-ecr-login@v2
```

Keep the Docker Hub login. Configure the existing metadata step with two newline-separated images:

```yaml
images: |
  mmetznerm/vehicle-maintenance-history
  ${{ steps.login-ecr.outputs.registry }}/${{ vars.ECR_REPOSITORY }}
tags: |
  type=raw,value=latest
  type=sha,prefix=sha-,format=short
```

Keep a single `docker/build-push-action@v7` invocation and its existing `linux/amd64`, cache, label, context, and Dockerfile settings. Add this job output for the downstream job:

```yaml
outputs:
  ecr_registry: ${{ steps.login-ecr.outputs.registry }}
```

- [ ] **Step 5: Add the feature-gated SSM deploy job**

Add `deploy-to-ec2` with:

- `needs: publish-docker-image`;
- the same `main` push condition plus `vars.AWS_DEPLOY_ENABLED == 'true'`;
- `ubuntu-latest`;
- job-scoped `contents: read` and `id-token: write`;
- `environment` deliberately omitted;
- AWS credential setup through the same OIDC role.

Create deterministic image outputs in one Bash step:

```bash
echo "image_uri=${{ needs.publish-docker-image.outputs.ecr_registry }}/${{ vars.ECR_REPOSITORY }}" >> "$GITHUB_OUTPUT"
echo "image_tag=sha-${GITHUB_SHA::7}" >> "$GITHUB_OUTPUT"
```

Send the command with AWS CLI and JSON generated by `jq` so image values are never shell-concatenated into JSON:

```bash
command="/opt/vehicle-maintenance-history/deploy.sh '${{ steps.image.outputs.image_uri }}' '${{ steps.image.outputs.image_tag }}'"
parameters="$(jq -cn --arg command "$command" '{commands: [$command]}')"
command_id="$(aws ssm send-command \
  --region '${{ vars.AWS_REGION }}' \
  --instance-ids '${{ vars.EC2_INSTANCE_ID }}' \
  --document-name AWS-RunShellScript \
  --comment "Deploy ${{ steps.image.outputs.image_tag }}" \
  --parameters "$parameters" \
  --query Command.CommandId \
  --output text)"
echo "command_id=$command_id" >> "$GITHUB_OUTPUT"
```

Wait in a separate step with `continue-on-error: true`, capture the wait exit code as a step output, and always run `aws ssm get-command-invocation --command-id ... --instance-id ...`. Add a final step that exits non-zero unless the stored wait code is zero. This guarantees the SSM stdout/stderr is visible even when deployment fails.

- [ ] **Step 6: Run workflow contract and syntax checks**

Run:

```bash
bash deploy/tests/workflow_contract_test.sh
docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest
```

Expected: the contract script prints `PASS`, and actionlint exits zero with no findings.

- [ ] **Step 7: Commit the delivery workflow**

```bash
git add .github/workflows/ci-cd.yml .github/workflows/codeql.yml deploy/tests/workflow_contract_test.sh
git commit -m "ci: publish to ECR and deploy through SSM"
```

---

## Task 5: Write a console-oriented AWS runbook and portfolio explanation

**Files:**

- Create: `docs/aws-deployment.md`
- Modify: `README.md`

- [ ] **Step 1: Write the AWS guide in the actual execution order**

Create `docs/aws-deployment.md` with these numbered sections and concrete values:

1. **Safety and cost:** state HTTP/test-data limitations; change the RDS storage autoscaling maximum from 1000 GiB to 30 GiB.
2. **ECR:** verify private repository `mmetznerm/vehicle-maintenance-history` in `us-east-2`.
3. **Network:** create `vehicle-maintenance-history-app-sg` with inbound TCP 80 from `0.0.0.0/0`; assign `vehicle-maintenance-history-db-sg` to RDS with inbound TCP 5432 whose source is the app security group; confirm RDS public access is `No` and neither group exposes port 22.
4. **GitHub OIDC provider:** provider URL `https://token.actions.githubusercontent.com`, audience `sts.amazonaws.com`.
5. **GitHub Actions IAM role:** role name `github-actions-vehicle-maintenance-history`; trust only `repo:mmetznerm/vehicle-maintenance-history:ref:refs/heads/main`; include exact ECR push permissions for ARN `arn:aws:ecr:us-east-2:675244612319:repository/mmetznerm/vehicle-maintenance-history`, global `ecr:GetAuthorizationToken`, exact selected EC2 instance ARN for `ssm:SendCommand`, AWS document ARN `arn:aws:ssm:us-east-2::document/AWS-RunShellScript`, and command read permissions.
6. **EC2 instance role:** role name `vehicle-maintenance-history-ec2-role`; attach `AmazonSSMManagedInstanceCore` and `AmazonEC2ContainerRegistryPullOnly`; add `ssm:GetParameter` for `arn:aws:ssm:us-east-2:675244612319:parameter/vmh/prod/*` and `kms:Decrypt` restricted through the SSM service in `us-east-2`.
7. **SecureString values:** generate the application password with `openssl rand -hex 32` and JWT secret with `openssl rand -hex 64`; query the real endpoint for `vehicle-maintenance-history-db`; create `/vmh/prod/app-env` with exactly `RDS_HOST`, JDBC URL ending `/vehicle_maintenance_history?sslmode=require`, username `vmh_app`, generated password, JWT secret, and `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70.0`; create `/vmh/prod/rds-master-password` from the existing `vmh_admin` password.
8. **EC2:** launch Amazon Linux 2023 x86_64, `t3.micro`, 10 GiB gp3, public IPv4, same VPC as RDS, public subnet, app security group, EC2 instance role, and tags `Name=vehicle-maintenance-history` and `Application=vehicle-maintenance-history`.
9. **First deployment:** verify the instance is online in Systems Manager; copy the first ECR `sha-` tag; derive its full 40-character Git commit; use Session Manager to download `compose.prod.yml`, `deploy.sh`, and `setup-ec2.sh` from `raw.githubusercontent.com/mmetznerm/vehicle-maintenance-history/<full-commit>/deploy/`; run `sudo bash setup-ec2.sh <full-ECR-image-URI> <sha-tag>`; verify the final health message.
10. **Remove bootstrap credential:** delete `/vmh/prod/rds-master-password` only after the application is healthy; retain `/vmh/prod/app-env`.
11. **GitHub variables:** set `AWS_REGION=us-east-2`, `AWS_ROLE_ARN=arn:aws:iam::675244612319:role/github-actions-vehicle-maintenance-history`, `ECR_REPOSITORY=mmetznerm/vehicle-maintenance-history`, and the actual `EC2_INSTANCE_ID`; leave `AWS_DEPLOY_ENABLED` absent during setup, then set it to `true`. Retain Docker secrets `DOCKER_USERNAME` and `DOCKER_ACCESS_TOKEN`.
12. **Acceptance checks:** same SHA tag in Docker Hub and ECR, successful deploy job, public frontend over HTTP, Actuator `UP`, registration and login with test data, persistence after `docker restart`, no port 22, private RDS, and no long-lived AWS keys in GitHub.
13. **Operations:** Session Manager commands for `docker compose ps`, `docker compose logs --tail=200 app`, and local Actuator health; manual rollback by listing ECR tags and rerunning `/opt/vehicle-maintenance-history/deploy.sh` with a previously successful SHA.

For the two AWS values not known at documentation time, provide exact read-only CloudShell commands rather than ambiguous sample values:

```bash
aws rds describe-db-instances --region us-east-2 --db-instance-identifier vehicle-maintenance-history-db --query 'DBInstances[0].Endpoint.Address' --output text
aws ec2 describe-instances --region us-east-2 --filters 'Name=tag:Name,Values=vehicle-maintenance-history' 'Name=instance-state-name,Values=pending,running,stopping,stopped' --query 'Reservations[0].Instances[0].InstanceId' --output text
```

Also explain how to copy the resulting endpoint and instance ID into the console fields. Do not include real passwords or secrets in the guide.

- [ ] **Step 2: Update the README architecture and registry responsibilities**

Add a concise `AWS Deployment` section that:

- links to `docs/aws-deployment.md`;
- describes Docker Hub as the public portfolio registry and ECR as the private runtime registry;
- states that GitHub OIDC replaces persistent AWS keys;
- explains that SSM deploys an immutable SHA tag to EC2 and EC2 reaches private RDS;
- notes that the public demo uses HTTP and test data only;
- includes a small text flow: `GitHub -> Actions -> Docker Hub + ECR -> SSM -> EC2 -> RDS`.

Update `Out Of Scope` so it no longer claims all advanced CI/CD or AWS deployment is excluded. Keep the genuinely excluded items from the approved design.

- [ ] **Step 3: Check documentation references and secret hygiene**

Run:

```bash
rg -n "docs/aws-deployment.md|Docker Hub|Amazon ECR|OIDC|AWS_DEPLOY_ENABLED|vehicle_maintenance_history" README.md docs/aws-deployment.md
rg -n "AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|fedcba9876543210|abcdef0123456789abcdef" README.md docs/aws-deployment.md && exit 1 || true
```

Expected: the first command finds all architectural and operational terms; the second finds no credential names or test-secret literals in user documentation.

- [ ] **Step 4: Commit the deployment documentation**

```bash
git add README.md docs/aws-deployment.md
git commit -m "docs: add AWS deployment guide"
```

---

## Task 6: Run the complete local release verification

**Files:**

- Verify: `deploy/compose.prod.yml`
- Verify: `deploy/deploy.sh`
- Verify: `deploy/setup-ec2.sh`
- Verify: `deploy/tests/*.sh`
- Verify: `.github/workflows/ci-cd.yml`
- Verify: `.github/workflows/codeql.yml`
- Verify: `README.md`
- Verify: `docs/aws-deployment.md`

- [ ] **Step 1: Run all deployment contract tests from a clean shell**

```bash
bash deploy/tests/compose_prod_test.sh
bash deploy/tests/deploy_test.sh
bash deploy/tests/setup_ec2_test.sh
bash deploy/tests/workflow_contract_test.sh
```

Expected: every script exits zero and prints only `PASS` results plus intentional diagnostics from failure-path fixtures.

- [ ] **Step 2: Run shell and workflow linters**

```bash
docker run --rm -v "$PWD:/mnt" koalaman/shellcheck:stable deploy/deploy.sh deploy/setup-ec2.sh deploy/tests/testlib.sh deploy/tests/compose_prod_test.sh deploy/tests/deploy_test.sh deploy/tests/setup_ec2_test.sh deploy/tests/workflow_contract_test.sh
docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest
```

Expected: both commands exit zero without findings.

- [ ] **Step 3: Re-render production Compose independently**

```bash
IMAGE_URI=registry.example.invalid/mmetznerm/vehicle-maintenance-history \
IMAGE_TAG=sha-0123abc \
SPRING_DATASOURCE_URL='jdbc:postgresql://db.internal:5432/vehicle_maintenance_history?sslmode=require' \
SPRING_DATASOURCE_USERNAME=vmh_app \
SPRING_DATASOURCE_PASSWORD=0123456789abcdef0123456789abcdef \
JWT_SECRET=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789 \
JAVA_TOOL_OPTIONS='-XX:MaxRAMPercentage=70.0' \
docker compose -f deploy/compose.prod.yml config --quiet
```

Expected: exit zero with no output.

- [ ] **Step 4: Run the backend quality suites**

On Windows PowerShell:

```powershell
Set-Location backend
.\mvnw.cmd -B verify
.\mvnw.cmd -B verify -Pintegration-tests
Set-Location ..
```

Expected: both Maven invocations end with `BUILD SUCCESS`.

- [ ] **Step 5: Run the frontend quality suites**

```bash
npm --prefix frontend ci
npm --prefix frontend run lint
npm --prefix frontend run test:coverage
npm --prefix frontend run build
```

Expected: lint exits zero, tests and coverage pass, and the production build completes.

- [ ] **Step 6: Build the exact target platform locally**

```bash
docker build --platform linux/amd64 -f backend/Dockerfile -t vehicle-maintenance-history:aws-plan-verify .
```

Expected: the multi-stage image build completes successfully.

- [ ] **Step 7: Review the final diff and repository hygiene**

```bash
git diff --check origin/main...HEAD
git status --short
git log --oneline origin/main..HEAD
```

Expected:

- `git diff --check` prints nothing;
- only the intentionally local `.superpowers/` visualization directory may remain untracked;
- the branch contains the design commit plus the four implementation commits and documentation commit described above;
- no generated build output, environment file, password, token, AWS access key, or visualization asset is staged.

- [ ] **Step 8: Perform the pre-PR review**

Compare the final implementation line by line with `docs/superpowers/specs/2026-08-16-aws-ec2-deployment-design.md`. Confirm that every required repository change, failure behavior, security boundary, feature gate, documentation instruction, and verification item is represented, and that none of the deliberately excluded infrastructure was introduced.

Do not enable `AWS_DEPLOY_ENABLED` or attempt the AWS acceptance checks until these files have been merged into `main`, the AWS roles and EC2 instance exist, and the manual first deployment has succeeded.
