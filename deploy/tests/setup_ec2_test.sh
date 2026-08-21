#!/usr/bin/env bash
set -euo pipefail

TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$TEST_DIR/../.." && pwd)"
SETUP_SCRIPT="$REPO_ROOT/deploy/setup-ec2.sh"

# testlib is resolved from the test script's runtime directory.
# shellcheck disable=SC1091
source "$TEST_DIR/testlib.sh"

IMAGE_URI='675244612319.dkr.ecr.us-east-2.amazonaws.com/mmetznerm/vehicle-maintenance-history'
IMAGE_TAG='sha-0123abc'
MASTER_PASSWORD='fedcba9876543210fedcba9876543210'
APP_PASSWORD='0123456789abcdef0123456789abcdef'
APP_ENV="RDS_HOST=db.internal
SPRING_DATASOURCE_URL=jdbc:postgresql://db.internal:5432/vehicle_maintenance_history?sslmode=require
SPRING_DATASOURCE_USERNAME=vmh_app
SPRING_DATASOURCE_PASSWORD=$APP_PASSWORD
JWT_SECRET=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"

setup_case() {
  CASE_DIR="$(mktemp -d)"
  BIN_DIR="$CASE_DIR/bin"
  LOG_FILE="$CASE_DIR/commands.log"
  SQL_FILE="$CASE_DIR/bootstrap.sql"
  mkdir -p "$BIN_DIR"
  : >"$CASE_DIR/fstab"

  cat >"$BIN_DIR/aws" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'aws %s\n' "$*" >>"$VMH_TEST_LOG"
case " $* " in
  *' --name /vmh/prod/app-env '*) printf '%s\n' "$VMH_TEST_APP_ENV" ;;
  *' --name /vmh/prod/rds-master-password '*) printf '%s\n' "$VMH_TEST_MASTER_PASSWORD" ;;
  *' ecr get-login-password '*) printf 'mock-ecr-password\n' ;;
  *) exit 64 ;;
esac
STUB

  cat >"$BIN_DIR/id" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
[[ "$1" == '-u' ]]
printf '%s\n' "${VMH_TEST_EUID:-0}"
STUB

  cat >"$BIN_DIR/docker" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'docker %s\n' "$*" >>"$VMH_TEST_LOG"
if [[ "$1 ${2:-}" == 'compose version' ]]; then
  [[ -f "$VMH_TEST_COMPOSE_READY" ]] && exit 0
  exit 1
fi
if [[ "$1" == 'run' ]]; then
  printf 'docker run pgsslmode=%s\n' "${PGSSLMODE:-}" >>"$VMH_TEST_LOG"
  cat >"$VMH_TEST_SQL"
  if [[ "${VMH_TEST_DB_FAIL:-0}" == 1 ]]; then
    exit 42
  fi
fi
STUB

  cat >"$BIN_DIR/curl" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'curl %s\n' "$*" >>"$VMH_TEST_LOG"
output=''
url=''
while [[ $# -gt 0 ]]; do
  case "$1" in
    -o) output="$2"; shift 2 ;;
    *) url="$1"; shift ;;
  esac
done
case "$url" in
  *checksums.txt)
    if [[ "${VMH_TEST_MISSING_CHECKSUM_ENTRY:-0}" == 1 ]]; then
      printf 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa *docker-compose-linux-aarch64\n' >"$output"
    else
      printf 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa *docker-compose-linux-x86_64\n' >"$output"
    fi
    ;;
  *docker-compose-linux-x86_64) printf '#!/usr/bin/env bash\nexit 0\n' >"$output" ;;
  http://localhost/actuator/health) exit 0 ;;
  *) exit 64 ;;
esac
STUB

  cat >"$BIN_DIR/sha256sum" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'sha256sum %s\n' "$*" >>"$VMH_TEST_LOG"
[[ "$1" == '-c' ]]
if [[ "${VMH_TEST_BAD_CHECKSUM:-0}" == 1 ]]; then
  exit 1
fi
STUB

  cat >"$BIN_DIR/install" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'install %s\n' "$*" >>"$VMH_TEST_LOG"
