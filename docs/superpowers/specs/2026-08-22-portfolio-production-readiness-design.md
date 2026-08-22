# Portfolio Production Readiness Design

**Date:** 2026-08-22

**Status:** Approved

**Extends:**
`docs/superpowers/specs/2026-08-16-aws-ec2-deployment-design.md` and
`docs/superpowers/specs/2026-08-16-aws-ec2-deployment-hardening-design.md`

## Context

Vehicle Maintenance History is already deployed as a small public portfolio
application. A merge to `main` runs the quality gates, publishes the same
commit image to Docker Hub and private Amazon ECR, and deploys the immutable
ECR SHA tag to one EC2 instance through Systems Manager. The application is
healthy and persists data in Amazon RDS for PostgreSQL.

The infrastructure bootstrap is complete, but the environment still needs an
operational closeout. The temporary RDS master credential must be removed, the
database must be confirmed private, cost limits and alerts must be verified,
the public behavior must be accepted end to end, and the repository must
present the running system clearly to portfolio reviewers.

This design closes that gap without replacing the working EC2 architecture or
adding paid infrastructure.

## Goals

1. Remove the temporary RDS master password from Parameter Store and remove
   the EC2 role's permission to read it.
2. Keep the application connected to RDS through private VPC networking while
   removing public database exposure.
3. Keep the RDS allocation at 20 GiB and cap storage autoscaling at 30 GiB.
4. Verify the production user journeys and persistence with fictitious data.
5. Record non-sensitive acceptance evidence for the deployed SHA image.
6. Improve the README with a live demo link, architecture, screenshots,
   delivery flow, security decisions, and honest limitations.
7. Define an always-on, Free Tier-conscious operating routine with an explicit
   response to detected or forecast charges.
8. Record HTTPS, observability, automatic rollback, Terraform, and a possible
   ECS migration as separately triggered future initiatives.

## Non-goals

This phase does not:

- create an Application Load Balancer, NAT Gateway, Route 53 hosted zone,
  custom domain, certificate, Aurora cluster, ECS service, or additional EC2
  instance;
- implement HTTPS, centralized application logging, automatic rollback,
  Terraform, CloudFormation, ECS, EKS, Kubernetes, Multi-AZ, RDS Proxy, Auto
  Scaling, or deployment approvals;
- add an automated resource shutdown system;
- promise that AWS usage will remain free after eligibility or credits expire;
- publish AWS credentials, application credentials, Parameter Store contents,
  access tokens, personal data, or reusable passwords;
- redesign the application's Java or React features.

## Constraints

- Region remains `us-east-2`.
- Runtime remains one Amazon Linux 2023 x86_64 `t3.micro` EC2 instance with an
  encrypted 10 GiB gp3 root volume and IMDSv2 only.
- Database remains PostgreSQL on a Single-AZ `db.t4g.micro` RDS instance.
- Runtime deployments continue to use immutable `sha-<commit>` ECR tags.
- Docker Hub remains the public portfolio image registry; EC2 pulls only from
  private ECR.
- GitHub Actions continues to authenticate to AWS with separate OIDC publish
  and deploy roles and no persistent AWS access keys.
- Operations use Systems Manager Session Manager, never SSH; inbound TCP 22
  remains absent.
- The public endpoint remains HTTP-only in this phase and may contain only
  fictitious demonstration data and test-only credentials.
- The application remains online while Free Tier eligibility or credits cover
  the measured usage and no charge is detected or forecast.
- Every destructive AWS action requires a fresh, explicit operator
  confirmation immediately before execution.

## Architecture

The deployed topology is unchanged:

```text
Developer
    |
    v
GitHub -> GitHub Actions -> Docker Hub
                   |
                   +----------> Amazon ECR
                                      |
                              SSM deploy command
                                      |
                                      v
                               Amazon EC2 :80
                                      |
                         security-group reference :5432
                                      |
                                      v
                        private Amazon RDS PostgreSQL
```

The public browser reaches only EC2 on TCP 80. EC2 reaches RDS on TCP 5432 by
referencing the application security group in the database security group.
The database has no public access. GitHub Actions reaches EC2 only through an
SSM command targeted to the existing instance ID.

## Workstream 1: Preflight and Evidence Boundary

Before any mutation, capture a non-sensitive baseline:

