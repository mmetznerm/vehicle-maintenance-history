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
services="$(docker compose -f "$REPO_ROOT/deploy/compose.prod.yml" config --services)"
[[ "$services" == "app" ]] || fail "expected exactly one service named app, got: $services"
assert_contains "$rendered" "registry.example.invalid/mmetznerm/vehicle-maintenance-history:sha-0123abc"
assert_contains "$rendered" "target: 8080"
assert_contains "$rendered" "published: \"80\""
assert_contains "$rendered" "restart: unless-stopped"
assert_contains "$rendered" "mem_limit: \"805306368\""
assert_contains "$rendered" "max-size: 10m"
assert_contains "$rendered" "max-file: \"3\""
assert_contains "$rendered" "- curl"
assert_contains "$rendered" "- -fsS"
assert_contains "$rendered" "- http://localhost:8080/actuator/health"
assert_not_contains "$rendered" "postgres:"
printf 'PASS: production Compose contract\n'
