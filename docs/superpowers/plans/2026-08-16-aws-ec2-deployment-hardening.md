# AWS EC2 Deployment Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the existing GitHub Actions → ECR → SSM → EC2 delivery path serial, provenance-safe, least-privileged, recoverable, and continuously verified.

**Architecture:** GitHub protects the production job with concurrency while the EC2 deploy script enforces a second host-side `flock`. A tested repository script polls SSM to a terminal state, publish and deploy use separate OIDC roles, ECR SHA tags become immutable, and the EC2 root volume and swap behavior protect secrets at rest and during interrupted setup.

**Tech Stack:** Bash, Docker Compose, Docker-based ShellCheck/actionlint, GitHub Actions, AWS OIDC, ECR, Systems Manager Run Command, EC2 Amazon Linux 2023, EBS, SSM Parameter Store.

## Global Constraints

- Keep one public EC2 instance and one private PostgreSQL RDS instance; do not add Terraform, CloudFormation, ECS, EKS, ALB, autoscaling, HTTPS, SSH, or automatic rollback.
- Region is `us-east-2`; AWS account is `675244612319`; ECR repository is `mmetznerm/vehicle-maintenance-history`.
- Deploy and rollback must use only tags matching `sha-[0-9a-f]{7,40}`; `latest` is never a deployment input.
- GitHub uses temporary OIDC credentials only; never add AWS access-key secrets.
- Publish role variable is `AWS_PUBLISH_ROLE_ARN`; deploy role variable is `AWS_DEPLOY_ROLE_ARN`.
- Publish role has ECR permissions only; deploy role has SSM command permissions only.
- ECR uses `IMMUTABLE_WITH_EXCLUSION` with only the wildcard exclusion `latest`.
- EC2 uses an encrypted 10 GiB gp3 root volume and remains accessible operationally through SSM, not SSH.
- Production deploy lock is `/var/lock/vehicle-maintenance-history-deploy.lock`; default lock timeout is 900 seconds.
- SSM polling uses 90 attempts, 10 seconds between nonterminal attempts, and only `Success` is successful.
- Preserve the local `docker-compose.yml`; production continues to use `deploy/compose.prod.yml` with only the `app` service.
- Do not call AWS, enable `AWS_DEPLOY_ENABLED`, or mutate cloud resources during implementation or local verification.

---

## Task 1: Protect the EC2 deploy script with ECR validation and a host lock

**Files:**

- Modify: `deploy/deploy.sh`
- Modify: `deploy/setup-ec2.sh`
- Modify: `deploy/tests/deploy_test.sh`
- Modify: `deploy/tests/setup_ec2_test.sh`

**Interfaces:**

- Consumes: `IMAGE_URI`, `IMAGE_TAG`, `AWS_REGION`, and the existing `VMH_APP_DIR`/health-test overrides.
- Produces: `VMH_DEPLOY_LOCK_FILE` and `VMH_DEPLOY_LOCK_TIMEOUT_SECONDS`; validated ECR-only rollout behavior used by setup, SSM, and manual rollback.

- [ ] **Step 1: Add failing ECR URI and lock tests**

Extend `setup_case` with a deterministic `flock` stub and make it executable:

```bash
cat >"$BIN_DIR/flock" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'flock %s\n' "$*" >>"$VMH_TEST_LOG"
[[ "${VMH_TEST_FLOCK_FAIL:-0}" != 1 ]]
STUB

/usr/bin/chmod +x "$BIN_DIR/aws" "$BIN_DIR/docker" "$BIN_DIR/curl" "$BIN_DIR/chmod" "$BIN_DIR/flock"
```

Pass test-local lock values from `run_deploy`:

```bash
VMH_DEPLOY_LOCK_FILE="$CASE_DIR/deploy.lock" \
VMH_DEPLOY_LOCK_TIMEOUT_SECONDS=0 \
```

Add these cases before the existing success/retry cases:

```bash
case_non_ecr_uri_is_rejected_before_external_commands() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  set +e
  output="$(run_deploy success 'registry.example.invalid/vmh' sha-0123abc 2>&1)"
  status=$?
  set -e
  [[ $status -ne 0 ]] || fail 'non-ECR URI must fail'
  assert_contains "$output" 'IMAGE_URI must equal the configured production ECR repository'
  [[ ! -e "$LOG_FILE" ]] || fail 'invalid URI must not call external commands'
)

case_cross_region_ecr_uri_is_rejected() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  set +e
  output="$(run_deploy success '123456789012.dkr.ecr.us-east-1.amazonaws.com/vmh' sha-0123abc 2>&1)"
  status=$?
  set -e
  [[ $status -ne 0 ]] || fail 'cross-region URI must fail'
  assert_contains "$output" 'IMAGE_URI must equal the configured production ECR repository'
)

case_foreign_ecr_repository_is_rejected() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  set +e
  output="$(run_deploy success '999999999999.dkr.ecr.us-east-2.amazonaws.com/other/repository' sha-0123abc 2>&1)"
  status=$?
  set -e
  [[ $status -ne 0 ]] || fail 'foreign ECR repository must fail'
  assert_contains "$output" 'IMAGE_URI must equal the configured production ECR repository'
)

case_lock_failure_stops_before_secret_retrieval() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  set +e
  VMH_TEST_FLOCK_FAIL=1 run_deploy success '675244612319.dkr.ecr.us-east-2.amazonaws.com/mmetznerm/vehicle-maintenance-history' sha-0123abc
  status=$?
  set -e
  [[ $status -ne 0 ]] || fail 'lock timeout must fail'
  log="$(<"$LOG_FILE")"
  assert_contains "$log" 'flock -w 0 9'
  assert_not_contains "$log" 'aws ssm get-parameter'
  assert_not_contains "$log" 'docker login'
  [[ ! -e "$APP_DIR/.env" ]] || fail 'lock failure must not replace .env'
)
```

