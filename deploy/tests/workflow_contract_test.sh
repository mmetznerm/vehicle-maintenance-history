#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="${WORKFLOW_CONTRACT_ROOT:-$(cd "$script_dir/../.." && pwd)}"

# testlib is resolved from the test script's runtime directory.
# shellcheck disable=SC1091
source "$script_dir/testlib.sh"

assert_count() {
  local haystack="$1"
  local needle="$2"
  local expected="$3"
  local actual
  actual="$( (grep -F -o -- "$needle" <<<"$haystack" || true) | wc -l | tr -d ' ')"
  [[ "$actual" == "$expected" ]] || fail "expected $expected occurrence(s) of: $needle; got $actual"
}

extract_job() {
  local workflow_path="$1"
  local job_name="$2"

  awk -v job_name="$job_name" '
    $0 == "  " job_name ":" { printing = 1 }
    printing && $0 != "  " job_name ":" && /^  [[:alnum:]_-]+:$/ { exit }
    printing { print }
  ' "$workflow_path"
}

extract_step() {
  local job_block="$1"
  local step_name="$2"

  awk -v marker="      - name: ""$step_name" '
    $0 == marker { printing = 1 }
    printing && $0 != marker && /^      - name: / { exit }
    printing { print }
  ' <<<"$job_block"
}

require_job() {
  local workflow_path="$1"
  local job_name="$2"
  local job_block
  job_block="$(extract_job "$workflow_path" "$job_name")"
  [[ -n "$job_block" ]] || fail "expected job: $job_name"
  printf '%s\n' "$job_block"
}

require_step() {
  local job_block="$1"
  local step_name="$2"
  local step_block
  step_block="$(extract_step "$job_block" "$step_name")"
  [[ -n "$step_block" ]] || fail "expected step: $step_name"
  printf '%s\n' "$step_block"
}

ci_path="$repo_root/.github/workflows/ci-cd.yml"
codeql_path="$repo_root/.github/workflows/codeql.yml"
ci_workflow="$(<"$ci_path")"
codeql_workflow="$(<"$codeql_path")"
all_workflows="$ci_workflow"
all_workflows+=$'\n'
all_workflows+="$codeql_workflow"

assert_count "$all_workflows" "actions/checkout@v7" 5
assert_count "$all_workflows" "uses: actions/checkout@" 5
assert_not_contains "$all_workflows" "actions/checkout@v4"
assert_count "$all_workflows" "actions/setup-java@v5" 3
assert_count "$all_workflows" "uses: actions/setup-java@" 3
assert_not_contains "$all_workflows" "actions/setup-java@v4"
assert_count "$ci_workflow" "actions/setup-node@v7" 1
assert_count "$ci_workflow" "uses: actions/setup-node@" 1
assert_not_contains "$ci_workflow" "actions/setup-node@v4"
assert_contains "$codeql_workflow" "github/codeql-action/init@v4"
assert_contains "$codeql_workflow" "github/codeql-action/analyze@v4"
assert_contains "$codeql_workflow" "language: java-kotlin"
assert_contains "$codeql_workflow" "language: javascript-typescript"
assert_contains "$codeql_workflow" "security-events: write"
assert_not_contains "$all_workflows" "AWS_ACCESS_KEY_ID"
assert_not_contains "$all_workflows" "AWS_SECRET_ACCESS_KEY"

publish_job="$(require_job "$ci_path" "publish-docker-image")"
deploy_job="$(require_job "$ci_path" "deploy-to-ec2")"

assert_contains "$publish_job" "name: Publish Docker Image to Docker Hub and ECR"
assert_contains "$publish_job" "permissions:"
assert_contains "$publish_job" "contents: read"
assert_contains "$publish_job" "id-token: write"
assert_contains "$publish_job" "outputs:"
assert_contains "$publish_job" "ecr_registry: \${{ steps.login-ecr.outputs.registry }}"

publish_aws_credentials="$(require_step "$publish_job" "Configure AWS credentials")"
assert_contains "$publish_aws_credentials" "uses: aws-actions/configure-aws-credentials@v6"
assert_contains "$publish_aws_credentials" "role-to-assume: \${{ vars.AWS_ROLE_ARN }}"
assert_contains "$publish_aws_credentials" "aws-region: \${{ vars.AWS_REGION }}"

publish_ecr_login="$(require_step "$publish_job" "Log in to Amazon ECR")"
assert_contains "$publish_ecr_login" "id: login-ecr"
assert_contains "$publish_ecr_login" "uses: aws-actions/amazon-ecr-login@v2"

