# AWS EC2 Deployment Hardening Design

**Date:** 2026-08-16
**Status:** Approved for specification review
**Extends:** `docs/superpowers/specs/2026-08-16-aws-ec2-deployment-design.md`

## Context

The first implementation of the AWS deployment design passes its local release gates, but final branch review identified operational gaps that matter once GitHub Actions can start remote work on EC2. A canceled runner does not cancel an already-sent SSM command, the default SSM waiter can time out before a normal rollout completes, and the initial design did not require ECR tag immutability or encrypted EBS storage.

This hardening keeps the portfolio architecture intentionally small—one EC2 instance and one private RDS instance—while making deployment ordering, provenance, credentials, storage, and recovery explicit. It does not provision AWS resources automatically.

## Selected Approach

Use a minimal production-safe hardening layer rather than retaining the original single-role and local-only contract model. The alternative of session policies on one broad role is rejected because a compromised job could request a fresh unrestricted OIDC session. Merely documenting the risks is also rejected because concurrent remote commands and false timeout failures affect correctness.

## Deployment Serialization

GitHub may continue canceling obsolete pull-request validation, but a workflow running for `main` must not be canceled after it can create remote side effects. Workflow-level `cancel-in-progress` will therefore be conditional on the event being a pull request.

The `deploy-to-ec2` job will have a repository-wide production concurrency group with `cancel-in-progress: false`. GitHub may replace an older pending deployment with a newer pending one, but it must never cancel the currently running production deployment.

GitHub serialization is not the host trust boundary. `deploy/deploy.sh` will also acquire an exclusive lock at `/var/lock/vehicle-maintenance-history-deploy.lock` before retrieving or replacing `.env`, logging in, or invoking Compose. It will wait for a bounded period and fail without changing the deployment if it cannot acquire the lock. This protects against manual runs, SSM commands whose originating workflow was canceled, and other callers outside GitHub concurrency.

## Reliable SSM Completion

The deploy workflow will replace the default `aws ssm wait command-executed` waiter with explicit bounded polling for up to 15 minutes. It will:

1. call `get-command-invocation` every 10 seconds;
2. treat `InvocationDoesNotExist` as eventual consistency and retry it;
3. continue for `Pending`, `InProgress`, and `Delayed`;
4. accept only `Success` as successful;
5. treat `Cancelled`, `TimedOut`, `Failed`, `Cancelling`, unknown states, and the overall deadline as failure;
6. always request the final command invocation so stdout/stderr is visible;
7. fail the job only after diagnostic retrieval.

Repository and instance values will enter Bash through step-level environment variables. The workflow will validate the ECR image URI, SHA tag, region, and EC2 instance ID before constructing the remote command. `jq` will continue producing the SSM JSON payload.

## Separate GitHub OIDC Roles

The single GitHub role will be replaced in the runbook and workflow by two roles with the same repository/`main` OIDC trust condition:

- `github-actions-vehicle-maintenance-history-publish`, exposed as `AWS_PUBLISH_ROLE_ARN`, with only ECR authorization and push permissions;
- `github-actions-vehicle-maintenance-history-deploy`, exposed as `AWS_DEPLOY_ROLE_ARN`, with only SSM SendCommand and command-read permissions.

The publish job cannot send commands to EC2, and the deploy job cannot push or replace images. Docker Hub credentials remain limited to the publish job. No long-lived AWS access keys are introduced.

## Image Provenance

The ECR repository `mmetznerm/vehicle-maintenance-history` in `us-east-2` will use `IMMUTABLE_WITH_EXCLUSION` with the single wildcard exclusion `latest`. Existing `sha-*` tags cannot be overwritten; `latest` remains a convenience pointer and is never used for deployment or rollback.

The runbook will include the exact console setting, CLI update command, and a read-only verification command. It will explain that rerunning a publish for a commit whose immutable ECR SHA tag already exists can fail by design; deployment and rollback always use the existing SHA tag. Recording an image digest is recommended for audit, but digest-based deployment is outside this hardening scope.