In the successful rollout case, assert that the first logged external call is `flock -w 0 9` and that it appears before `aws ssm get-parameter`.

Add this setup guard case so invalid images cannot change the database before the installed deploy script rejects them:

```bash
case_invalid_image_uri_stops_setup_before_external_commands() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  set +e
  output="$(VMH_SKIP_HOST_SETUP=1 run_setup '999999999999.dkr.ecr.us-east-2.amazonaws.com/other/repository' "$IMAGE_TAG" 2>&1)"
  status=$?
  set -e
  [[ $status -ne 0 ]] || fail 'invalid setup image URI must fail'
  assert_contains "$output" 'IMAGE_URI must equal the configured production ECR repository'
  [[ ! -e "$LOG_FILE" ]] || fail 'image validation must precede setup external commands'
)
```

- [ ] **Step 2: Run the focused suite and confirm RED**

Run `bash deploy/tests/deploy_test.sh`.

Expected: new URI and lock assertions fail because `deploy.sh` neither validates ECR nor calls `flock`.

- [ ] **Step 3: Validate the URI and lock inputs before any AWS or Docker call**

Replace every successful test image URI in `deploy_test.sh` and the `IMAGE_URI` constant in `setup_ec2_test.sh` with `675244612319.dkr.ecr.us-east-2.amazonaws.com/mmetznerm/vehicle-maintenance-history`. Move `REGION` before registry derivation and add the same validation immediately after tag validation in both `deploy.sh` and `setup-ec2.sh`:

```bash
EXPECTED_IMAGE_URI="675244612319.dkr.ecr.us-east-2.amazonaws.com/mmetznerm/vehicle-maintenance-history"
if [[ "$REGION" != us-east-2 || "$IMAGE_URI" != "$EXPECTED_IMAGE_URI" ]]; then
  printf 'IMAGE_URI must equal the configured production ECR repository\n' >&2
  exit 2
fi

LOCK_FILE="${VMH_DEPLOY_LOCK_FILE:-/var/lock/vehicle-maintenance-history-deploy.lock}"
LOCK_TIMEOUT_SECONDS="${VMH_DEPLOY_LOCK_TIMEOUT_SECONDS:-900}"
[[ "$LOCK_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]] || {
  printf 'VMH_DEPLOY_LOCK_TIMEOUT_SECONDS must be a non-negative integer\n' >&2
  exit 2
}
REGISTRY="${IMAGE_URI%%/*}"
```

Immediately before creating the temporary `.env`, acquire a process-lifetime lock:

```bash
mkdir -p "$APP_DIR" "$(dirname "$LOCK_FILE")"
exec 9>"$LOCK_FILE"
if ! flock -w "$LOCK_TIMEOUT_SECONDS" 9; then
  printf 'Deployment lock was not acquired within %s seconds\n' "$LOCK_TIMEOUT_SECONDS" >&2
  exit 75
fi
```

Do not release descriptor 9 manually; Bash closes it on every exit path.

- [ ] **Step 4: Run focused and regression checks**

```bash
bash deploy/tests/deploy_test.sh
bash deploy/tests/setup_ec2_test.sh
bash -n deploy/deploy.sh deploy/setup-ec2.sh deploy/tests/deploy_test.sh deploy/tests/setup_ec2_test.sh
```

Expected: every case prints `PASS`; syntax checks exit zero.

- [ ] **Step 5: Commit the deploy guard**

```bash
git add deploy/deploy.sh deploy/setup-ec2.sh deploy/tests/deploy_test.sh deploy/tests/setup_ec2_test.sh
git commit -m "infra: serialize EC2 deployments"
```

---

## Task 2: Make swap creation recover from interrupted bootstrap

**Files:**

- Modify: `deploy/setup-ec2.sh`
- Modify: `deploy/tests/setup_ec2_test.sh`

**Interfaces:**

- Consumes: `VMH_SWAP_FILE`, `VMH_FSTAB_FILE`, `VMH_TMPDIR`, and the host setup switch.
- Produces: an active, valid swap file created atomically in the target directory with a duplicate-safe fstab entry.

- [ ] **Step 1: Replace existence-only test fixtures with validity-aware stubs**

Add executable `blkid` and `mv` stubs. `mkswap` marks its argument valid, and `mv` moves both the file and marker:

```bash
cat >"$BIN_DIR/blkid" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'blkid %s\n' "$*" >>"$VMH_TEST_LOG"
path="${@: -1}"
[[ -f "$path.vmh-valid-swap" ]] || exit 2
printf 'swap\n'
STUB

cat >"$BIN_DIR/mkswap" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'mkswap %s\n' "$*" >>"$VMH_TEST_LOG"
: >"$1.vmh-valid-swap"
STUB

cat >"$BIN_DIR/mv" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'mv %s\n' "$*" >>"$VMH_TEST_LOG"
source="${@: -2:1}"
destination="${@: -1}"
/usr/bin/mv -f "$source" "$destination"
if [[ -f "$source.vmh-valid-swap" ]]; then
  /usr/bin/mv -f "$source.vmh-valid-swap" "$destination.vmh-valid-swap"
fi
STUB
```