metadata_step="$(require_step "$publish_job" "Generate Docker metadata")"
assert_contains "$metadata_step" "mmetznerm/vehicle-maintenance-history"
# GitHub expressions are asserted literally, not expanded by this shell test.
# shellcheck disable=SC2016
assert_contains "$metadata_step" '${{ steps.login-ecr.outputs.registry }}/${{ vars.ECR_REPOSITORY }}'
assert_contains "$metadata_step" "type=sha,prefix=sha-,format=short"

build_push_step="$(require_step "$publish_job" "Build and push Docker image")"
assert_contains "$build_push_step" "uses: docker/build-push-action@v7"
assert_contains "$build_push_step" "tags: \${{ steps.metadata.outputs.tags }}"
assert_count "$publish_job" "uses: docker/build-push-action@v7" 1
assert_count "$ci_workflow" "uses: docker/build-push-action@v7" 1

assert_contains "$deploy_job" "needs: publish-docker-image"
assert_contains "$deploy_job" "if: github.event_name == 'push' && github.ref == 'refs/heads/main' && vars.AWS_DEPLOY_ENABLED == 'true'"
assert_contains "$deploy_job" "permissions:"
assert_contains "$deploy_job" "contents: read"
assert_contains "$deploy_job" "id-token: write"
assert_not_contains "$deploy_job" "environment:"

deploy_aws_credentials="$(require_step "$deploy_job" "Configure AWS credentials")"
assert_contains "$deploy_aws_credentials" "uses: aws-actions/configure-aws-credentials@v6"
assert_contains "$deploy_aws_credentials" "role-to-assume: \${{ vars.AWS_ROLE_ARN }}"
assert_contains "$deploy_aws_credentials" "aws-region: \${{ vars.AWS_REGION }}"

image_step="$(require_step "$deploy_job" "Set image reference")"
assert_contains "$image_step" "id: image"
# GitHub expressions are asserted literally, not expanded by this shell test.
# shellcheck disable=SC2016
assert_contains "$image_step" 'image_uri=${{ needs.publish-docker-image.outputs.ecr_registry }}/${{ vars.ECR_REPOSITORY }}'
# GitHub expressions are asserted literally, not expanded by this shell test.
# shellcheck disable=SC2016
assert_contains "$image_step" 'image_tag=sha-${GITHUB_SHA::7}'

send_command_step="$(require_step "$deploy_job" "Send deployment command")"
assert_contains "$send_command_step" "id: send-command"
assert_contains "$send_command_step" "AWS-RunShellScript"
# GitHub expressions and shell snippets are asserted literally, not expanded here.
# shellcheck disable=SC2016
assert_contains "$send_command_step" '${{ vars.EC2_INSTANCE_ID }}'
# GitHub expressions and shell snippets are asserted literally, not expanded here.
# shellcheck disable=SC2016
assert_contains "$send_command_step" 'jq -cn --arg command "$command"'
assert_contains "$send_command_step" "aws ssm send-command"

wait_step="$(require_step "$deploy_job" "Wait for deployment command")"
assert_contains "$wait_step" "id: wait-for-command"
assert_contains "$wait_step" "continue-on-error: true"
assert_contains "$wait_step" "wait_exit_code=\$?"
# GitHub expressions and shell snippets are asserted literally, not expanded here.
# shellcheck disable=SC2016
assert_contains "$wait_step" 'echo "exit_code=$wait_exit_code" >> "$GITHUB_OUTPUT"'

diagnostic_step="$(require_step "$deploy_job" "Show deployment command invocation")"
assert_contains "$diagnostic_step" "if: always()"
assert_contains "$diagnostic_step" "aws ssm get-command-invocation"
# GitHub expressions are asserted literally, not expanded by this shell test.
# shellcheck disable=SC2016
assert_contains "$diagnostic_step" '${{ steps.send-command.outputs.command_id }}'

failure_step="$(require_step "$deploy_job" "Fail when deployment command did not complete")"
assert_contains "$failure_step" "if: always()"
# GitHub expressions are asserted literally, not expanded by this shell test.
# shellcheck disable=SC2016
assert_contains "$failure_step" '${{ steps.wait-for-command.outputs.exit_code }}'
assert_contains "$failure_step" "[[ '\${{ steps.wait-for-command.outputs.exit_code }}' == '0' ]]"

printf 'PASS: workflow contract\n'