## Encrypted EC2 Storage

The EC2 launch instructions will require the 10 GiB gp3 root volume to have EBS encryption enabled. The acceptance checklist will verify encryption before application secrets are installed. This protects the mode-`600` `.env`, Docker layers, logs, swap contents, and snapshots at rest. The default account EBS encryption key is sufficient for this portfolio design; a dedicated customer-managed key remains optional.

## Recoverable Swap Setup

Host bootstrap must tolerate interruption during swap creation. If the target swap file is already active, setup leaves it unchanged. If it is inactive, setup validates that it is a usable swap file. A missing, partial, or invalid file is replaced using a same-directory temporary file that is allocated, permissioned, formatted, and atomically renamed before activation. `/etc/fstab` remains duplicate-safe.

Tests will cover an invalid pre-existing file, a valid inactive file, an already-active file, first creation, and a second idempotent run.

## Deployment Script Input Safety

`deploy/deploy.sh` will continue rejecting mutable tags. It will additionally accept only an Amazon ECR image URI whose region matches `AWS_REGION`; malformed or non-ECR registries fail before AWS or Docker calls. This prevents an ECR authorization token from being sent to an arbitrary registry during manual operation.

## Contracts in CI

A new `deployment-contracts` job will run for pull requests and `main` before image publication. It will execute:

- all four deployment contract suites;
- ShellCheck over every deployment script;
- actionlint over the repository workflows;
- a quiet render of the production Compose file with deterministic test values.

`publish-docker-image` will require this job in addition to the existing backend and frontend quality jobs.

Workflow validation will combine actionlint's parsed workflow validation with the existing job/step-scoped contract. The scoped contract will ignore YAML comments and add assertions for publish dependencies and condition, `push: true`, `linux/amd64`, both `latest` and SHA metadata, deployment concurrency, distinct role variables, polling states/deadline, and final diagnostic/failure linkage. Mutation cases will prove that moving or removing critical fields makes the test fail.

## IAM and Runbook Tightening

The EC2 role's `ssm:GetParameter` resources will list exactly:

- `arn:aws:ssm:us-east-2:675244612319:parameter/vmh/prod/app-env`
- `arn:aws:ssm:us-east-2:675244612319:parameter/vmh/prod/rds-master-password`

The existing KMS condition remains limited to decrypt operations invoked through SSM in `us-east-2`; the guide will clarify that the account's default SSM/EBS encryption keys are used unless the operator deliberately chooses a customer-managed key.

The runbook will also recommend IMDSv2-only metadata, ECR vulnerability scanning, and a lifecycle policy as optional production improvements. They are not prerequisites for the portfolio deployment.

## Failure Behavior

- A second deploy waits for the host lock and exits without mutation if the timeout expires.
- A canceled GitHub runner cannot cause a later deploy to overlap the still-running remote command.
- SSM eventual consistency does not create a false failure.
- SSM non-success terminal states and the 15-minute deadline fail the workflow after diagnostics.
- An existing immutable SHA tag cannot silently change provenance.
- Invalid ECR URIs fail before the login-password pipeline.
- Partial swap files are repaired without duplicate fstab entries.

## Verification

Implementation is complete only when:

1. focused RED/GREEN tests cover the deploy lock, ECR URI validation, SSM polling, role separation, workflow concurrency, CI contract job, and interrupted swap repair;
2. all deployment contracts, ShellCheck, actionlint, and Compose render pass;
3. backend unit and integration suites pass;
4. frontend lint, coverage, and build pass;
5. the `linux/amd64` application image builds;
6. documentation secret-hygiene checks pass;
7. a final branch review reports no Critical or Important findings.

## Out of Scope

This hardening does not add Terraform or CloudFormation, ECS/EKS, an ALB, autoscaling, HTTPS, Route 53, SSH, multi-instance deployment, automatic rollback, a second RDS instance, GitHub Environments, or live AWS mutations from the development session.