Remove `mkswap` from the generic stub loop, add `blkid` and `mv` to the executable set, and keep `swapon` tracking active state.

- [ ] **Step 2: Add failing partial, valid, and active swap tests**

```bash
case_invalid_inactive_swap_is_repaired_atomically() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  printf 'partial' >"$CASE_DIR/swapfile"
  run_setup "$IMAGE_URI" "$IMAGE_TAG"
  log="$(<"$LOG_FILE")"
  assert_contains "$log" 'blkid -p -s TYPE -o value'
  assert_contains "$log" 'mkswap '
  assert_contains "$log" "mv -f $CASE_DIR/swapfile."
  assert_contains "$log" "swapon $CASE_DIR/swapfile"
)

case_valid_inactive_swap_is_reused() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  : >"$CASE_DIR/swapfile"
  : >"$CASE_DIR/swapfile.vmh-valid-swap"
  run_setup "$IMAGE_URI" "$IMAGE_TAG"
  log="$(<"$LOG_FILE")"
  assert_not_contains "$log" 'dd if=/dev/zero'
  assert_not_contains "$log" 'mkswap '
  assert_contains "$log" "swapon $CASE_DIR/swapfile"
)

case_active_swap_is_left_untouched() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  : >"$CASE_DIR/swap-active"
  run_setup "$IMAGE_URI" "$IMAGE_TAG"
  log="$(<"$LOG_FILE")"
  assert_not_contains "$log" 'blkid '
  assert_not_contains "$log" 'dd if=/dev/zero'
  assert_not_contains "$log" 'mkswap '
)
```

- [ ] **Step 3: Run the setup suite and confirm RED**

Run `bash deploy/tests/setup_ec2_test.sh`.

Expected: the partial-file case fails because the current script trusts existence, and the atomic `mv` assertion is absent.

- [ ] **Step 4: Implement repeat-safe swap preparation**

Extract this function before `prepare_host`:

```bash
prepare_swap() {
  local swap_type swap_tmp

  if swapon --show=NAME --noheadings | grep -Fxq "$SWAP_FILE"; then
    return
  fi

  swap_type=''
  if [[ -f "$SWAP_FILE" ]]; then
    swap_type="$(blkid -p -s TYPE -o value "$SWAP_FILE" 2>/dev/null || true)"
  fi

  if [[ "$swap_type" != swap ]]; then
    mkdir -p "$(dirname "$SWAP_FILE")"
    swap_tmp="$(mktemp "${SWAP_FILE}.XXXXXX")"
    TEMP_FILES+=("$swap_tmp" "$swap_tmp.vmh-valid-swap")
    dd if=/dev/zero of="$swap_tmp" bs=1M count=1024 status=none
    chmod 600 "$swap_tmp"
    mkswap "$swap_tmp"
    mv -f "$swap_tmp" "$SWAP_FILE"
  fi

  swapon "$SWAP_FILE"
  grep -Fqx "$SWAP_FILE none swap sw 0 0" "$FSTAB_FILE" ||
    printf '%s none swap sw 0 0\n' "$SWAP_FILE" >>"$FSTAB_FILE"
}
```

The `.vmh-valid-swap` cleanup entry exists only for the test stub and is harmless when absent in production. Replace the existing swap block in `prepare_host` with `prepare_swap`.

- [ ] **Step 5: Run focused and regression checks**

```bash
bash deploy/tests/setup_ec2_test.sh
bash deploy/tests/deploy_test.sh
bash -n deploy/setup-ec2.sh deploy/tests/setup_ec2_test.sh
```

Expected: all setup, deploy, first-run, second-run, invalid, valid, and active swap cases pass.

- [ ] **Step 6: Commit swap recovery**

```bash
git add deploy/setup-ec2.sh deploy/tests/setup_ec2_test.sh
git commit -m "infra: make EC2 swap bootstrap recoverable"
```

---

## Task 3: Add tested SSM polling, production concurrency, and separate roles

**Files:**

- Create: `deploy/wait-for-ssm-command.sh`
- Create: `deploy/tests/ssm_wait_test.sh`
- Modify: `.github/workflows/ci-cd.yml`
- Modify: `deploy/tests/workflow_contract_test.sh`
- Modify: `deploy/tests/testlib.sh`

**Interfaces:**

- Consumes: command ID and EC2 instance ID arguments plus `AWS_REGION`, `VMH_SSM_POLL_ATTEMPTS`, and `VMH_SSM_POLL_DELAY_SECONDS`.
- Produces: exit zero only for SSM `Success`; `AWS_PUBLISH_ROLE_ARN` and `AWS_DEPLOY_ROLE_ARN`; a non-cancelable, serialized production deploy job.

- [ ] **Step 1: Add a reusable equality assertion**

Append to `deploy/tests/testlib.sh`:

```bash
assert_equals() {
  local expected="$1"
  local actual="$2"
  [[ "$actual" == "$expected" ]] || fail "expected '$expected', got '$actual'"
}
```

- [ ] **Step 2: Write the failing SSM polling suite**

