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

REGION="${AWS_REGION:-us-east-2}"
EXPECTED_IMAGE_URI="675244612319.dkr.ecr.us-east-2.amazonaws.com/mmetznerm/vehicle-maintenance-history"
if [[ "$REGION" != us-east-2 || "$IMAGE_URI" != "$EXPECTED_IMAGE_URI" ]]; then
  printf 'IMAGE_URI must equal the configured production ECR repository\n' >&2
  exit 2
fi

if [[ "$(id -u)" -ne 0 ]]; then
  printf 'setup-ec2.sh must run as root\n' >&2
  exit 1
fi

APP_DIR="${VMH_APP_DIR:-/opt/vehicle-maintenance-history}"
SOURCE_DIR="${VMH_SOURCE_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"
ENV_PARAMETER="${VMH_ENV_PARAMETER:-/vmh/prod/app-env}"
MASTER_PARAMETER="${VMH_MASTER_PARAMETER:-/vmh/prod/rds-master-password}"
SWAP_FILE="${VMH_SWAP_FILE:-/swapfile}"
FSTAB_FILE="${VMH_FSTAB_FILE:-/etc/fstab}"
COMPOSE_PLUGIN_DIR="${VMH_COMPOSE_PLUGIN_DIR:-/usr/local/lib/docker/cli-plugins}"
TMP_DIR="${VMH_TMPDIR:-/tmp}"
TEMP_FILES=()

cleanup() {
  local path
  unset MASTER_PASSWORD APP_DB_PASSWORD
  for path in "${TEMP_FILES[@]}"; do
    rm -rf "$path"
  done
}
trap cleanup EXIT

ensure_swap_persistence() {
  grep -Fqx "$SWAP_FILE none swap sw 0 0" "$FSTAB_FILE" ||
    printf '%s none swap sw 0 0\n' "$SWAP_FILE" >>"$FSTAB_FILE"
}

prepare_swap() {
  local swap_type swap_tmp

  if swapon --show=NAME --noheadings | grep -Fxq "$SWAP_FILE"; then
    ensure_swap_persistence
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
  ensure_swap_persistence
}

prepare_host() {
  local compose_tmp checksum_file

  dnf install -y docker
  systemctl enable --now docker

  prepare_swap

  if ! docker compose version >/dev/null 2>&1; then
    mkdir -p "$TMP_DIR" "$COMPOSE_PLUGIN_DIR"
    compose_tmp="$(mktemp -d "$TMP_DIR/compose.XXXXXX")"
    TEMP_FILES+=("$compose_tmp")
    curl -fsSL "https://github.com/docker/compose/releases/download/v5.4.0/checksums.txt" -o "$compose_tmp/checksums.txt"
    curl -fsSL "https://github.com/docker/compose/releases/download/v5.4.0/docker-compose-linux-x86_64" -o "$compose_tmp/docker-compose-linux-x86_64"
    checksum_file="$compose_tmp/docker-compose-linux-x86_64.checksum"
    if ! grep -E '^[[:xdigit:]]{64} [ *]docker-compose-linux-x86_64$' "$compose_tmp/checksums.txt" >"$checksum_file"; then
      printf 'Docker Compose checksum entry is missing\n' >&2
      exit 1
    fi
    (
      cd "$compose_tmp"
      sha256sum -c "$(basename "$checksum_file")"
    )
    install -m 0755 "$compose_tmp/docker-compose-linux-x86_64" "$COMPOSE_PLUGIN_DIR/docker-compose"
  fi
  docker compose version >/dev/null
}

if [[ "${VMH_SKIP_HOST_SETUP:-}" != 1 ]]; then
  prepare_host
fi

mkdir -p "$APP_DIR" "$TMP_DIR"
cp "$SOURCE_DIR/compose.prod.yml" "$APP_DIR/compose.prod.yml"
cp "$SOURCE_DIR/deploy.sh" "$APP_DIR/deploy.sh"
chmod 0644 "$APP_DIR/compose.prod.yml"
chmod 0755 "$APP_DIR/deploy.sh"

app_env_file="$(mktemp "$TMP_DIR/app-env.XXXXXX")"
TEMP_FILES+=("$app_env_file")
chmod 600 "$app_env_file"
aws ssm get-parameter \
  --name "$ENV_PARAMETER" \
  --with-decryption \
  --query Parameter.Value \
  --output text \
  --region "$REGION" >"$app_env_file"
MASTER_PASSWORD="$(aws ssm get-parameter \
  --name "$MASTER_PARAMETER" \
  --with-decryption \
  --query Parameter.Value \
  --output text \
  --region "$REGION")"

read_environment_value() {
  local key="$1"
  local value
  value="$(grep -E "^${key}=" "$app_env_file" | head -n 1 | cut -d= -f2-)"
  [[ -n "$value" ]] || {
    printf 'Missing required environment value: %s\n' "$key" >&2
    exit 1
  }
  printf '%s' "$value"
}

RDS_HOST="$(read_environment_value RDS_HOST)"
APP_DB_USERNAME="$(read_environment_value SPRING_DATASOURCE_USERNAME)"
APP_DB_PASSWORD="$(read_environment_value SPRING_DATASOURCE_PASSWORD)"
[[ "$APP_DB_USERNAME" == vmh_app ]] || {
  printf 'SPRING_DATASOURCE_USERNAME must be vmh_app\n' >&2
  exit 1
}

PGPASSWORD="$MASTER_PASSWORD" APP_DB_PASSWORD="$APP_DB_PASSWORD" RDS_HOST="$RDS_HOST" PGSSLMODE=require \
  docker run --rm -i --network host \
    -e PGPASSWORD \
    -e APP_DB_PASSWORD \
    -e RDS_HOST \
    -e PGSSLMODE \
    postgres:16-alpine \
    sh -ceu 'exec psql --host "$RDS_HOST" --port 5432 --username vmh_admin --dbname postgres --set=ON_ERROR_STOP=1 --set=app_password="$APP_DB_PASSWORD"' <<'SQL'
SELECT format('CREATE ROLE vmh_app LOGIN PASSWORD %L', :'app_password')
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'vmh_app')
\gexec

SELECT format('ALTER ROLE vmh_app WITH LOGIN PASSWORD %L', :'app_password')
\gexec

SELECT 'CREATE DATABASE vehicle_maintenance_history OWNER vmh_app'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'vehicle_maintenance_history')
\gexec
SQL

"$APP_DIR/deploy.sh" "$IMAGE_URI" "$IMAGE_TAG"
printf 'Bootstrap complete. After confirming the application is healthy, delete %s.\n' "$MASTER_PARAMETER"
