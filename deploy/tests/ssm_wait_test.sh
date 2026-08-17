#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$TEST_DIR/../.." && pwd)"
WAIT_SCRIPT="$REPO_ROOT/deploy/wait-for-ssm-command.sh"
# shellcheck disable=SC1091
source "$TEST_DIR/testlib.sh"

# Keep ambient CI settings from changing this suite's controlled defaults.
unset VMH_SSM_POLL_ATTEMPTS \
  VMH_SSM_POLL_DELAY_SECONDS \
  VMH_SSM_EXECUTION_TIMEOUT_SECONDS \
  VMH_SSM_DELIVERY_TIMEOUT_SECONDS \
  VMH_SSM_POLL_MARGIN_SECONDS

setup_case() {
  CASE_DIR="$(mktemp -d)"
  BIN_DIR="$CASE_DIR/bin"
  LOG_FILE="$CASE_DIR/aws.log"
  OUTCOMES_FILE="$CASE_DIR/outcomes"
  SLEEP_LOG="$CASE_DIR/sleep.log"
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

  cat >"$BIN_DIR/sleep" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'sleep %s\n' "$*" >>"$VMH_TEST_SLEEP_LOG"
STUB
  chmod +x "$BIN_DIR/sleep"
}

run_wait_with_instance() {
  local instance_id="$1"
  local outcomes="$2"
  printf '%s' "$outcomes" >"$OUTCOMES_FILE"
  : >"$LOG_FILE"
  set +e
  WAIT_OUTPUT="$(VMH_TEST_LOG="$LOG_FILE" \
    VMH_TEST_OUTCOMES_FILE="$OUTCOMES_FILE" \
    VMH_TEST_SLEEP_LOG="$SLEEP_LOG" \
    VMH_SSM_POLL_ATTEMPTS="${VMH_SSM_POLL_ATTEMPTS:-4}" \
    VMH_SSM_POLL_DELAY_SECONDS="${VMH_SSM_POLL_DELAY_SECONDS:-0}" \
    VMH_SSM_EXECUTION_TIMEOUT_SECONDS="${VMH_SSM_EXECUTION_TIMEOUT_SECONDS:-0}" \
    VMH_SSM_DELIVERY_TIMEOUT_SECONDS="${VMH_SSM_DELIVERY_TIMEOUT_SECONDS:-0}" \
    VMH_SSM_POLL_MARGIN_SECONDS="${VMH_SSM_POLL_MARGIN_SECONDS:-0}" \
    AWS_REGION=us-east-2 \
    PATH="$BIN_DIR:$PATH" \
    "$WAIT_SCRIPT" 11111111-2222-3333-4444-555555555555 "$instance_id" 2>&1)"
  WAIT_STATUS=$?
  set -e
}

run_wait() {
  run_wait_with_instance i-0123456789abcdef0 "$1"
}

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

case_undersized_poll_budget_fails_before_aws() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  VMH_SSM_POLL_ATTEMPTS=2 \
    VMH_SSM_POLL_DELAY_SECONDS=10 \
    VMH_SSM_EXECUTION_TIMEOUT_SECONDS=1200 \
    run_wait 'Success'
  [[ "$WAIT_STATUS" -ne 0 ]] || fail 'undersized poll budget must fail'
  [[ ! -s "$LOG_FILE" ]] || fail 'poll budget validation must precede AWS'
  [[ ! -s "$SLEEP_LOG" ]] || fail 'poll budget validation must precede sleep'
  assert_contains "$WAIT_OUTPUT" 'SSM polling budget of 10 seconds must cover delivery timeout of 0 seconds, remote execution timeout of 1200 seconds, and poll margin of 0 seconds'
)

case_undersized_delivery_execution_budget_fails_before_aws() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
    VMH_SSM_POLL_ATTEMPTS=2 \
    VMH_SSM_POLL_DELAY_SECONDS=10 \
    VMH_SSM_DELIVERY_TIMEOUT_SECONDS=60 \
    VMH_SSM_EXECUTION_TIMEOUT_SECONDS=0 \
    VMH_SSM_POLL_MARGIN_SECONDS=30 \
    run_wait 'Success'
  [[ "$WAIT_STATUS" -ne 0 ]] || fail 'poll budget must include delivery, execution, and margin'
  [[ ! -s "$LOG_FILE" ]] || fail 'complete poll budget validation must precede AWS'
  [[ ! -s "$SLEEP_LOG" ]] || fail 'complete poll budget validation must precede sleep'
  assert_contains "$WAIT_OUTPUT" 'SSM polling budget of 10 seconds must cover delivery timeout of 60 seconds, remote execution timeout of 0 seconds, and poll margin of 30 seconds'
)

case_poll_budget_requires_delivery_execution_and_margin() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  VMH_SSM_POLL_ATTEMPTS=129 \
    VMH_SSM_POLL_DELAY_SECONDS=10 \
    VMH_SSM_DELIVERY_TIMEOUT_SECONDS=60 \
    VMH_SSM_EXECUTION_TIMEOUT_SECONDS=1200 \
    VMH_SSM_POLL_MARGIN_SECONDS=30 \
    run_wait 'Success'
  [[ "$WAIT_STATUS" -ne 0 ]] || fail '1280-second poll budget must not omit delivery, execution, or margin'
  [[ ! -s "$LOG_FILE" ]] || fail 'full poll budget validation must precede AWS'
  [[ ! -s "$SLEEP_LOG" ]] || fail 'full poll budget validation must precede sleep'
  assert_contains "$WAIT_OUTPUT" 'SSM polling budget of 1280 seconds must cover delivery timeout of 60 seconds, remote execution timeout of 1200 seconds, and poll margin of 30 seconds'
)

case_invalid_remote_execution_timeout_fails_before_aws() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  VMH_SSM_EXECUTION_TIMEOUT_SECONDS=invalid run_wait 'Success'
  [[ "$WAIT_STATUS" -ne 0 ]] || fail 'invalid remote execution timeout must fail'
  [[ ! -s "$LOG_FILE" ]] || fail 'remote timeout validation must precede AWS'
)

case_invalid_delivery_timeout_fails_before_aws() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  VMH_SSM_DELIVERY_TIMEOUT_SECONDS=invalid run_wait 'Success'
  [[ "$WAIT_STATUS" -ne 0 ]] || fail 'invalid delivery timeout must fail'
  [[ ! -s "$LOG_FILE" ]] || fail 'delivery timeout validation must precede AWS'
  [[ ! -s "$SLEEP_LOG" ]] || fail 'delivery timeout validation must precede sleep'
)

case_invalid_poll_margin_fails_before_aws() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT
  VMH_SSM_POLL_MARGIN_SECONDS=invalid run_wait 'Success'
  [[ "$WAIT_STATUS" -ne 0 ]] || fail 'invalid poll margin must fail'
  [[ ! -s "$LOG_FILE" ]] || fail 'poll margin validation must precede AWS'
  [[ ! -s "$SLEEP_LOG" ]] || fail 'poll margin validation must precede sleep'
)

case_eventual_consistency_then_success
case_failed_status_is_failure
case_unknown_status_is_failure
case_deadline_is_failure
case_invalid_instance_id_fails_before_aws
case_undersized_poll_budget_fails_before_aws
case_undersized_delivery_execution_budget_fails_before_aws
case_poll_budget_requires_delivery_execution_and_margin
case_invalid_remote_execution_timeout_fails_before_aws
case_invalid_delivery_timeout_fails_before_aws
case_invalid_poll_margin_fails_before_aws

printf 'PASS: SSM command polling\n'