Create `deploy/tests/ssm_wait_test.sh`. Its `aws` stub consumes comma-separated outcomes; the literal outcome `InvocationDoesNotExist` prints that AWS error to stderr and exits 254, while other outcomes print a status and exit zero. Use command ID `11111111-2222-3333-4444-555555555555`, instance ID `i-0123456789abcdef0`, `AWS_REGION=us-east-2`, four attempts, and zero delay in normal cases.

Use these complete helpers before the cases:

```bash
#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$TEST_DIR/../.." && pwd)"
WAIT_SCRIPT="$REPO_ROOT/deploy/wait-for-ssm-command.sh"
# shellcheck disable=SC1091
source "$TEST_DIR/testlib.sh"

setup_case() {
  CASE_DIR="$(mktemp -d)"
  BIN_DIR="$CASE_DIR/bin"
  LOG_FILE="$CASE_DIR/aws.log"
  OUTCOMES_FILE="$CASE_DIR/outcomes"
  mkdir -p "$BIN_DIR"

  cat >"$BIN_DIR/aws" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'aws %s\n' "$*" >>"$VMH_TEST_LOG"
outcomes="$(<"$VMH_TEST_OUTCOMES_FILE")"
outcome="${outcomes%%,*}"
if [[ "$outcomes" == *,* ]]; then
  printf '%s' "${outcomes#*,}" >"$VMH_TEST_OUTCOMES_FILE"
fi
if [[ "$outcome" == InvocationDoesNotExist ]]; then
  printf 'An error occurred (InvocationDoesNotExist) when calling GetCommandInvocation\n' >&2
  exit 254
fi
printf '%s\n' "$outcome"
STUB
  chmod +x "$BIN_DIR/aws"
}

run_wait_with_instance() {
  local instance_id="$1"
  local outcomes="$2"
  printf '%s' "$outcomes" >"$OUTCOMES_FILE"
  : >"$LOG_FILE"
  set +e
  VMH_TEST_LOG="$LOG_FILE" \
    VMH_TEST_OUTCOMES_FILE="$OUTCOMES_FILE" \
    VMH_SSM_POLL_ATTEMPTS="${VMH_SSM_POLL_ATTEMPTS:-4}" \
    VMH_SSM_POLL_DELAY_SECONDS=0 \
    AWS_REGION=us-east-2 \
    PATH="$BIN_DIR:$PATH" \
    "$WAIT_SCRIPT" 11111111-2222-3333-4444-555555555555 "$instance_id"
  WAIT_STATUS=$?
  set -e
}

run_wait() {
  run_wait_with_instance i-0123456789abcdef0 "$1"
}
```

The suite must include:

```bash
case_eventual_consistency_then_success() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  run_wait 'InvocationDoesNotExist,Pending,InProgress,Success'
  assert_equals 0 "$WAIT_STATUS"
  assert_contains "$(<"$LOG_FILE")" 'get-command-invocation'
)

case_failed_status_is_failure() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  run_wait 'Pending,Failed'
  [[ "$WAIT_STATUS" -ne 0 ]] || fail 'Failed must fail polling'
)

case_unknown_status_is_failure() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  run_wait 'Mystery'
  [[ "$WAIT_STATUS" -ne 0 ]] || fail 'unknown SSM status must fail polling'
)

case_deadline_is_failure() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  VMH_SSM_POLL_ATTEMPTS=2 run_wait 'InProgress,InProgress,Success'
  [[ "$WAIT_STATUS" -ne 0 ]] || fail 'poll deadline must fail'
  assert_equals 2 "$(grep -c 'get-command-invocation' "$LOG_FILE")"
)

case_invalid_instance_id_fails_before_aws() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  run_wait_with_instance 'not-an-instance' 'Success'
  [[ "$WAIT_STATUS" -ne 0 ]] || fail 'invalid instance ID must fail'
  [[ ! -s "$LOG_FILE" ]] || fail 'validation must precede AWS'
)
```

- [ ] **Step 3: Run the new suite and confirm RED**

Run `bash deploy/tests/ssm_wait_test.sh`.

Expected: non-zero because `deploy/wait-for-ssm-command.sh` does not exist.

- [ ] **Step 4: Implement the bounded SSM poller**

Create executable `deploy/wait-for-ssm-command.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

[[ "$#" -eq 2 ]] || { printf 'Usage: %s COMMAND_ID INSTANCE_ID\n' "$0" >&2; exit 2; }
COMMAND_ID="$1"
INSTANCE_ID="$2"
REGION="${AWS_REGION:-us-east-2}"
POLL_ATTEMPTS="${VMH_SSM_POLL_ATTEMPTS:-90}"
POLL_DELAY_SECONDS="${VMH_SSM_POLL_DELAY_SECONDS:-10}"

[[ "$COMMAND_ID" =~ ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$ ]] || { printf 'Invalid SSM command ID\n' >&2; exit 2; }
[[ "$INSTANCE_ID" =~ ^i-[0-9a-f]{8,17}$ ]] || { printf 'Invalid EC2 instance ID\n' >&2; exit 2; }
[[ "$POLL_ATTEMPTS" =~ ^[1-9][0-9]*$ && "$POLL_DELAY_SECONDS" =~ ^[0-9]+$ ]] || { printf 'Invalid SSM polling configuration\n' >&2; exit 2; }

error_file="$(mktemp)"
trap 'rm -f "$error_file"' EXIT

for ((attempt = 1; attempt <= POLL_ATTEMPTS; attempt++)); do
  : >"$error_file"
  set +e
  status="$(aws ssm get-command-invocation --region "$REGION" --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" --query Status --output text 2>"$error_file")"
  aws_status=$?
  set -e

  if ((aws_status != 0)); then
    if grep -Fq InvocationDoesNotExist "$error_file"; then
      printf 'SSM invocation not visible yet (%s/%s)\n' "$attempt" "$POLL_ATTEMPTS"
    else
      cat "$error_file" >&2
      exit 1
    fi
  else
    printf 'SSM status: %s (%s/%s)\n' "$status" "$attempt" "$POLL_ATTEMPTS"
    case "$status" in
      Success) exit 0 ;;
      Pending|InProgress|Delayed) ;;
      Cancelled|TimedOut|Failed|Cancelling) exit 1 ;;
      *) printf 'Unknown SSM status: %s\n' "$status" >&2; exit 1 ;;
    esac
  fi

  ((attempt < POLL_ATTEMPTS)) && sleep "$POLL_DELAY_SECONDS"
done

printf 'SSM command did not reach Success within %s attempts\n' "$POLL_ATTEMPTS" >&2
exit 1
```