- current `main` full commit and deployed `sha-<commit>` tag;
- successful GitHub Actions run URL;
- internal Actuator health result;
- EC2 instance state, instance profile, VPC, subnet, security groups, encrypted
  root volume, and IMDSv2 configuration;
- RDS state, class, VPC, public accessibility, attached security groups,
  allocated storage, and autoscaling maximum;
- names and resources in the EC2 inline Parameter Store policy;
- existing AWS Budget and Free Tier alert configuration.

Evidence records outcomes and resource names, not decrypted values, session
credentials, account login details, or full console exports. Screenshots of
the AWS console are not portfolio artifacts because they can expose account
metadata and unrelated resources.

The preflight stops if the application is unhealthy, the deployed image cannot
be identified, the application is not using `vmh_app`, or a recovery path for
the current networking configuration has not been recorded.

## Workstream 2: Bootstrap Credential Closure

The one-time parameter `/vmh/prod/rds-master-password` exists only to create
the `vmh_app` role and `vehicle_maintenance_history` database during initial
EC2 bootstrap. It is not required by normal deployment or runtime.

Close the bootstrap path in this order:

1. Confirm the current application health and a successful authenticated
   database-backed request.
2. Confirm `/vmh/prod/app-env` names `vmh_app` as the datasource user without
   recording any value from the parameter.
3. Obtain explicit operator confirmation for deletion.
4. Delete `/vmh/prod/rds-master-password` through the AWS console.
5. Remove only the master-parameter ARN from the EC2 inline policy. Preserve
   `ssm:GetParameter` for `/vmh/prod/app-env` and the existing KMS restriction
   through SSM in `us-east-2`.
6. Verify the parameter is absent, the role policy no longer names it, the
   running application remains healthy, and a normal SHA deployment can still
   read `/vmh/prod/app-env`.

Deletion is intentionally not automated. If bootstrap must be repeated later,
an administrator resets the RDS master password, creates a new temporary
SecureString for the bootstrap, grants the EC2 role the narrow temporary read,
runs and verifies bootstrap, then removes both the parameter and permission
again. No master password is recoverable from this repository.

## Workstream 3: Private Database and Storage Guardrail

The application security group keeps one inbound rule: TCP 80 from
`0.0.0.0/0`. It has no inbound SSH rule. The database security group keeps one
inbound rule: TCP 5432 whose source is the application security group. It has
no public CIDR source for PostgreSQL.

Before changing RDS public accessibility, confirm both resources are in the
same VPC and that the application already resolves the RDS endpoint and
connects through the private path. Then:

1. attach only the intended database security group to RDS;
2. remove public CIDR access to TCP 5432;
3. set RDS public accessibility to `No`;
4. retain 20 GiB allocated storage and set the autoscaling maximum to 30 GiB;
5. apply the changes and wait until the instance is available;
6. verify internal health, authenticated database access, and one GitHub
   Actions deployment through SSM.

Record the prior security group attachments and rules before mutation. If the
application loses database connectivity, stop the work, inspect VPC placement,
DNS resolution, routing, and the security-group reference, then restore the
captured prior configuration if the private path cannot be corrected safely.
Do not add a public `0.0.0.0/0:5432` rule as a diagnostic shortcut.

## Workstream 4: Cost-conscious Always-on Operation

The demo remains online continuously only while measured usage is covered and
no actual or forecast charge is reported. The closeout verifies:

- the existing AWS Budget is active and sends notifications to a confirmed
  address;
- AWS Free Tier usage alerts are enabled;
- EC2, RDS, EBS, RDS storage, backup storage, public IPv4, ECR storage, and
  data transfer are reviewed in Billing;
- no unused Elastic IP, NAT Gateway, load balancer, manual RDS snapshot, extra
  database, or extra running instance exists for this project;
- RDS storage autoscaling cannot exceed 30 GiB;
- Docker log rotation remains bounded by the production Compose file.

Review Billing and Free Tier weekly for the first four weeks after acceptance,
then monthly. A budget alert, loss of Free Tier eligibility, exhausted credits,
or any unexplained actual or forecast charge is a stop condition: stop EC2 and
RDS, record that the public demo is temporarily unavailable, investigate the
charge, and decide whether to delete the resources or explicitly accept paid
operation. Stopping RDS is temporary because AWS starts a stopped instance
after seven consecutive days; storage and backup charges may continue while it
is stopped.

The budget is an alert, not a hard spending cap. This design therefore makes
no zero-cost guarantee and does not describe the demo as permanently free.

