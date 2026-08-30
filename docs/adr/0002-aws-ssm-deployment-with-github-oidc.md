# ADR 0002: Passwordless AWS EC2 Deployment via Systems Manager and OpenID Connect

## Status
Accepted

## Context
Deploying production Docker containers to AWS EC2 via SSH requires managing long-lived SSH private keys or AWS Access Keys inside GitHub repository secrets, creating a significant security vulnerability if keys leak.

## Decision
We implemented a **passwordless CI/CD deployment pipeline** using **AWS OpenID Connect (OIDC)** and **AWS Systems Manager (SSM)**:

1. **GitHub OIDC Role Assumption:** GitHub Actions assumes short-lived IAM roles via AWS OIDC without static AWS access key credentials.
2. **Container Registry Publishing:** Images are built using Docker buildx and published to Amazon ECR and Docker Hub.
3. **AWS SSM Command Execution:** GitHub Actions triggers deployment scripts on the EC2 instance via `aws ssm send-command` over HTTPS without exposing SSH ports (Port 22 closed).
4. **Polling Verification:** A shell script polls SSM execution status until deployment succeeds or times out.

## Consequences

### Positive
- Zero long-lived AWS credentials stored in GitHub repository secrets.
- EC2 instance does not require an open inbound SSH port (22), reducing attack surface.
- Full audit log of deployment commands recorded in AWS CloudTrail and SSM.

### Negative
- Requires initial AWS IAM role and SSM Agent configuration on the EC2 host.