Set executable mode before running the focused suite:

```bash
chmod +x deploy/wait-for-ssm-command.sh deploy/tests/ssm_wait_test.sh
```

- [ ] **Step 5: Replace cancellation, roles, shell interpolation, and waiter behavior in the workflow**

Make workflow cancellation conditional:

```yaml
concurrency:
  group: ci-cd-${{ github.ref }}
  cancel-in-progress: ${{ github.event_name == 'pull_request' }}
```

Use `vars.AWS_PUBLISH_ROLE_ARN` in the publish credential step. Add checkout and production concurrency to deploy, then use `vars.AWS_DEPLOY_ROLE_ARN`:

```yaml
  deploy-to-ec2:
    concurrency:
      group: vehicle-maintenance-history-production
      cancel-in-progress: false
    steps:
      - name: Checkout repository
        uses: actions/checkout@v7
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v6
        with:
          role-to-assume: ${{ vars.AWS_DEPLOY_ROLE_ARN }}
```

Pass contexts through `env` in image and send steps. Replace the image step with:

```yaml
      - name: Set image reference
        id: image
        shell: bash
        env:
          ECR_REGISTRY: ${{ needs.publish-docker-image.outputs.ecr_registry }}
          ECR_REPOSITORY: ${{ vars.ECR_REPOSITORY }}
          AWS_REGION: ${{ vars.AWS_REGION }}
        run: |
          [[ "$AWS_REGION" == us-east-2 ]]
          [[ "$ECR_REPOSITORY" == mmetznerm/vehicle-maintenance-history ]]
          [[ "$ECR_REGISTRY" =~ ^[0-9]{12}\.dkr\.ecr\.us-east-2\.amazonaws\.com$ ]]
          echo "image_uri=$ECR_REGISTRY/$ECR_REPOSITORY" >> "$GITHUB_OUTPUT"
          echo "image_tag=sha-${GITHUB_SHA::7}" >> "$GITHUB_OUTPUT"
```

Replace the send step body with validated environment values:

```yaml
      - name: Send deployment command
        id: send-command
        shell: bash
        env:
          AWS_REGION: ${{ vars.AWS_REGION }}
          EC2_INSTANCE_ID: ${{ vars.EC2_INSTANCE_ID }}
          IMAGE_URI: ${{ steps.image.outputs.image_uri }}
          IMAGE_TAG: ${{ steps.image.outputs.image_tag }}
        run: |
          [[ "$AWS_REGION" == us-east-2 ]]
          [[ "$EC2_INSTANCE_ID" =~ ^i-[0-9a-f]{8,17}$ ]]
          [[ "$IMAGE_URI" =~ ^[0-9]{12}\.dkr\.ecr\.us-east-2\.amazonaws\.com/mmetznerm/vehicle-maintenance-history$ ]]
          [[ "$IMAGE_TAG" =~ ^sha-[0-9a-f]{7}$ ]]
          printf -v command '%s %s %s' /opt/vehicle-maintenance-history/deploy.sh "$IMAGE_URI" "$IMAGE_TAG"
          parameters="$(jq -cn --arg command "$command" '{commands: [$command]}')"
          command_id="$(aws ssm send-command \
            --region "$AWS_REGION" \
            --instance-ids "$EC2_INSTANCE_ID" \
            --document-name AWS-RunShellScript \
            --comment "Deploy $IMAGE_TAG" \
            --parameters "$parameters" \
            --query Command.CommandId \
            --output text)"
          echo "command_id=$command_id" >> "$GITHUB_OUTPUT"
```

The strict character validation makes the unquoted remote arguments safe; `jq` protects the JSON transport.

```bash
printf -v command '%s %s %s' /opt/vehicle-maintenance-history/deploy.sh "$IMAGE_URI" "$IMAGE_TAG"
parameters="$(jq -cn --arg command "$command" '{commands: [$command]}')"
```

Replace the waiter command with:

```yaml
      - name: Poll deployment command
        id: poll-command
        continue-on-error: true
        shell: bash
        env:
          AWS_REGION: ${{ vars.AWS_REGION }}
          COMMAND_ID: ${{ steps.send-command.outputs.command_id }}
          EC2_INSTANCE_ID: ${{ vars.EC2_INSTANCE_ID }}
        run: |
          set +e
          bash deploy/wait-for-ssm-command.sh "$COMMAND_ID" "$EC2_INSTANCE_ID"
          poll_exit_code=$?
          set -e
          echo "exit_code=$poll_exit_code" >> "$GITHUB_OUTPUT"
          exit "$poll_exit_code"
```

