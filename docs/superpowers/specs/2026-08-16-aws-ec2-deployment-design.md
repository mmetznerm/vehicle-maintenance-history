# AWS EC2 Deployment Design

## Goal

Extend the existing CI/CD pipeline so that a successful merge to `main`:

1. passes the existing backend, frontend, coverage, integration, and security checks;
2. builds the application image once;
3. publishes the same commit-scoped image to Docker Hub and Amazon ECR; and
4. deploys that exact image to one Amazon EC2 instance through AWS Systems Manager.

The deployed application uses the existing private Amazon RDS for PostgreSQL
instance. The result is intentionally small enough for a portfolio while still
demonstrating production-oriented identity, image provenance, network isolation,
database migrations, and automated delivery.

## Context

The project already provides:

- Java 21 and Spring Boot 3.5;
- a React frontend embedded in the Spring Boot artifact;
- Flyway migrations for the application schema;
- unit, controller, integration, frontend, coverage, and CodeQL checks;
- a multi-stage production Dockerfile that runs as a non-root user;
- an Actuator health endpoint;
- Docker Hub delivery with `latest` and `sha-<short-commit>` tags.

The following AWS resources already exist in `us-east-2`:

- RDS instance `vehicle-maintenance-history-db`, PostgreSQL 16.14,
  `db.t4g.micro`, 20 GiB gp3, encrypted, Single-AZ, and private;
- RDS master user `vmh_admin`;
- private ECR repository `mmetznerm/vehicle-maintenance-history`.

The RDS instance has no initial database name. The deployment setup therefore
creates an application database and user before the first application start.

## Scope Decisions

### Necessary stages

- GitHub stores the source and receives pull requests.
- GitHub Actions performs CI, publishes images, and initiates deployment.
- Docker Hub remains a public portfolio artifact.
- Amazon ECR is the private operational registry used by EC2.
- Amazon EC2 hosts the single application container.
- Amazon RDS hosts persistent PostgreSQL data.

No separate GitHub webhook is required. Native `pull_request` and `push` events
already trigger GitHub Actions.

### Deliberately excluded

- Terraform or CloudFormation;
- ECS, EKS, Kubernetes, or multiple EC2 instances;
- Application Load Balancer and Auto Scaling;
- Route 53, a custom domain, and HTTPS;
- RDS Multi-AZ, Aurora, and RDS Proxy;
- SSH access;
- automatic rollback;
- long-lived AWS access keys in GitHub;
- GitHub Environments and deployment approvals.

These exclusions keep the system understandable and affordable. The first
public endpoint is an HTTP demonstration endpoint and must only use test data
and test credentials. HTTPS is the first recommended follow-up improvement.

## Architecture

```text
Developer
    |
    v
GitHub main
    |
    v
GitHub Actions
    |-- CI and CodeQL
    |-- one linux/amd64 image build
    |-- Docker Hub: latest + sha-<commit>
    |-- Amazon ECR: latest + sha-<commit>
    `-- OIDC -> AWS IAM role -> SSM Run Command
                                      |
                                      v
                           EC2 t3.micro, x86_64
                           Amazon Linux 2023
                           Docker, public port 80
                                      |
                                      v
                           private PostgreSQL 5432
                           Amazon RDS PostgreSQL 16
