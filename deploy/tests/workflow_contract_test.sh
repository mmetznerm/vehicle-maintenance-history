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
  local workflow="$1"
  local job_name="$2"

  awk -v job_name="$job_name" '
    $0 == "  " job_name ":" { printing = 1 }
    printing && $0 != "  " job_name ":" && /^  [[:alnum:]_-]+:$/ { exit }
    printing { print }
  ' <<<"$workflow"
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
  local workflow="$1"
  local job_name="$2"
  local job_block
  job_block="$(extract_job "$workflow" "$job_name")"
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
ci_workflow="$(grep -Ev '^[[:space:]]*#' "$ci_path")"
codeql_workflow="$(grep -Ev '^[[:space:]]*#' "$codeql_path")"
all_workflows="$ci_workflow"
all_workflows+=$'\n'
all_workflows+="$codeql_workflow"

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
assert_contains "$ci_workflow" "group: ci-cd-\${{ github.ref }}"
assert_contains "$ci_workflow" "cancel-in-progress: \${{ github.event_name == 'pull_request' }}"

publish_job="$(require_job "$ci_workflow" "publish-docker-image")"
deploy_job="$(require_job "$ci_workflow" "deploy-to-ec2")"
deployment_contracts_job="$(require_job "$ci_workflow" "deployment-contracts")"

assert_count "$all_workflows" "actions/checkout@v7" 7
assert_count "$all_workflows" "uses: actions/checkout@" 7

assert_contains "$deployment_contracts_job" "name: Deployment Contracts"
assert_contains "$deployment_contracts_job" "runs-on: ubuntu-latest"

deployment_contracts_checkout_step="$(require_step "$deployment_contracts_job" "Checkout repository")"
assert_contains "$deployment_contracts_checkout_step" "uses: actions/checkout@v7"

deployment_contract_tests_step="$(require_step "$deployment_contracts_job" "Run deployment contract tests")"
assert_contains "$deployment_contract_tests_step" "bash deploy/tests/compose_prod_test.sh"
assert_contains "$deployment_contract_tests_step" "bash deploy/tests/deploy_test.sh"
assert_contains "$deployment_contract_tests_step" "bash deploy/tests/setup_ec2_test.sh"
assert_contains "$deployment_contract_tests_step" "bash deploy/tests/ssm_wait_test.sh"
assert_contains "$deployment_contract_tests_step" "bash deploy/tests/workflow_contract_test.sh"

shellcheck_step="$(require_step "$deployment_contracts_job" "Run ShellCheck")"
assert_contains "$shellcheck_step" 'docker run --rm -v "$PWD:/mnt" koalaman/shellcheck:stable'
assert_contains "$shellcheck_step" "/mnt/deploy/deploy.sh"
assert_contains "$shellcheck_step" "/mnt/deploy/setup-ec2.sh"
assert_contains "$shellcheck_step" "/mnt/deploy/wait-for-ssm-command.sh"
assert_contains "$shellcheck_step" "/mnt/deploy/tests/testlib.sh"
assert_contains "$shellcheck_step" "/mnt/deploy/tests/compose_prod_test.sh"
assert_contains "$shellcheck_step" "/mnt/deploy/tests/deploy_test.sh"
assert_contains "$shellcheck_step" "/mnt/deploy/tests/setup_ec2_test.sh"
assert_contains "$shellcheck_step" "/mnt/deploy/tests/ssm_wait_test.sh"
assert_contains "$shellcheck_step" "/mnt/deploy/tests/workflow_contract_test.sh"

actionlint_step="$(require_step "$deployment_contracts_job" "Run actionlint")"
assert_contains "$actionlint_step" 'docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest'

compose_render_step="$(require_step "$deployment_contracts_job" "Render production Compose")"
assert_contains "$compose_render_step" "IMAGE_URI: registry.example.invalid/mmetznerm/vehicle-maintenance-history"
assert_contains "$compose_render_step" "IMAGE_TAG: sha-0123abc"
assert_contains "$compose_render_step" "SPRING_DATASOURCE_URL: jdbc:postgresql://db.internal:5432/vehicle_maintenance_history?sslmode=require"
assert_contains "$compose_render_step" "SPRING_DATASOURCE_USERNAME: vmh_app"
assert_contains "$compose_render_step" "SPRING_DATASOURCE_PASSWORD: 0123456789abcdef0123456789abcdef"
assert_contains "$compose_render_step" "JWT_SECRET: abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
assert_contains "$compose_render_step" "JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=70.0"
assert_contains "$compose_render_step" "docker compose -f deploy/compose.prod.yml config --quiet"