Pass diagnostic contexts by `env`; the final step must use `POLL_EXIT_CODE: ${{ steps.poll-command.outputs.exit_code }}` and `[[ "$POLL_EXIT_CODE" == 0 ]]`.

- [ ] **Step 6: Strengthen the existing workflow contract for the new behavior**

Update checkout counts for the new deploy checkout. Assert the workflow-level conditional cancellation; publish/deploy role variables occur only in their owning jobs; deploy concurrency is non-canceling; checkout precedes poller execution; no `aws ssm wait`; contexts are supplied by `env`; and the final failure uses `poll-command.outputs.exit_code`.

- [ ] **Step 7: Run focused tests and linters**

```bash
bash deploy/tests/ssm_wait_test.sh
bash deploy/tests/workflow_contract_test.sh
docker run --rm -v "$PWD:/mnt" koalaman/shellcheck:stable \
  /mnt/deploy/wait-for-ssm-command.sh \
  /mnt/deploy/tests/ssm_wait_test.sh \
  /mnt/deploy/tests/workflow_contract_test.sh
docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest
```

Expected: polling cases and workflow contract print `PASS`; both linters exit zero without findings.

- [ ] **Step 8: Commit workflow safety**

```bash
git add deploy/wait-for-ssm-command.sh deploy/tests/ssm_wait_test.sh deploy/tests/testlib.sh deploy/tests/workflow_contract_test.sh .github/workflows/ci-cd.yml
git commit -m "ci: harden serialized SSM deployments"
```

---

## Task 4: Run deployment contracts as a required CI gate

**Files:**

- Modify: `.github/workflows/ci-cd.yml`
- Modify: `deploy/tests/workflow_contract_test.sh`

**Interfaces:**

- Consumes: all deployment test scripts and Docker on `ubuntu-latest`.
- Produces: required `deployment-contracts` job; `publish-docker-image.needs` includes it.

- [ ] **Step 1: Add failing job-scoped contract assertions**

Strip full-line YAML comments before job extraction:

```bash
ci_workflow="$(grep -Ev '^[[:space:]]*#' "$ci_path")"
codeql_workflow="$(grep -Ev '^[[:space:]]*#' "$codeql_path")"
```

Update the exact checkout assertions to seven total references after the deploy and contract jobs add one checkout each:

```bash
assert_count "$all_workflows" "actions/checkout@v7" 7
assert_count "$all_workflows" "uses: actions/checkout@" 7
```

Require `deployment-contracts` and assert exact steps/commands for checkout, all five test scripts, ShellCheck, actionlint, and Compose render. Also assert in `publish-docker-image`:

```bash
assert_contains "$publish_job" 'deployment-contracts'
assert_contains "$publish_job" "if: github.event_name == 'push' && github.ref == 'refs/heads/main'"
assert_contains "$build_push_step" 'platforms: linux/amd64'
assert_contains "$build_push_step" 'push: true'
assert_contains "$metadata_step" 'type=raw,value=latest'
assert_contains "$metadata_step" 'type=sha,prefix=sha-,format=short'
```

Add temporary mutation checks at the end of the script. They copy only workflow inputs, invoke the same contract with recursion disabled, and never alter the repository:

```bash
assert_workflow_mutation_fails() {
  local mutation="$1"
  local mutation_root status
  mutation_root="$(mktemp -d)"
  mkdir -p "$mutation_root/.github"
  cp -R "$repo_root/.github/workflows" "$mutation_root/.github/workflows"
  case "$mutation" in
    missing-contract-need)
      sed -i '/^      - deployment-contracts$/d' "$mutation_root/.github/workflows/ci-cd.yml"
      ;;
    push-disabled)
      sed -i 's/^          push: true$/          push: false/' "$mutation_root/.github/workflows/ci-cd.yml"
      ;;
    shared-aws-role)
      sed -i 's/vars.AWS_DEPLOY_ROLE_ARN/vars.AWS_PUBLISH_ROLE_ARN/' "$mutation_root/.github/workflows/ci-cd.yml"
      ;;
    cancel-running-deploy)
      sed -i 's/^      cancel-in-progress: false$/      cancel-in-progress: true/' "$mutation_root/.github/workflows/ci-cd.yml"
      ;;
    missing-ssm-poller)
      sed -i 's#bash deploy/wait-for-ssm-command.sh "$COMMAND_ID" "$EC2_INSTANCE_ID"#true#' "$mutation_root/.github/workflows/ci-cd.yml"
      ;;
    *) fail "unknown workflow mutation: $mutation" ;;
  esac
  set +e
  WORKFLOW_CONTRACT_ROOT="$mutation_root" \
    VMH_SKIP_WORKFLOW_MUTATIONS=1 \
    bash "${BASH_SOURCE[0]}" >/dev/null 2>&1
  status=$?
  set -e
  rm -rf "$mutation_root"
  [[ $status -ne 0 ]] || fail "workflow mutation must fail: $mutation"
}

if [[ "${VMH_SKIP_WORKFLOW_MUTATIONS:-0}" != 1 ]]; then
  assert_workflow_mutation_fails missing-contract-need
  assert_workflow_mutation_fails push-disabled
  assert_workflow_mutation_fails shared-aws-role
  assert_workflow_mutation_fails cancel-running-deploy
  assert_workflow_mutation_fails missing-ssm-poller
fi
```