source="${@: -2:1}"
destination="${@: -1}"
/usr/bin/mkdir -p "$(/usr/bin/dirname "$destination")"
/usr/bin/cp "$source" "$destination"
/usr/bin/chmod 755 "$destination"
: >"$VMH_TEST_COMPOSE_READY"
STUB

  cat >"$BIN_DIR/flock" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'flock %s\n' "$*" >>"$VMH_TEST_LOG"
[[ "${VMH_TEST_FLOCK_FAIL:-0}" != 1 ]]
STUB

  for command in dnf systemctl swapon dd chmod; do
    cat >"$BIN_DIR/$command" <<STUB
#!/usr/bin/env bash
set -euo pipefail
printf '$command %s\\n' "\$*" >>"\$VMH_TEST_LOG"
STUB
  done

  cat >>"$BIN_DIR/dd" <<'STUB'
for argument in "$@"; do
  case "$argument" in
    of=*) : >"${argument#of=}" ;;
  esac
done
STUB

  cat >>"$BIN_DIR/chmod" <<'STUB'
exec /usr/bin/chmod "$@"
STUB

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

  cat >"$BIN_DIR/swapon" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
printf 'swapon %s\n' "$*" >>"$VMH_TEST_LOG"
if [[ "$1" == '--show=NAME' ]]; then
  [[ -f "$VMH_TEST_SWAP_ACTIVE" ]] && printf '%s\n' "$VMH_SWAP_FILE"
  exit 0