assert_contains "$publish_job" "name: Publish Docker Image to Docker Hub and ECR"
assert_contains "$publish_job" "permissions:"
assert_contains "$publish_job" "contents: read"
assert_contains "$publish_job" "id-token: write"
assert_contains "$publish_job" "outputs:"
assert_contains "$publish_job" "ecr_registry: \${{ steps.login-ecr.outputs.registry }}"
assert_contains "$publish_job" "deployment-contracts"
assert_contains "$publish_job" "if: github.event_name == 'push' && github.ref == 'refs/heads/main'"

publish_aws_credentials="$(require_step "$publish_job" "Configure AWS credentials")"
assert_contains "$publish_aws_credentials" "uses: aws-actions/configure-aws-credentials@v6"
assert_contains "$publish_aws_credentials" "role-to-assume: \${{ vars.AWS_PUBLISH_ROLE_ARN }}"
assert_contains "$publish_aws_credentials" "aws-region: \${{ vars.AWS_REGION }}"
assert_not_contains "$publish_job" "AWS_DEPLOY_ROLE_ARN"

publish_ecr_login="$(require_step "$publish_job" "Log in to Amazon ECR")"
assert_contains "$publish_ecr_login" "id: login-ecr"
assert_contains "$publish_ecr_login" "uses: aws-actions/amazon-ecr-login@v2"

metadata_step="$(require_step "$publish_job" "Generate Docker metadata")"
assert_contains "$metadata_step" "mmetznerm/vehicle-maintenance-history"
# GitHub expressions are asserted literally, not expanded by this shell test.
# shellcheck disable=SC2016
assert_contains "$metadata_step" '${{ steps.login-ecr.outputs.registry }}/${{ vars.ECR_REPOSITORY }}'
assert_contains "$metadata_step" "type=raw,value=latest"
assert_contains "$metadata_step" "type=sha,prefix=sha-,format=short"

build_push_step="$(require_step "$publish_job" "Build and push Docker image")"
assert_contains "$build_push_step" "uses: docker/build-push-action@v7"
assert_contains "$build_push_step" "tags: \${{ steps.metadata.outputs.tags }}"
assert_contains "$build_push_step" "platforms: linux/amd64"
assert_contains "$build_push_step" "push: true"
assert_count "$publish_job" "uses: docker/build-push-action@v7" 1
assert_count "$ci_workflow" "uses: docker/build-push-action@v7" 1

assert_contains "$deploy_job" "needs: publish-docker-image"
assert_contains "$deploy_job" "if: github.event_name == 'push' && github.ref == 'refs/heads/main' && vars.AWS_DEPLOY_ENABLED == 'true'"
assert_contains "$deploy_job" "permissions:"
assert_contains "$deploy_job" "contents: read"
assert_contains "$deploy_job" "id-token: write"
assert_not_contains "$deploy_job" "environment:"
assert_contains "$deploy_job" "concurrency:"
assert_contains "$deploy_job" "group: vehicle-maintenance-history-production"
assert_contains "$deploy_job" "cancel-in-progress: false"

deploy_checkout_step="$(require_step "$deploy_job" "Checkout repository")"
assert_contains "$deploy_checkout_step" "uses: actions/checkout@v7"

deploy_aws_credentials="$(require_step "$deploy_job" "Configure AWS credentials")"
assert_contains "$deploy_aws_credentials" "uses: aws-actions/configure-aws-credentials@v6"
assert_contains "$deploy_aws_credentials" "role-to-assume: \${{ vars.AWS_DEPLOY_ROLE_ARN }}"
assert_contains "$deploy_aws_credentials" "aws-region: \${{ vars.AWS_REGION }}"
assert_not_contains "$deploy_job" "AWS_PUBLISH_ROLE_ARN"
assert_not_contains "$ci_workflow" "AWS_ROLE_ARN"