- [ ] **Step 2: Run the contract and confirm RED**

Run `bash deploy/tests/workflow_contract_test.sh`.

Expected: focused failure because `deployment-contracts` does not yet exist.

- [ ] **Step 3: Add the required deployment-contracts job**

Add before publish:

```yaml
  deployment-contracts:
    name: Deployment Contracts
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v7
      - name: Run deployment contract tests
        run: |
          bash deploy/tests/compose_prod_test.sh
          bash deploy/tests/deploy_test.sh
          bash deploy/tests/setup_ec2_test.sh
          bash deploy/tests/ssm_wait_test.sh
          bash deploy/tests/workflow_contract_test.sh
      - name: Run ShellCheck
        run: |
          docker run --rm -v "$PWD:/mnt" koalaman/shellcheck:stable \
            /mnt/deploy/deploy.sh \
            /mnt/deploy/setup-ec2.sh \
            /mnt/deploy/wait-for-ssm-command.sh \
            /mnt/deploy/tests/testlib.sh \
            /mnt/deploy/tests/compose_prod_test.sh \
            /mnt/deploy/tests/deploy_test.sh \
            /mnt/deploy/tests/setup_ec2_test.sh \
            /mnt/deploy/tests/ssm_wait_test.sh \
            /mnt/deploy/tests/workflow_contract_test.sh
      - name: Run actionlint
        run: docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest
      - name: Render production Compose
        env:
          IMAGE_URI: registry.example.invalid/mmetznerm/vehicle-maintenance-history
          IMAGE_TAG: sha-0123abc
          SPRING_DATASOURCE_URL: jdbc:postgresql://db.internal:5432/vehicle_maintenance_history?sslmode=require
          SPRING_DATASOURCE_USERNAME: vmh_app
          SPRING_DATASOURCE_PASSWORD: 0123456789abcdef0123456789abcdef
          JWT_SECRET: abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789
          JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=70.0
        run: docker compose -f deploy/compose.prod.yml config --quiet
```

Append `deployment-contracts` to `publish-docker-image.needs`.

- [ ] **Step 4: Execute mutation, workflow, and regression gates**

```bash
bash deploy/tests/workflow_contract_test.sh
bash deploy/tests/compose_prod_test.sh
bash deploy/tests/deploy_test.sh
bash deploy/tests/setup_ec2_test.sh
bash deploy/tests/ssm_wait_test.sh
docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest
```

Expected: original contract and all regression suites pass; internal mutations fail as expected; actionlint has no findings.

- [ ] **Step 5: Commit the CI gate**

```bash
git add .github/workflows/ci-cd.yml deploy/tests/workflow_contract_test.sh
git commit -m "ci: require deployment contract checks"
```

---

## Task 5: Document immutable ECR, encrypted EBS, and split IAM roles

**Files:**

- Modify: `docs/aws-deployment.md`
- Modify: `README.md`

**Interfaces:**

- Consumes: `AWS_PUBLISH_ROLE_ARN`, `AWS_DEPLOY_ROLE_ARN`, ECR `IMMUTABLE_WITH_EXCLUSION`, encrypted gp3, exact Parameter Store ARNs, and new CI behavior.
- Produces: the operator-facing sequence for configuring AWS/GitHub before enabling deployment.

- [ ] **Step 1: Update ECR provenance instructions with exact commands**

In section 2 require **Immutable with exclusion**, wildcard `latest`, then include:

```bash
aws ecr put-image-tag-mutability \
  --region us-east-2 \
  --repository-name mmetznerm/vehicle-maintenance-history \
  --image-tag-mutability IMMUTABLE_WITH_EXCLUSION \
  --image-tag-mutability-exclusion-filters filterType=WILDCARD,filter=latest

aws ecr describe-repositories \
  --region us-east-2 \
  --repository-names mmetznerm/vehicle-maintenance-history \
  --query 'repositories[0].{mutability:imageTagMutability,exclusions:imageTagMutabilityExclusionFilters}' \
  --output json
```

State that the result must show `IMMUTABLE_WITH_EXCLUSION` and only `latest`; an existing SHA cannot be republished by a rerun.

- [ ] **Step 2: Split the GitHub role section and policies**

Keep the existing OIDC trust policy on both roles. Replace the combined role with:

- `github-actions-vehicle-maintenance-history-publish`: the existing `EcrLogin` and `EcrPushApplication` statements only;
- `github-actions-vehicle-maintenance-history-deploy`: the existing `SsmSendToSelectedInstance` and `SsmCommandRead` statements only.

Explicitly state that neither role may receive the other role's permissions.

- [ ] **Step 3: Restrict EC2 parameters and require encrypted storage**

Change `ReadApplicationParameters.Resource` to:

```json
[
  "arn:aws:ssm:us-east-2:675244612319:parameter/vmh/prod/app-env",
  "arn:aws:ssm:us-east-2:675244612319:parameter/vmh/prod/rds-master-password"
]
```

In EC2 section 8 require `10 GiB`, `gp3`, **Encrypted: Yes**, default account EBS key, and **Metadata version: V2 only**. Add EBS encryption to acceptance checks.

- [ ] **Step 4: Update GitHub variables and operational notes**

Replace `AWS_ROLE_ARN` with:

