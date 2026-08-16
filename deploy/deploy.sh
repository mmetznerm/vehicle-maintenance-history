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

mkdir -p "$APP_DIR"
temporary_env="$(mktemp "$APP_DIR/.env.XXXXXX")"
trap 'rm -f "$temporary_env"' EXIT

aws ssm get-parameter \
  --name "$ENV_PARAMETER" \
  --with-decryption \
  --query Parameter.Value \
  --output text \
  --region "$REGION" >"$temporary_env"
printf 'IMAGE_URI=%s\nIMAGE_TAG=%s\n' "$IMAGE_URI" "$IMAGE_TAG" >>"$temporary_env"
chmod 600 "$temporary_env"
mv "$temporary_env" "$APP_DIR/.env"

aws ecr get-login-password --region "$REGION" |
  docker login --username AWS --password-stdin "$REGISTRY"

compose=(docker compose --env-file "$APP_DIR/.env" -f "$APP_DIR/compose.prod.yml")
"${compose[@]}" pull
"${compose[@]}" up -d --remove-orphans

for ((attempt = 1; attempt <= HEALTH_ATTEMPTS; attempt++)); do
  printf 'Health check attempt %s/%s\n' "$attempt" "$HEALTH_ATTEMPTS"
  if curl -fsS http://localhost/actuator/health >/dev/null; then
    printf 'Health check succeeded\n'
    exit 0
  fi

  if ((attempt < HEALTH_ATTEMPTS)); then
    sleep "$HEALTH_DELAY_SECONDS"
  fi
done

printf 'Health check failed after %s attempts\n' "$HEALTH_ATTEMPTS" >&2
exit 1
