#!/usr/bin/env bash
set -euo pipefail

[[ "$#" -eq 2 ]] || { printf 'Usage: %s COMMAND_ID INSTANCE_ID\n' "$0" >&2; exit 2; }
COMMAND_ID="$1"
INSTANCE_ID="$2"
REGION="${AWS_REGION:-us-east-2}"
POLL_ATTEMPTS="${VMH_SSM_POLL_ATTEMPTS:-130}"
POLL_DELAY_SECONDS="${VMH_SSM_POLL_DELAY_SECONDS:-10}"
EXECUTION_TIMEOUT_SECONDS="${VMH_SSM_EXECUTION_TIMEOUT_SECONDS:-1200}"

[[ "$COMMAND_ID" =~ ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$ ]] || { printf 'Invalid SSM command ID\n' >&2; exit 2; }
[[ "$INSTANCE_ID" =~ ^i-[0-9a-f]{8,17}$ ]] || { printf 'Invalid EC2 instance ID\n' >&2; exit 2; }
[[ "$POLL_ATTEMPTS" =~ ^[1-9][0-9]*$ && "$POLL_DELAY_SECONDS" =~ ^[0-9]+$ && "$EXECUTION_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]] || { printf 'Invalid SSM polling configuration\n' >&2; exit 2; }
(( (POLL_ATTEMPTS - 1) * POLL_DELAY_SECONDS >= EXECUTION_TIMEOUT_SECONDS )) || {
  printf 'SSM polling budget of %s seconds must cover remote execution timeout of %s seconds\n' \
    "$(( (POLL_ATTEMPTS - 1) * POLL_DELAY_SECONDS ))" "$EXECUTION_TIMEOUT_SECONDS" >&2
  exit 2
}

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