```text
AWS_PUBLISH_ROLE_ARN=arn:aws:iam::675244612319:role/github-actions-vehicle-maintenance-history-publish
AWS_DEPLOY_ROLE_ARN=arn:aws:iam::675244612319:role/github-actions-vehicle-maintenance-history-deploy
```

Explain that production deploys are serialized in Actions and again by `/var/lock/vehicle-maintenance-history-deploy.lock`, and SSM may poll for up to 15 minutes. Mention ECR scanning/lifecycle policy as optional, without making them acceptance prerequisites.

- [ ] **Step 5: Update README architecture and CI gates**

State that pull requests run deployment contracts, ShellCheck, actionlint, and Compose rendering; publish and deploy assume different OIDC roles; ECR SHA tags are immutable; and EC2 uses encrypted EBS.

- [ ] **Step 6: Run documentation hygiene checks**

```bash
rg -n "IMMUTABLE_WITH_EXCLUSION|AWS_PUBLISH_ROLE_ARN|AWS_DEPLOY_ROLE_ARN|Encrypted|deployment-contracts|vehicle-maintenance-history-deploy.lock" README.md docs/aws-deployment.md
rg -n "AWS_ROLE_ARN|parameter/vmh/prod/\*|AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|fedcba9876543210|abcdef0123456789abcdef" README.md docs/aws-deployment.md && exit 1 || true
git diff --check
```

Expected: required hardening terms are found; obsolete broad role/parameter names and credential literals are absent; diff check is clean.

- [ ] **Step 7: Commit hardening documentation**

```bash
git add README.md docs/aws-deployment.md
git commit -m "docs: harden AWS deployment operations"
```

---

## Task 6: Run the complete hardened release verification

**Files:**

- Verify: `deploy/*.sh`
- Verify: `deploy/tests/*.sh`
- Verify: `.github/workflows/*.yml`
- Verify: `README.md`
- Verify: `docs/aws-deployment.md`
- Verify: both AWS deployment design specifications

**Interfaces:**

- Consumes: Tasks 1–5 at one HEAD.
- Produces: fresh evidence that the hardening is ready for branch integration without touching AWS.

- [ ] **Step 1: Run every deployment contract**

```bash
bash deploy/tests/compose_prod_test.sh
bash deploy/tests/deploy_test.sh
bash deploy/tests/setup_ec2_test.sh
bash deploy/tests/ssm_wait_test.sh
bash deploy/tests/workflow_contract_test.sh
```

Expected: all suites exit zero and print their `PASS` results.

- [ ] **Step 2: Run shell, workflow, and Compose gates**

```bash
docker run --rm -v "$PWD:/mnt" koalaman/shellcheck:stable \
  /mnt/deploy/deploy.sh /mnt/deploy/setup-ec2.sh /mnt/deploy/wait-for-ssm-command.sh \
  /mnt/deploy/tests/testlib.sh /mnt/deploy/tests/compose_prod_test.sh \
  /mnt/deploy/tests/deploy_test.sh /mnt/deploy/tests/setup_ec2_test.sh \
  /mnt/deploy/tests/ssm_wait_test.sh /mnt/deploy/tests/workflow_contract_test.sh
docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest
IMAGE_URI=registry.example.invalid/mmetznerm/vehicle-maintenance-history \
IMAGE_TAG=sha-0123abc \
SPRING_DATASOURCE_URL='jdbc:postgresql://db.internal:5432/vehicle_maintenance_history?sslmode=require' \
SPRING_DATASOURCE_USERNAME=vmh_app \
SPRING_DATASOURCE_PASSWORD=0123456789abcdef0123456789abcdef \
JWT_SECRET=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789 \
JAVA_TOOL_OPTIONS='-XX:MaxRAMPercentage=70.0' \
docker compose -f deploy/compose.prod.yml config --quiet
```

Expected: all commands exit zero; ShellCheck, actionlint, and quiet Compose render produce no findings.

- [ ] **Step 3: Run backend and frontend quality suites**

```powershell
Set-Location backend
.\mvnw.cmd -B verify
.\mvnw.cmd -B verify -Pintegration-tests
Set-Location ..
```

```bash
npm --prefix frontend ci
npm --prefix frontend run lint
npm --prefix frontend run test:coverage
npm --prefix frontend run build
```

Expected: Maven unit and integration builds succeed; frontend lint, tests/coverage, and production build pass.

- [ ] **Step 4: Build the target image**

```bash
docker build --platform linux/amd64 -f backend/Dockerfile -t vehicle-maintenance-history:aws-hardening-verify .
```

Expected: the multi-stage image builds for `linux/amd64`.

- [ ] **Step 5: Check hygiene and specification coverage**

```bash
git diff --check origin/main...HEAD
git status --short
git diff --exit-code origin/main...HEAD -- docker-compose.yml
if rg -n "AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY" \
  .github README.md docs/aws-deployment.md deploy/deploy.sh deploy/setup-ec2.sh deploy/wait-for-ssm-command.sh; then exit 1; fi
```

Expected: diff and development Compose checks are clean, worktree is clean, and no credential/private-key pattern is present. Compare every requirement in both design specs with the final files and confirm that no excluded infrastructure was introduced.

- [ ] **Step 6: Perform final whole-branch review**

Request a read-only review of `origin/main..HEAD` on the most capable available model. Critical or Important findings must be fixed and all affected gates rerun. Do not enable `AWS_DEPLOY_ENABLED` or perform AWS acceptance checks before merge and the manual bootstrap.

---
