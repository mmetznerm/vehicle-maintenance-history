#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"

# shellcheck source=testlib.sh
source "$script_dir/testlib.sh"

assert_count() {
  local haystack="$1"
  local needle="$2"
  local expected="$3"
  local actual
  actual="$( (grep -F -o -- "$needle" <<<"$haystack" || true) | wc -l | tr -d ' ')"
  [[ "$actual" == "$expected" ]] || fail "expected $expected occurrence(s) of: $needle; got $actual"
}

ci_workflow="$(<"$repo_root/.github/workflows/ci-cd.yml")"
codeql_workflow="$(<"$repo_root/.github/workflows/codeql.yml")"
all_workflows="$ci_workflow"
all_workflows+=$'\n'
all_workflows+="$codeql_workflow"

assert_contains "$all_workflows" "actions/checkout@v7"
assert_count "$all_workflows" "actions/checkout@v7" 5
assert_not_contains "$all_workflows" "actions/checkout@v4"
assert_contains "$all_workflows" "actions/setup-java@v5"
assert_count "$all_workflows" "actions/setup-java@v5" 3
assert_not_contains "$all_workflows" "actions/setup-java@v4"
assert_contains "$ci_workflow" "actions/setup-node@v7"
assert_count "$ci_workflow" "actions/setup-node@v7" 1
assert_not_contains "$ci_workflow" "actions/setup-node@v4"
assert_contains "$codeql_workflow" "github/codeql-action/init@v4"
assert_contains "$codeql_workflow" "github/codeql-action/analyze@v4"
assert_contains "$codeql_workflow" "language: java-kotlin"
assert_contains "$codeql_workflow" "language: javascript-typescript"
assert_contains "$codeql_workflow" "security-events: write"

assert_contains "$ci_workflow" "Publish Docker Image to Docker Hub and ECR"
assert_contains "$ci_workflow" "aws-actions/configure-aws-credentials@v6"
assert_contains "$ci_workflow" "aws-actions/amazon-ecr-login@v2"
assert_contains "$ci_workflow" "id-token: write"
assert_count "$ci_workflow" "id-token: write" 2
assert_count "$ci_workflow" "aws-actions/configure-aws-credentials@v6" 2
assert_contains "$ci_workflow" "mmetznerm/vehicle-maintenance-history"
assert_contains "$ci_workflow" '${{ steps.login-ecr.outputs.registry }}/${{ vars.ECR_REPOSITORY }}'
assert_contains "$ci_workflow" "ecr_registry: \${{ steps.login-ecr.outputs.registry }}"
assert_contains "$ci_workflow" "uses: docker/build-push-action@v7"
assert_count "$ci_workflow" "uses: docker/build-push-action@v7" 1
assert_contains "$ci_workflow" "deploy-to-ec2:"
assert_contains "$ci_workflow" "vars.AWS_DEPLOY_ENABLED == 'true'"
assert_contains "$ci_workflow" "AWS-RunShellScript"
assert_contains "$ci_workflow" '${{ vars.EC2_INSTANCE_ID }}'
assert_contains "$ci_workflow" '${{ vars.ECR_REPOSITORY }}'
assert_contains "$ci_workflow" 'image_tag=sha-${GITHUB_SHA::7}'
assert_contains "$ci_workflow" 'jq -cn --arg command "$command"'
assert_contains "$ci_workflow" "aws ssm get-command-invocation"
assert_contains "$ci_workflow" "continue-on-error: true"
assert_not_contains "$ci_workflow" "environment:"
assert_not_contains "$all_workflows" "AWS_ACCESS_KEY_ID"
assert_not_contains "$all_workflows" "AWS_SECRET_ACCESS_KEY"

printf 'PASS: workflow contract\n'