image_step="$(require_step "$deploy_job" "Set image reference")"
assert_contains "$image_step" "id: image"
assert_contains "$image_step" "env:"
assert_contains "$image_step" "ECR_REGISTRY: \${{ needs.publish-docker-image.outputs.ecr_registry }}"
assert_contains "$image_step" "ECR_REPOSITORY: \${{ vars.ECR_REPOSITORY }}"
assert_contains "$image_step" "AWS_REGION: \${{ vars.AWS_REGION }}"
# shellcheck disable=SC2016
assert_contains "$image_step" '[[ "$AWS_REGION" == us-east-2 ]]'
# shellcheck disable=SC2016
assert_contains "$image_step" '[[ "$ECR_REPOSITORY" == mmetznerm/vehicle-maintenance-history ]]'
# shellcheck disable=SC2016
assert_contains "$image_step" 'echo "image_uri=$ECR_REGISTRY/$ECR_REPOSITORY" >> "$GITHUB_OUTPUT"'
# shellcheck disable=SC2016
assert_contains "$image_step" 'echo "image_tag=sha-${GITHUB_SHA::7}" >> "$GITHUB_OUTPUT"'

send_command_step="$(require_step "$deploy_job" "Send deployment command")"
assert_contains "$send_command_step" "id: send-command"
assert_contains "$send_command_step" "AWS-RunShellScript"
assert_contains "$send_command_step" "env:"
assert_contains "$send_command_step" "AWS_REGION: \${{ vars.AWS_REGION }}"
assert_contains "$send_command_step" "EC2_INSTANCE_ID: \${{ vars.EC2_INSTANCE_ID }}"
assert_contains "$send_command_step" "IMAGE_URI: \${{ steps.image.outputs.image_uri }}"
assert_contains "$send_command_step" "IMAGE_TAG: \${{ steps.image.outputs.image_tag }}"
# shellcheck disable=SC2016
assert_contains "$send_command_step" '[[ "$EC2_INSTANCE_ID" =~ ^i-[0-9a-f]{8,17}$ ]]'
# shellcheck disable=SC2016
assert_contains "$send_command_step" 'printf -v command '\''%s %s %s'\'' /opt/vehicle-maintenance-history/deploy.sh "$IMAGE_URI" "$IMAGE_TAG"'
# shellcheck disable=SC2016
assert_contains "$send_command_step" 'jq -cn --arg command "$command"'
assert_contains "$send_command_step" "aws ssm send-command"

poll_step="$(require_step "$deploy_job" "Poll deployment command")"
assert_contains "$poll_step" "id: poll-command"
assert_contains "$poll_step" "continue-on-error: true"
assert_contains "$poll_step" "COMMAND_ID: \${{ steps.send-command.outputs.command_id }}"
assert_contains "$poll_step" "EC2_INSTANCE_ID: \${{ vars.EC2_INSTANCE_ID }}"
# shellcheck disable=SC2016
assert_contains "$poll_step" 'bash deploy/wait-for-ssm-command.sh "$COMMAND_ID" "$EC2_INSTANCE_ID"'
assert_contains "$poll_step" "poll_exit_code=\$?"
# shellcheck disable=SC2016
assert_contains "$poll_step" 'echo "exit_code=$poll_exit_code" >> "$GITHUB_OUTPUT"'
assert_not_contains "$ci_workflow" "aws ssm wait"

checkout_line="$(grep -n -m 1 -- 'name: Checkout repository' <<<"$deploy_job" | cut -d: -f1)"
poll_line="$(grep -n -m 1 -- 'name: Poll deployment command' <<<"$deploy_job" | cut -d: -f1)"
((checkout_line < poll_line)) || fail 'deploy checkout must precede polling'

diagnostic_step="$(require_step "$deploy_job" "Show deployment command invocation")"
assert_contains "$diagnostic_step" "if: always()"
assert_contains "$diagnostic_step" "env:"
assert_contains "$diagnostic_step" "AWS_REGION: \${{ vars.AWS_REGION }}"
assert_contains "$diagnostic_step" "COMMAND_ID: \${{ steps.send-command.outputs.command_id }}"
assert_contains "$diagnostic_step" "EC2_INSTANCE_ID: \${{ vars.EC2_INSTANCE_ID }}"
assert_contains "$diagnostic_step" "aws ssm get-command-invocation"

failure_step="$(require_step "$deploy_job" "Fail when deployment command did not complete")"
assert_contains "$failure_step" "if: always()"
assert_contains "$failure_step" "env:"
assert_contains "$failure_step" "POLL_EXIT_CODE: \${{ steps.poll-command.outputs.exit_code }}"
# shellcheck disable=SC2016
assert_contains "$failure_step" '[[ "$POLL_EXIT_CODE" == 0 ]]'

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

printf 'PASS: workflow contract\n'