## Workstream 5: Production Acceptance

Use a new test-only identity and fictitious vehicle information. Never reuse a
personal or production password over the HTTP endpoint. The identity value and
password do not enter Git history, screenshots, issue comments, or Action
logs.

Acceptance covers:

1. public landing page loads over the current EC2 IPv4 address;
2. a test user registers, logs out, logs in, and refreshes an authenticated
   session;
3. the user creates, reads, updates, and deletes a vehicle;
4. the user creates, reads, updates, and deletes a maintenance record;
5. a retained fictitious vehicle and maintenance record survive a container
   restart performed through Session Manager;
6. the same retained records survive a later automatic SHA deployment from a
   merge to `main`;
7. internal and public Actuator endpoints report `UP`;
8. the application continues to operate after RDS public accessibility is
   disabled;
9. the successful deployment run identifies the same SHA tag published to
   Docker Hub and ECR.

Acceptance evidence is stored in `docs/portfolio-acceptance.md`. It contains
the date, full Git commit, deployed short SHA tag, Actions run link, result for
each check, and a note that only fictitious data was used. It contains no
password, access or refresh token, parameter value, RDS endpoint, command
invocation output, private IP, or AWS console screenshot.

## Workstream 6: Portfolio Presentation

Update `README.md` so a reviewer can understand and try the system without
reading the runbook. Add:

- a `Live Demo` section with the current HTTP URL, an availability disclaimer,
  and a warning to use only fictitious data and test-only credentials;
- two or three application screenshots from `docs/images/portfolio/` showing
  the vehicle list, vehicle details, and maintenance history with fictitious
  content;
- the deployed architecture and the distinction between public Docker Hub and
  private runtime ECR;
- the merge-to-deploy flow and immutable SHA provenance;
- the main security choices: OIDC, split IAM roles, SSM instead of SSH,
  encrypted EBS, private RDS, application-only database role, and Parameter
  Store;
- quality gates and the successful production acceptance link;
- explicit limitations: HTTP, single EC2, Single-AZ database, dynamic public
  IPv4, test data only, manual recovery, and Free Tier-dependent availability.

Screenshots must be cropped to the application UI, use fictitious records,
avoid email addresses and browser password managers, and remain legible in
GitHub's README width. The live URL may change after an EC2 stop/start; the
operator updates it and rechecks the link before presenting the portfolio.

## Workstream 7: Operations Documentation

Extend `docs/aws-deployment.md` with a post-bootstrap closeout section that
matches this design, including:

- credential deletion and IAM narrowing;
- private RDS verification;
- storage and cost guardrails;
- production acceptance commands and expected outcomes;
- the weekly-to-monthly billing review cadence;
- the cost stop condition and RDS seven-day automatic restart behavior;
- how to update the dynamic public demo URL;
- recovery instructions that do not expose or recreate secrets casually.

The runbook remains console-oriented. Read-only AWS CLI commands may be used to
verify state, but this phase does not add mutating AWS automation scripts or
infrastructure as code.

## Workstream 8: Future Roadmap

Create `docs/portfolio-roadmap.md`. Each initiative is explicitly outside the
current implementation and begins only when its trigger is met:

### HTTPS and stable domain

**Trigger:** the owner accepts domain/DNS cost and wants a stable public URL or
intends to use anything beyond fictitious test credentials.

**Next design:** compare a small reverse proxy with automated certificates on
the existing EC2 instance against an AWS-managed load balancer and certificate.
Cost remains a first-class decision.

### Observability and alerts

**Trigger:** the demo receives recurring traffic, failures cannot be diagnosed
from Session Manager and bounded Docker logs, or an availability target is
introduced.

**Next design:** define metrics, log retention, alarms, notification channel,
and a monthly cost ceiling before enabling paid ingestion.

### Automatic rollback

**Trigger:** multiple accepted SHA releases exist and recovery time becomes
more important than the simplicity of manual rollback.

**Next design:** preserve the previously healthy immutable tag, roll back only
after bounded health failure, verify the restored health, and retain complete
deployment diagnostics without exposing secrets.

### Terraform

**Trigger:** a second environment is required, the current environment must be
recreated, or infrastructure changes become frequent enough that console drift
is material.

**Next design:** inventory and import existing resources, define state storage
and locking costs, protect secrets from state, and migrate one resource class
at a time without replacement surprises.

### Possible ECS migration