```

The frontend and backend continue to share one origin because the production
image embeds the frontend in Spring Boot. The frontend therefore needs no
separate hosting or production API URL.

## AWS Resources

### EC2 instance

- Amazon Linux 2023, x86_64;
- `t3.micro` to match the published `linux/amd64` image;
- 10 GiB gp3 root volume;
- auto-assigned public IPv4 address;
- the same VPC as RDS;
- a public subnet with outbound internet access;
- no inbound SSH rule;
- one GiB swap file created idempotently during setup;
- application files under `/opt/vehicle-maintenance-history`.

An auto-assigned address can change after an instance stop/start. This is
accepted for the initial portfolio deployment. The SSM deployment targets the
instance ID and is not affected by a public IP change.

### Security groups

`vehicle-maintenance-history-app-sg`:

- inbound TCP 80 from `0.0.0.0/0`;
- normal outbound access for ECR, SSM, Docker Hub, and RDS.

`vehicle-maintenance-history-db-sg`:

- inbound TCP 5432 only from `vehicle-maintenance-history-app-sg`;
- no public database access.

### GitHub OIDC role

Role name: `github-actions-vehicle-maintenance-history`.

The trust policy accepts GitHub's OIDC provider only when:

- audience is `sts.amazonaws.com`; and
- subject is
  `repo:mmetznerm/vehicle-maintenance-history:ref:refs/heads/main`.

The role can:

- obtain an ECR authorization token;
- upload layers and images only to the application ECR repository;
- send `AWS-RunShellScript` only to the selected application EC2 instance; and
- read the resulting SSM command status and output.

No IAM user access key is stored in GitHub. The existing `GITHUB_ACTIONS` IAM
user can be disabled after OIDC has completed one successful deployment.

### EC2 instance role

Role name: `vehicle-maintenance-history-ec2-role`.

It uses:

- `AmazonSSMManagedInstanceCore`;
- `AmazonEC2ContainerRegistryPullOnly`; and
- a small inline policy allowing decryption and read access only to the two
  application parameters.

### Parameter Store

Two SecureString parameters minimize console setup:

- `/vmh/prod/app-env` contains the production environment file;
- `/vmh/prod/rds-master-password` contains the existing RDS master password
  and is deleted after successful database initialization.

The application environment contains:

- the JDBC URL using the RDS endpoint, port 5432, database
  `vehicle_maintenance_history`, and `sslmode=require`;
- application user `vmh_app` and its generated password;
- a generated JWT secret of at least 32 bytes;
- a JVM memory percentage suitable for the small EC2 instance.

Secrets are generated as hexadecimal strings so the SecureString can be written
directly as an environment file without shell quoting ambiguity.

### Cost guardrail

The RDS storage autoscaling maximum is reduced from 1000 GiB to 30 GiB before
deployment. This does not change the current 20 GiB allocation but prevents an
unexpectedly large irreversible storage expansion in a portfolio environment.

## Repository Changes

### `deploy/compose.prod.yml`

The production Compose file:

- contains only the application service;
- uses `${IMAGE_URI}:${IMAGE_TAG}` from the generated environment file;
- publishes host port 80 to container port 8080;
- passes Spring datasource and JWT variables to the container;
- uses `restart: unless-stopped`;
- preserves the existing Actuator health check;
- caps and rotates Docker JSON logs;
- applies a memory limit appropriate for `t3.micro`.

The root `docker-compose.yml` remains unchanged and continues to run the local
application and PostgreSQL development environment.

### `deploy/setup-ec2.sh`

This repeat-safe one-time script:

1. installs and enables Docker;
2. creates the swap file and application directory;
3. installs the production Compose file and deploy script;
4. retrieves the two SecureString parameters through the instance role;
5. connects to the default RDS `postgres` database using a temporary
   `postgres:16-alpine` client container;
6. creates login role `vmh_app` when absent and synchronizes its password with
   the protected application environment;
7. creates database `vehicle_maintenance_history` owned by `vmh_app` when
   absent; and
8. performs the first deployment.

The script never prints either password. It can be rerun safely while the master
password parameter exists. After setup succeeds, the operator deletes
`/vmh/prod/rds-master-password`; from that point onward only `deploy.sh` is used,
and the application never receives the master credential.

The first setup is deliberately manual and auditable. After the deployment
changes have reached `main`, the operator opens an EC2 Session Manager shell,
downloads the setup artifacts from the exact Git commit represented by the
first ECR SHA tag into `/opt/vehicle-maintenance-history`, and runs the setup
script with `sudo`. No SSH key or inbound port 22 is needed.

### `deploy/deploy.sh`

The repeatable deployment script:

1. accepts the full ECR image URI and commit-scoped `sha-<commit>` tag;
2. authenticates Docker to ECR with the EC2 instance role;
3. refreshes `/vmh/prod/app-env` into a root-readable environment file;
4. adds the non-secret image URI and tag;
5. pulls the image and runs `docker compose up -d`;
6. retries the local Actuator health endpoint for a bounded period; and
7. exits non-zero when authentication, pull, startup, or health validation
   fails.

Automatic rollback is not included. Every prior SHA tag remains deployable, so
the documented rollback procedure reruns this script with a previous tag.

### `.github/workflows/ci-cd.yml`

The existing quality jobs remain the deployment gate. The workflow also:

- updates GitHub-maintained actions that currently emit Node.js deprecation
  warnings;
- grants `id-token: write` only to jobs that authenticate to AWS;
- assumes the GitHub OIDC role in `us-east-2`;
- logs in to Docker Hub and ECR;
- uses one Buildx invocation with both registry names;
- publishes `latest` and `sha-<short-commit>` to both registries;
- starts deployment only after both registry pushes succeed;
- invokes `deploy.sh` through SSM with the exact SHA tag;
- waits for SSM completion and surfaces command output in the Actions log.

The deploy job is also gated by repository variable `AWS_DEPLOY_ENABLED`. It is
unset during infrastructure setup. The first merge can therefore publish the
ECR image without attempting to deploy to an EC2 instance that is not ready.
After the manual first deployment succeeds, the variable is set to `true` and
subsequent merges deploy automatically.

Repository variables:

- `AWS_REGION` with value `us-east-2`;
- `AWS_ROLE_ARN` with the OIDC role ARN;
- `ECR_REPOSITORY` with value
  `mmetznerm/vehicle-maintenance-history`;
- `EC2_INSTANCE_ID` with the application instance ID.
- `AWS_DEPLOY_ENABLED` with value `true` only after the first deployment.

Existing Docker Hub secrets remain:

- `DOCKER_USERNAME`;
- `DOCKER_ACCESS_TOKEN`.

### `.github/workflows/codeql.yml`

The GitHub-maintained checkout and Java setup actions are updated to their
Node.js 24-compatible major versions. CodeQL behavior, languages, schedule, and
permissions otherwise remain unchanged.

### Other documentation

`docs/aws-deployment.md` provides console-oriented, ordered instructions for:

- the RDS storage limit and security group;
- OIDC provider and both IAM roles;
- Parameter Store values;
- EC2 creation and first setup;
- GitHub repository variables;
- initial deployment, verification, logs, and manual rollback.

The README links to this guide and explains the public Docker Hub versus private
ECR responsibilities.

## Delivery Flow

### Pull requests

Pull requests run CI and CodeQL only. They do not authenticate to AWS, publish
images, or deploy infrastructure.

### Pushes to `main`

1. All backend and frontend gates pass.
2. GitHub Actions builds one `linux/amd64` image.
3. The image is pushed to Docker Hub and ECR with identical tags.
4. The deploy job assumes the OIDC role.
5. SSM executes `deploy.sh` on EC2 with the ECR URI and commit-scoped SHA tag.
6. The workflow succeeds only after the EC2 health check succeeds.

`latest` is convenient for browsing registries. Deployment always uses the SHA
tag so the running version is traceable to source.

## Failure Behavior

- A failed test prevents image publication.
- A failed push to either registry prevents deployment.
- An OIDC, SSM, ECR pull, container startup, or health failure makes the
  workflow fail visibly.
- SSM command output and container logs provide diagnostic evidence.
- A failed application deployment requires the documented manual rollback to a
  previously successful SHA tag.
- Flyway failure leaves the container unhealthy and fails deployment rather
  than silently running against a partially migrated schema.

## Verification

Local and static validation:

- backend unit, controller, coverage, and integration suites;
- frontend lint, coverage, and production build;
- `actionlint` for GitHub Actions;
- `docker compose config` for the production Compose file;
- `shellcheck` for both shell scripts;
- a local `linux/amd64` production image build;
- Git diff and whitespace checks.

AWS acceptance checks:

- Docker Hub and ECR contain the same `sha-<commit>` tag;
- GitHub Actions shows a successful SSM deployment;
- EC2 serves the frontend on its public HTTP address;
- `/actuator/health` reports `UP`;
- Flyway creates the application tables in
  `vehicle_maintenance_history`;
- registration and login work with test credentials;
- application data remains after a container restart;
- EC2 has no inbound port 22 rule;
- RDS has no public access and no public 5432 rule;
- GitHub contains no long-lived AWS access key.

## Documentation References

- [GitHub OIDC with AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws)
- [Amazon ECR private registry authentication](https://docs.aws.amazon.com/AmazonECR/latest/userguide/registry_auth.html)
- [AmazonEC2ContainerRegistryPullOnly](https://docs.aws.amazon.com/AmazonECR/latest/userguide/security-iam-awsmanpol.html)
- [Connecting to RDS for PostgreSQL](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_ConnectToPostgreSQLInstance.html)
