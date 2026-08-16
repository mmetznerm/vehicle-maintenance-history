#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
DEPLOY_SCRIPT="$PROJECT_DIR/deploy/deploy.sh"

# testlib is resolved from the test script's runtime directory.
# shellcheck disable=SC1091
source "$SCRIPT_DIR/testlib.sh"

APP_ENV='SPRING_DATASOURCE_URL=jdbc:postgresql://db.internal:5432/vehicle_maintenance_history?sslmode=require
SPRING_DATASOURCE_USERNAME=vmh_app
SPRING_DATASOURCE_PASSWORD=0123456789abcdef0123456789abcdef
JWT_SECRET=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70.0'

setup_case() {
  CASE_DIR="$(mktemp -d)"
  BIN_DIR="$CASE_DIR/bin"
  LOG_FILE="$CASE_DIR/commands.log"
  APP_DIR="$CASE_DIR/app"
  mkdir -p "$BIN_DIR"

  cat >"$BIN_DIR/aws" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'aws %s\n' "$*" >>"$VMH_TEST_LOG"
if [[ "$1 $2" == 'ssm get-parameter' ]]; then
  printf '%s\n' "$VMH_TEST_APP_ENV"
elif [[ "$1 $2" == 'ecr get-login-password' ]]; then
  printf 'mock-ecr-password\n'
fi
STUB

  cat >"$BIN_DIR/docker" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
if [[ "$1" == 'login' ]]; then
  password="$(cat)"
  printf 'docker %s password=%s\n' "$*" "$password" >>"$VMH_TEST_LOG"
else
  printf 'docker %s\n' "$*" >>"$VMH_TEST_LOG"
fi
STUB

  cat >"$BIN_DIR/curl" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'curl %s\n' "$*" >>"$VMH_TEST_LOG"
if [[ ! -e "$VMH_TEST_CURL_OUTCOMES_FILE" ]]; then
  printf '%s' "$VMH_TEST_CURL_OUTCOMES" >"$VMH_TEST_CURL_OUTCOMES_FILE"
fi
outcomes="$(<"$VMH_TEST_CURL_OUTCOMES_FILE")"
outcome="${outcomes%%,*}"
if [[ "$outcomes" == *,* ]]; then
  printf '%s' "${outcomes#*,}" >"$VMH_TEST_CURL_OUTCOMES_FILE"
else
  : >"$VMH_TEST_CURL_OUTCOMES_FILE"
fi
case "$outcome" in
  success) printf '{"status":"UP"}\n'; exit 0 ;;
  *) exit 1 ;;
esac
STUB

  cat >"$BIN_DIR/chmod" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'chmod %s\n' "$*" >>"$VMH_TEST_LOG"
exec /usr/bin/chmod "$@"
STUB

  /usr/bin/chmod +x "$BIN_DIR/aws" "$BIN_DIR/docker" "$BIN_DIR/curl" "$BIN_DIR/chmod"
}

run_deploy() {
  VMH_TEST_LOG="$LOG_FILE" \
    VMH_TEST_APP_ENV="$APP_ENV" \
    VMH_TEST_CURL_OUTCOMES_FILE="$CASE_DIR/curl-outcomes" \
    VMH_TEST_CURL_OUTCOMES="${1:-success}" \
    VMH_APP_DIR="$APP_DIR" \
    PATH="$BIN_DIR:$PATH" \
    "$DEPLOY_SCRIPT" "${@:2}"
}

case_without_arguments_prints_usage() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  set +e
  output="$(run_deploy success 2>&1)"
  status=$?
  set -e

  [[ $status -ne 0 ]] || fail 'deploy without arguments must fail'
  assert_contains "$output" 'IMAGE_URI IMAGE_TAG'
  printf 'PASS: no arguments print usage\n'
)

case_latest_tag_is_rejected_before_external_commands() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  set +e
  output="$(run_deploy success '123456789012.dkr.ecr.us-east-2.amazonaws.com/vmh' latest 2>&1)"
  status=$?
  set -e

  [[ $status -ne 0 ]] || fail 'latest tag must fail'
  assert_contains "$output" 'IMAGE_TAG must match'
  [[ ! -e "$LOG_FILE" ]] || fail 'latest tag must not call aws or docker'
  printf 'PASS: latest tag is rejected before external commands\n'
)

case_deploys_sha_tag_with_environment_and_compose_rollout() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  run_deploy success '123456789012.dkr.ecr.us-east-2.amazonaws.com/vmh' sha-0123abc

  log="$(<"$LOG_FILE")"
  env_file="$(<"$APP_DIR/.env")"
  assert_contains "$log" 'aws ssm get-parameter --name /vmh/prod/app-env --with-decryption --query Parameter.Value --output text --region us-east-2'
  assert_contains "$log" 'aws ecr get-login-password --region us-east-2'
  assert_contains "$log" 'docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-2.amazonaws.com password=mock-ecr-password'
  assert_contains "$log" "docker compose --env-file $APP_DIR/.env -f $APP_DIR/compose.prod.yml pull"
  assert_contains "$log" "docker compose --env-file $APP_DIR/.env -f $APP_DIR/compose.prod.yml up -d --remove-orphans"
  assert_contains "$env_file" "$APP_ENV"
  assert_contains "$env_file" 'IMAGE_URI=123456789012.dkr.ecr.us-east-2.amazonaws.com/vmh'
  assert_contains "$env_file" 'IMAGE_TAG=sha-0123abc'
  assert_contains "$log" "chmod 600 $APP_DIR/.env."
  case "$(uname -s)" in
    MINGW*|MSYS*) ;;
    *) assert_file_mode 600 "$APP_DIR/.env" ;;
  esac
  printf 'PASS: sha deployment retrieves secrets, logs in, and rolls out compose\n'
)

case_health_check_succeeds_after_retries() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  VMH_HEALTH_ATTEMPTS=3 VMH_HEALTH_DELAY_SECONDS=0 run_deploy fail,fail,success '123456789012.dkr.ecr.us-east-2.amazonaws.com/vmh' sha-0123abc

  log="$(<"$LOG_FILE")"
  curl_count="$(grep -c '^curl ' "$LOG_FILE")"
  [[ "$curl_count" == 3 ]] || fail "expected three health probes, got $curl_count"
  assert_contains "$log" 'curl -fsS http://localhost/actuator/health'
  printf 'PASS: health check retries until success\n'
)

case_health_check_fails_after_all_attempts() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  set +e
  VMH_HEALTH_ATTEMPTS=2 VMH_HEALTH_DELAY_SECONDS=0 run_deploy fail,fail '123456789012.dkr.ecr.us-east-2.amazonaws.com/vmh' sha-0123abc
  status=$?
  set -e

  [[ $status -ne 0 ]] || fail 'permanently unhealthy deployment must fail'
  curl_count="$(grep -c '^curl ' "$LOG_FILE")"
  [[ "$curl_count" == 2 ]] || fail "expected two health probes, got $curl_count"
  printf 'PASS: health check fails after all attempts\n'
)

case_without_arguments_prints_usage
case_latest_tag_is_rejected_before_external_commands
case_deploys_sha_tag_with_environment_and_compose_rollout
case_health_check_succeeds_after_retries
case_health_check_fails_after_all_attempts