**Trigger:** the application needs horizontal scaling, managed scheduling,
multiple instances, zero-downtime capacity replacement, or availability that
one EC2 instance cannot provide.

**Next design:** write an architecture decision record comparing the existing
EC2 deployment with ECS on EC2 and ECS Fargate, including load balancing,
networking, observability, deployment, and monthly cost. ECS is not treated as
inherently newer or better for the current workload.

## Files

This initiative produces or modifies:

- `docs/portfolio-acceptance.md` — non-sensitive production acceptance record;
- `docs/portfolio-roadmap.md` — future initiatives and their entry criteria;
- `docs/images/portfolio/*` — sanitized application screenshots;
- `docs/aws-deployment.md` — closeout, cost, verification, and recovery runbook;
- `README.md` — public portfolio presentation;
- `deploy/tests/workflow_contract_test.sh` only if a documentation link or
  deployment invariant needs an automated contract.

No application source, database migration, production Compose behavior, or
AWS resource definition changes are required by this design.

## Failure Handling and Recovery

- **Unhealthy preflight:** make no destructive change; diagnose the current
  deployment first.
- **Credential deletion uncertainty:** stop before deletion and verify
  `vmh_app` and database ownership. Never retain the master parameter merely
  for convenience after successful bootstrap.
- **IAM regression:** restore the last captured inline policy, confirm normal
  deployment, then narrow only the incorrect resource entry.
- **RDS connectivity regression:** do not open PostgreSQL publicly. Restore the
  captured security-group attachment or accessibility state while diagnosing
  the private VPC path.
- **Functional acceptance failure:** retain the failing evidence outside the
  public screenshots, create a focused defect, and do not mark acceptance
  complete.
- **Failed automatic deployment:** keep the previous accepted SHA available
  and follow the existing documented manual rollback procedure.
- **Detected charge:** stop EC2 and RDS, mark the demo unavailable, and inspect
  Billing before resuming.
- **Public IPv4 change:** update the README demo URL and repeat the public
  landing-page and health checks.

## Verification

Repository verification includes:

- all deployment shell contract suites;
- ShellCheck for deployment scripts when a shell file changes;
- actionlint when a workflow changes;
- `git diff --check`;
- a secret-pattern scan covering the changed documentation and images list;
- link and image-path checks for README artifacts;
- a diff review confirming no application or infrastructure behavior changed
  unintentionally.

Live verification includes:

- read-only AWS state checks before and after each mutation;
- internal health through Session Manager;
- public HTTP health and UI checks;
- functional CRUD and persistence checks with fictitious data;
- a successful post-closeout GitHub Actions SHA deployment;
- Billing, Budget, and Free Tier alert verification.

## Acceptance Criteria

This initiative is complete only when:

- `/vmh/prod/rds-master-password` is absent;
- the EC2 role can read only the application environment parameter required by
  deployment and cannot name the deleted master parameter;
- RDS reports public accessibility `No`;
- RDS TCP 5432 is reachable only from the application security group;
- EC2 has no inbound TCP 22 rule;
- RDS storage remains 20 GiB with a 30 GiB autoscaling maximum;
- internal and public health report `UP`;
- the functional and persistence checklist passes;
- a merge to `main` publishes and deploys one immutable SHA successfully;
- Budget and Free Tier notifications are verified and an always-on review
  cadence is recorded;
- README contains the live demo, sanitized screenshots, architecture, CI/CD,
  security decisions, and limitations;
- the acceptance record contains no sensitive information;
- the future roadmap contains all five initiatives and no current commitment
  to implement them;
- no new AWS service or paid portfolio dependency was introduced.

## Official References

- [AWS Systems Manager Parameter Store](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html)
- [Amazon RDS public accessibility](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_VPC.WorkingWithRDSInstanceinaVPC.html)
- [Amazon EC2 security group references](https://docs.aws.amazon.com/vpc/latest/userguide/security-group-rules.html#security-group-referencing)
- [Stopping an RDS DB instance temporarily](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_StopInstance.html)
- [Amazon VPC public IPv4 pricing](https://aws.amazon.com/vpc/pricing/)
- [AWS Free Tier usage alerts](https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/tracking-free-tier-usage.html)
- [Managing costs with AWS Budgets](https://docs.aws.amazon.com/cost-management/latest/userguide/budgets-managing-costs.html)
- [GitHub Actions OIDC with AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws)