fi
: >"$VMH_TEST_SWAP_ACTIVE"
STUB

  /usr/bin/chmod +x "$BIN_DIR"/*
}

run_setup() {
  VMH_TEST_LOG="$LOG_FILE" \
    VMH_TEST_SQL="$SQL_FILE" \
    VMH_TEST_APP_ENV="$APP_ENV" \
    VMH_TEST_MASTER_PASSWORD="$MASTER_PASSWORD" \
    VMH_TEST_COMPOSE_READY="$CASE_DIR/compose-ready" \
    VMH_TEST_SWAP_ACTIVE="$CASE_DIR/swap-active" \
    VMH_APP_DIR="$CASE_DIR/app" \
    VMH_SOURCE_DIR="$REPO_ROOT/deploy" \
    VMH_SWAP_FILE="$CASE_DIR/swapfile" \
    VMH_FSTAB_FILE="$CASE_DIR/fstab" \
    VMH_COMPOSE_PLUGIN_DIR="$CASE_DIR/cli-plugins" \
    VMH_TMPDIR="$CASE_DIR/tmp" \
    VMH_DEPLOY_LOCK_FILE="$CASE_DIR/deploy.lock" \
    VMH_DEPLOY_LOCK_TIMEOUT_SECONDS=0 \
    AWS_REGION="${VMH_TEST_AWS_REGION:-us-east-2}" \
    PATH="$BIN_DIR:$PATH" \
    "$SETUP_SCRIPT" "$@"
}

case_missing_arguments_print_usage() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  set +e
  output="$(VMH_SKIP_HOST_SETUP=1 run_setup 2>&1)"
  status=$?
  set -e

  [[ $status -ne 0 ]] || fail 'setup without image arguments must fail'
  assert_contains "$output" 'IMAGE_URI IMAGE_TAG'
  [[ ! -e "$LOG_FILE" ]] || fail 'argument validation must precede external commands'
  printf 'PASS: missing image arguments print usage\n'
)

case_non_root_fails_before_external_commands() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  set +e
  output="$(VMH_TEST_EUID=1000 VMH_ALLOW_NON_ROOT=1 VMH_SKIP_HOST_SETUP=1 run_setup "$IMAGE_URI" "$IMAGE_TAG" 2>&1)"
  status=$?
  set -e

  [[ $status -ne 0 ]] || fail 'non-root setup must fail without override'
  assert_contains "$output" 'must run as root'
  [[ ! -e "$LOG_FILE" ]] || fail 'root check must precede external commands'
  printf 'PASS: non-root execution is rejected before external commands\n'
)

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
  printf 'PASS: invalid image URI stops setup before external commands\n'
)

case_canonical_image_uri_stops_setup_outside_production_region_before_external_commands() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  set +e
  output="$(VMH_TEST_AWS_REGION=us-east-1 VMH_SKIP_HOST_SETUP=1 run_setup "$IMAGE_URI" "$IMAGE_TAG" 2>&1)"
  status=$?
  set -e

  [[ $status -ne 0 ]] || fail 'canonical setup image URI must fail outside the production region'
  assert_contains "$output" 'IMAGE_URI must equal the configured production ECR repository'
  [[ ! -e "$LOG_FILE" ]] || fail 'region validation must precede setup external commands'
  printf 'PASS: canonical image URI stops setup outside the production region before external commands\n'
)

case_success_bootstraps_database_and_deploys() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  VMH_SKIP_HOST_SETUP=1 run_setup "$IMAGE_URI" "$IMAGE_TAG"

  log="$(<"$LOG_FILE")"
  sql="$(<"$SQL_FILE")"
  assert_contains "$log" 'aws ssm get-parameter --name /vmh/prod/app-env --with-decryption --query Parameter.Value --output text --region us-east-2'
  assert_contains "$log" 'aws ssm get-parameter --name /vmh/prod/rds-master-password --with-decryption --query Parameter.Value --output text --region us-east-2'
  assert_contains "$log" 'docker run --rm -i --network host -e PGPASSWORD -e APP_DB_PASSWORD -e RDS_HOST -e PGSSLMODE postgres:16-alpine'
  assert_contains "$log" 'docker run pgsslmode=require'
  assert_file_exists "$CASE_DIR/app/compose.prod.yml"
  assert_file_exists "$CASE_DIR/app/deploy.sh"
  assert_executable "$CASE_DIR/app/deploy.sh"
  deployed_env="$(<"$CASE_DIR/app/.env")"
  assert_contains "$deployed_env" "IMAGE_URI=$IMAGE_URI"
  assert_contains "$deployed_env" "IMAGE_TAG=$IMAGE_TAG"
  assert_contains "$sql" "CREATE ROLE vmh_app LOGIN PASSWORD %L"
  assert_contains "$sql" "ALTER ROLE vmh_app WITH LOGIN PASSWORD %L"
  assert_contains "$sql" "CREATE DATABASE vehicle_maintenance_history OWNER vmh_app"
  assert_contains "$sql" '\gexec'
  assert_not_contains "$sql" "$MASTER_PASSWORD"
  assert_not_contains "$sql" "$APP_PASSWORD"
  assert_not_contains "$log" "$MASTER_PASSWORD"
  assert_not_contains "$log" "$APP_PASSWORD"
  printf 'PASS: setup retrieves parameters, initializes the database, and deploys\n'
)

case_database_client_failure_prevents_deploy() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  set +e
  VMH_SKIP_HOST_SETUP=1 VMH_TEST_DB_FAIL=1 run_setup "$IMAGE_URI" "$IMAGE_TAG"
  status=$?
  set -e

  [[ $status -ne 0 ]] || fail 'database-client failure must fail setup'
  log="$(<"$LOG_FILE")"
  assert_contains "$log" 'docker run'
  assert_not_contains "$log" 'docker login '
  printf 'PASS: database-client failure stops before deployment\n'
)

case_host_preparation_is_repeat_safe_and_verifies_compose() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  run_setup "$IMAGE_URI" "$IMAGE_TAG"
  run_setup "$IMAGE_URI" "$IMAGE_TAG"

  log="$(<"$LOG_FILE")"
  assert_contains "$log" 'dnf install -y docker'
  assert_contains "$log" 'systemctl enable --now docker'
  assert_contains "$log" 'curl -fsSL https://github.com/docker/compose/releases/download/v5.4.0/checksums.txt'
  assert_contains "$log" 'curl -fsSL https://github.com/docker/compose/releases/download/v5.4.0/docker-compose-linux-x86_64'
  assert_contains "$log" 'sha256sum -c'
  assert_contains "$log" 'install -m 0755'
  assert_contains "$log" 'dd if=/dev/zero'
  assert_contains "$log" 'mkswap '
  assert_contains "$log" 'swapon '
  assert_file_exists "$CASE_DIR/swapfile"
  [[ "$(grep -c '^dd ' "$LOG_FILE")" == 1 ]] || fail 'swapfile must only be created once'
  [[ "$(grep -c "^swapon $CASE_DIR/swapfile$" "$LOG_FILE")" == 1 ]] || fail 'swapfile must only be activated once'
  [[ "$(grep -Fc "$CASE_DIR/swapfile none swap sw 0 0" "$CASE_DIR/fstab")" == 1 ]] || fail 'fstab entry must not be duplicated'
  printf 'PASS: host preparation installs Docker, verified Compose, and swap\n'
)

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
  mv_command="$(awk -v destination="$CASE_DIR/swapfile" '$1 == "mv" && $2 == "-f" && $NF == destination { print; exit }' "$LOG_FILE")"
  [[ -n "$mv_command" ]] || fail 'invalid swap must be promoted from a temporary file'
  swap_tmp="${mv_command#mv -f }"
  swap_tmp="${swap_tmp% *}"
  mkswap_line="$(awk -v command="mkswap $swap_tmp" '$0 == command { print NR; exit }' "$LOG_FILE")"
  mv_line="$(awk -v command="$mv_command" '$0 == command { print NR; exit }' "$LOG_FILE")"
  [[ -n "$mkswap_line" && -n "$mv_line" ]] || fail 'temporary swap format and promotion must be logged'
  (( mkswap_line < mv_line )) || fail 'temporary swap must be formatted before promotion'
  printf 'PASS: invalid inactive swap is repaired atomically\n'
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
  printf 'PASS: valid inactive swap is reused\n'
)

case_active_swap_is_persisted_without_reactivation() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  : >"$CASE_DIR/swap-active"
  run_setup "$IMAGE_URI" "$IMAGE_TAG"
  run_setup "$IMAGE_URI" "$IMAGE_TAG"

  log="$(<"$LOG_FILE")"
  assert_not_contains "$log" 'blkid '
  assert_not_contains "$log" 'dd if=/dev/zero'
  assert_not_contains "$log" 'mkswap '
  assert_not_contains "$log" 'mv -f '
  assert_not_contains "$log" "swapon $CASE_DIR/swapfile"
  [[ "$(grep -Fc "$CASE_DIR/swapfile none swap sw 0 0" "$CASE_DIR/fstab")" == 1 ]] || fail 'active swap must have one persistent fstab entry'
  printf 'PASS: active swap is persisted without recreation or reactivation\n'
)

case_invalid_compose_checksum_prevents_installation() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  set +e
  VMH_TEST_BAD_CHECKSUM=1 run_setup "$IMAGE_URI" "$IMAGE_TAG"
  status=$?
  set -e

  [[ $status -ne 0 ]] || fail 'invalid Compose checksum must fail setup'
  log="$(<"$LOG_FILE")"
  assert_contains "$log" 'sha256sum -c'
  assert_not_contains "$log" 'install -m 0755'
  printf 'PASS: invalid Compose checksum prevents installation\n'
)

case_missing_compose_checksum_entry_reports_diagnostic() (
  setup_case
  trap 'rm -rf "$CASE_DIR"' EXIT

  set +e
  output="$(VMH_TEST_MISSING_CHECKSUM_ENTRY=1 run_setup "$IMAGE_URI" "$IMAGE_TAG" 2>&1)"
  status=$?
  set -e

  [[ $status -ne 0 ]] || fail 'missing Compose checksum entry must fail setup'
  assert_contains "$output" 'Docker Compose checksum entry is missing'
  log="$(<"$LOG_FILE")"
  assert_not_contains "$log" 'sha256sum -c'
  assert_not_contains "$log" 'install -m 0755'
  printf 'PASS: missing Compose checksum entry reports a diagnostic\n'
)

case_missing_arguments_print_usage
case_non_root_fails_before_external_commands
case_invalid_image_uri_stops_setup_before_external_commands
case_canonical_image_uri_stops_setup_outside_production_region_before_external_commands
case_success_bootstraps_database_and_deploys
case_database_client_failure_prevents_deploy
case_host_preparation_is_repeat_safe_and_verifies_compose
case_invalid_inactive_swap_is_repaired_atomically
case_valid_inactive_swap_is_reused
case_active_swap_is_persisted_without_reactivation
case_invalid_compose_checksum_prevents_installation
case_missing_compose_checksum_entry_reports_diagnostic
