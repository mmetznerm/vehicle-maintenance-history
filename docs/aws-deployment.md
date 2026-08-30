# AWS Deployment

This runbook provisions the lightweight portfolio deployment in the `us-east-2` region.
Follow the steps in order and substitute only the values marked as outputs of
a command. Query commands are read-only; the runbook does not execute
changes in AWS for you.

## 1. Security, Costs, and Demo Limits

This is a public HTTP demonstration. It does not have HTTPS, a custom domain,
load balancer, or production-grade protection. Use test data and credentials only;
never put personal data, reused passwords, or real client secrets into the application.

Before starting, open RDS in the AWS Console, select
`vehicle-maintenance-history-db` and choose **Modify**. Under **Storage**,
configure the maximum autoscaling storage threshold to **30 GiB** and
apply the change according to the selected maintenance window. The instance currently uses
20 GiB; the 30 GiB cap prevents accidental growth in a portfolio environment.
The `db.t4g.micro`, `t3.micro`, and 10 GiB volume are low-cost choices, but
they continue to incur charges while active.

When finishing the demonstration, stop or delete resources that are no longer
needed and review the Billing Dashboard. Do not remove RDS before deciding
how to preserve test data.

## 2. Private Amazon ECR

1. Region selector: **US East (Ohio) `us-east-2`**.
2. Open **Amazon ECR > Repositories** and confirm the private repository
   `mmetznerm/vehicle-maintenance-history`.
3. Do not change the repository to public and do not use another name. The workflow
   publishes the same build to Docker Hub and this ECR; EC2 uses only the
   private ECR at runtime.
4. Under **Tag immutability**, select **Immutable with exclusion** and configure
   the single exclusion as **Wildcard** with value `latest`. The `latest` tag is
   only a convenience pointer; deployments and rollbacks use only `sha-<commit>`.
5. As an alternative to the console, apply and verify the configuration with:

   ```bash
   aws ecr put-image-tag-mutability \
     --region us-east-2 \
     --repository-name mmetznerm/vehicle-maintenance-history \
     --image-tag-mutability IMMUTABLE_WITH_EXCLUSION \
     --image-tag-mutability-exclusion-filters filterType=WILDCARD,filter=latest

   aws ecr describe-repositories \
     --region us-east-2 \
     --repository-names mmetznerm/vehicle-maintenance-history \
     --query 'repositories[0].{mutability:imageTagMutability,exclusions:imageTagMutabilityExclusionFilters}' \
     --output json
   ```

   The output must show `IMMUTABLE_WITH_EXCLUSION` and only the `latest` exclusion.
   A workflow re-run cannot republish a SHA tag that already exists; use the existing SHA image for deployment or rollback.

## 3. Network and Security Groups

Use the same VPC as RDS. In the console, open **VPC > Security groups** and create
`vehicle-maintenance-history-app-sg` with a description identifying the application.
Under **Inbound rules**, add only:

| Type | Protocol | Port | Source |
|---|---|---:|---|
| HTTP | TCP | 80 | `0.0.0.0/0` |

Keep the necessary outbound traffic for ECR, SSM, Docker Hub, and RDS (default egress allows this).
Do not add a TCP 22 rule.

Create or confirm `vehicle-maintenance-history-db-sg`. On the database group,
add only this entry:

| Type | Protocol | Port | Source |
|---|---|---:|---|
| PostgreSQL | TCP | 5432 | reference to `vehicle-maintenance-history-app-sg` |

In **RDS > Databases > vehicle-maintenance-history-db > Modify > Connectivity**, associate `vehicle-maintenance-history-db-sg` with RDS and confirm
**Public access: No**. The port 5432 source must be the security group reference, never a public CIDR. Review both security groups and confirm neither exposes port 22.

## 4. GitHub OIDC Provider

In **IAM > Identity providers**, add an **OpenID Connect** provider with:

- Provider URL: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

If this provider already exists, reuse it after confirming exactly these two values.
The workflow receives temporary credentials via OIDC; do not create or store long-lived AWS credentials in GitHub.

## 5. GitHub Actions Roles

In **IAM > Roles > Create role**, create the two roles below using the GitHub OIDC provider and the same trust policy, restricted to the repository and the `main` branch:

- `github-actions-vehicle-maintenance-history-publish`;
- `github-actions-vehicle-maintenance-history-deploy`.

The equivalent trust policy applied to both is:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::675244612319:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:mmetznerm/vehicle-maintenance-history:ref:refs/heads/main"
        }
      }
    }
  ]
}
```

For `github-actions-vehicle-maintenance-history-publish`, add an inline policy with only ECR login and publishing permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EcrLogin",
      "Effect": "Allow",
      "Action": "ecr:GetAuthorizationToken",
      "Resource": "*"
    },
    {
      "Sid": "EcrPushApplication",
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": "arn:aws:ecr:us-east-2:675244612319:repository/mmetznerm/vehicle-maintenance-history"
    }
  ]
}
```

For `github-actions-vehicle-maintenance-history-deploy`, add an inline policy with only the SSM permissions below. The instance ARN will be completed with the actual ID obtained in step 8; `<EC2_INSTANCE_ID>` is a required field to replace, not an example value:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SsmSendToSelectedInstance",
      "Effect": "Allow",
      "Action": "ssm:SendCommand",
      "Resource": [
        "arn:aws:ec2:us-east-2:675244612319:instance/<EC2_INSTANCE_ID>",
        "arn:aws:ssm:us-east-2::document/AWS-RunShellScript"
      ]
    },
    {
      "Sid": "SsmCommandRead",
      "Effect": "Allow",
      "Action": "ssm:GetCommandInvocation",
      "Resource": "*"
    }
  ]
}
```

`id-token: write` remains restricted to jobs assuming each role. The publishing role cannot receive SSM permissions, and the deploy role cannot receive ECR permissions; neither can receive permissions from the other. Do not add generic `sts:AssumeRole`, access to other repositories, other branches, or other ECR repositories.

## 6. EC2 Instance Role

In **IAM > Roles > Create role**, choose **EC2** as trusted service and create `vehicle-maintenance-history-ec2-role`. Attach the managed policies:

- `AmazonSSMManagedInstanceCore`;
- `AmazonEC2ContainerRegistryPullOnly`.

Add an inline policy to read application parameters only and decrypt SecureStrings via Systems Manager in Ohio:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadApplicationParameters",
      "Effect": "Allow",
      "Action": "ssm:GetParameter",
      "Resource": [
        "arn:aws:ssm:us-east-2:675244612319:parameter/vmh/prod/app-env",
        "arn:aws:ssm:us-east-2:675244612319:parameter/vmh/prod/rds-master-password"
      ]
    },
    {
      "Sid": "DecryptOnlyThroughSsmOhio",
      "Effect": "Allow",
      "Action": "kms:Decrypt",
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "kms:ViaService": "ssm.us-east-2.amazonaws.com"
        }
      }
    }
  ]
}
```

The script uses `ssm:GetParameter` with `--with-decryption`; do not grant the instance permission to delete or modify parameters. Access to the master parameter exists only for bootstrap and will be removed in step 10.

The KMS condition allows only `kms:Decrypt` invoked by Systems Manager in `us-east-2`. Use default account keys for SSM and EBS; a customer-managed key is optional and should only be chosen deliberately.

## 7. SecureString Parameters

Open **Systems Manager > Parameter Store > Create parameter**. Use type **SecureString**, region `us-east-2`, and do not log contents in repository files or logs.

First generate both values locally or in CloudShell. Copy each output directly into the console form and do not publish the output:

```bash
APP_DB_PASSWORD="$(openssl rand -hex 32)"
APP_JWT_VALUE="$(openssl rand -hex 64)"
printf '%s\n' "$APP_DB_PASSWORD"
printf '%s\n' "$APP_JWT_VALUE"
```

To get the actual endpoint, run in CloudShell, in the correct region:

```bash
aws rds describe-db-instances --region us-east-2 --db-instance-identifier vehicle-maintenance-history-db --query 'DBInstances[0].Endpoint.Address' --output text
```

Copy the returned text, without spaces or quotes, for the `RDS_HOST` value and the JDBC URL host. Create `/vmh/prod/app-env` with six line-break separated entries. On each line, join the name and value with `=`; do not include the descriptive headers below:

| Name | Value |
|---|---|
| `RDS_HOST` | endpoint returned by the RDS command |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL for the endpoint, port 5432, database `vehicle_maintenance_history`, and `sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `vmh_app` |
| `SPRING_DATASOURCE_PASSWORD` | first random output generated above |
| `JWT_SECRET` | second random output generated above |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=70.0` |

Variable names must remain exactly as above: `RDS_HOST` is used by bootstrap, and `SPRING_DATASOURCE_*` and `JWT_SECRET` are passed by Compose to the application. Do not add `IMAGE_URI` or `IMAGE_TAG`; `deploy.sh` appends these values to the local file on EC2.

Create `/vmh/prod/rds-master-password` as **SecureString**, containing the existing password for master user `vmh_admin` on RDS. This value is not `vmh_app`'s password, must not be shown in the terminal, and will only remain available until initial database and application role creation.

## 8. EC2 Instance

In **EC2 > Launch instance**, configure:

- AMI: **Amazon Linux 2023**, architecture **x86_64**;
- type: `t3.micro`;
- root storage: **10 GiB**, `gp3`, **Encrypted: Yes**, using default account EBS key;
- instance metadata: **Metadata version: V2 only** (IMDSv2);
- network: same VPC as RDS and a public subnet with internet egress;
- auto-assign public IPv4: enabled;
- security group: `vehicle-maintenance-history-app-sg`;
- IAM instance profile: `vehicle-maintenance-history-ec2-role`;
- tags: `Name=vehicle-maintenance-history` and `Application=vehicle-maintenance-history`.

Do not configure an SSH rule or rely on a static IP address. Operational access is via **Systems Manager > Session Manager**; IPv4 address changes after a stop do not alter the SSM target, which is the instance ID.

After instance creation, obtain the actual ID in CloudShell with the read-only command below:

```bash
aws ec2 describe-instances --region us-east-2 --filters 'Name=tag:Name,Values=vehicle-maintenance-history' 'Name=instance-state-name,Values=pending,running,stopping,stopped' --query 'Reservations[0].Instances[0].InstanceId' --output text
```

Copy output to the `EC2_INSTANCE_ID` field in repository variables and replace `<EC2_INSTANCE_ID>` in GitHub role policy ARN (step 5). This same ID must appear as a managed instance in Systems Manager before initial bootstrap.

## 9. First Manual Deployment

Before the first push, create basic section 11 variables, leaving `AWS_DEPLOY_ENABLED` absent. This allows workflow ECR publishing without attempting SSM send to an unprepared instance.

1. In **Systems Manager > Fleet Manager**, confirm instance is **Online** and open a session in **Session Manager**.
2. In **ECR**, copy the first published `sha-<commit>` tag for `mmetznerm/vehicle-maintenance-history`.
3. Open the `main` workflow run that published this tag and copy full 40-character hexadecimal commit. Short tag and commit must be from the same run.
4. In EC2 session, download the three files from full commit. Directory below is temporary for bootstrap; script copies production files to `/opt/vehicle-maintenance-history`:

   ```bash
   sudo mkdir -p /opt/vehicle-maintenance-history/bootstrap
   cd /opt/vehicle-maintenance-history/bootstrap
   sudo curl -fsSL https://raw.githubusercontent.com/mmetznerm/vehicle-maintenance-history/<full-commit>/deploy/compose.prod.yml -o compose.prod.yml
   sudo curl -fsSL https://raw.githubusercontent.com/mmetznerm/vehicle-maintenance-history/<full-commit>/deploy/deploy.sh -o deploy.sh
   sudo curl -fsSL https://raw.githubusercontent.com/mmetznerm/vehicle-maintenance-history/<full-commit>/deploy/setup-ec2.sh -o setup-ec2.sh
   ```

   Replace `<full-commit>` with full SHA copied in previous step.
5. Execute bootstrap as root, using full ECR URI and copied tag:

   ```bash
   sudo bash setup-ec2.sh 675244612319.dkr.ecr.us-east-2.amazonaws.com/mmetznerm/vehicle-maintenance-history <sha-tag>
   ```

   `<sha-tag>` must match `sha-` followed by published hexadecimal value. Script installs Docker/Compose, creates 1 GiB swap, uses SecureStrings, creates `vmh_app` and `vehicle_maintenance_history` if needed, and calls `deploy.sh` with immutable image.
6. In session output, confirm `Health check succeeded` and final message `Bootstrap complete. After confirming the application is healthy, delete /vmh/prod/rds-master-password.`. If health check fails, do not delete master parameter; resolve cause and retry bootstrap.

## 10. Remove Bootstrap Credential

Remove bootstrap credential only in this exact sequence:

1. Confirm `UP` on internal and public Actuator.
2. Confirm `SPRING_DATASOURCE_USERNAME` in effective environment is `vmh_app`.
3. Obtain explicit approval to delete only `/vmh/prod/rds-master-password`.
4. In **Systems Manager > Parameter Store**, delete this parameter. An administrator can also execute deletion via CloudShell with `ssm:DeleteParameter`; do not grant this permission to instance role.
5. Remove parameter ARN from EC2 role inline policy while keeping `/vmh/prod/app-env`.
6. Confirm internal and public Actuator again.

Final EC2 inline policy must allow reading a single parameter:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadApplicationParameters",
      "Effect": "Allow",
      "Action": "ssm:GetParameter",
      "Resource": "arn:aws:ssm:us-east-2:675244612319:parameter/vmh/prod/app-env"
    },
    {
      "Sid": "DecryptOnlyThroughSsmOhio",
      "Effect": "Allow",
      "Action": "kms:Decrypt",
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "kms:ViaService": "ssm.us-east-2.amazonaws.com"
        }
      }
    }
  ]
}
```

If another bootstrap is needed, temporarily reset RDS administrative credential, create new temporary `SecureString` with new name, grant access only during recovery, execute bootstrap, and repeat full closeout above. Never reuse `/vmh/prod/app-env` for administrative credential and never retain temporary permission after recovery.

## 11. GitHub Variables and Secrets

In **GitHub > repository > Settings > Secrets and variables > Actions > Variables**, create:

| Name | Value |
|---|---|
| `AWS_REGION` | `us-east-2` |
| `AWS_PUBLISH_ROLE_ARN` | `arn:aws:iam::675244612319:role/github-actions-vehicle-maintenance-history-publish` |
| `AWS_DEPLOY_ROLE_ARN` | `arn:aws:iam::675244612319:role/github-actions-vehicle-maintenance-history-deploy` |
| `ECR_REPOSITORY` | `mmetznerm/vehicle-maintenance-history` |
| `EC2_INSTANCE_ID` | actual output of `describe-instances` command from step 8 |

During preparation, leave `AWS_DEPLOY_ENABLED` **absent**. After initial manual deploy and all verifications pass, create this variable with value `true`. Deploy job runs on `main` push only when set to `true`.

In **Secrets**, keep existing secrets `DOCKER_USERNAME` and `DOCKER_ACCESS_TOKEN`, used only for Docker Hub publishing. Do not add persistent AWS secrets: OIDC role provides temporary credentials to publish and deploy jobs. Publish assumes `AWS_PUBLISH_ROLE_ARN` and deploy assumes `AWS_DEPLOY_ROLE_ARN`.

Production deploys are serialized in GitHub Actions and again on EC2 via `/var/lock/vehicle-maintenance-history-deploy.lock`. Workflow bounds delivery/start to 60 seconds via `send-command` `--timeout-seconds` and sends remote execution `executionTimeout` of 1200 seconds (20 minutes) to `AWS-RunShellScript`. Then polls SSM state for up to 130 attempts with 10-second interval: 129 intervals total ~21 minutes 30 seconds. This 1290-second window covers 60-second delivery, 1200-second remote execution, and 30-second polling margin. ECR vulnerability scanning and lifecycle policy are optional enhancements and not prerequisites for demo acceptance.

## 12. Acceptance Checks

Consider deployment accepted only when all items below are true:

- same `sha-<commit>` tag exists on Docker Hub and private ECR;
- GitHub Actions deploy job completes successfully;
- public frontend opens via `http://<Public-IPv4>/` (no HTTPS in this phase);
- `http://<Public-IPv4>/actuator/health` responds with status `UP`;
- registration, logout, login, and refresh call work with fictitious data only;
- vehicle and maintenance CRUD operations function properly;
- data persists after container restart and subsequent deployment;
- root EBS 10 GiB `gp3` volume shows **Encrypted: Yes** before application secrets installation;
- `vehicle-maintenance-history-app-sg` security group exposes TCP 80 only with no TCP 22 ingress;
- RDS reports `PubliclyAccessible=false` with only dedicated database security group attached;
- database security group permits PostgreSQL TCP 5432 only with `vehicle-maintenance-history-app-sg` source;
- GitHub contains no persistent AWS credentials;
- RDS retains 20 GiB allocated and 30 GiB autoscaling limit;
- `/vmh/prod/rds-master-password` does not exist and EC2 inline policy omits this parameter, while `/vmh/prod/app-env` remains available;
- public evidence in [Portfolio Deployment Acceptance](portfolio-acceptance.md) is updated with accepted commit, workflow, and tag.

Execute functional acceptance with disposable identity and fictitious values. Do not log identity, password, or tokens. For registration, logout, and login:

1. Open `/register`, create test account, and wait for `/vehicles` to load.
2. Select **Sign out** and confirm redirection to `/login`.
3. Sign in again and confirm `/vehicles` opens without authentication error.

In browser developer console, validate refresh without printing tokens:

```javascript
void (async () => {
  const currentRefreshValue = localStorage.getItem("vehicle-history.refreshToken");
  if (!currentRefreshValue) throw new Error("refresh value is absent");
  const response = await fetch("/v1/auth/refresh", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken: currentRefreshValue }),
  });
  if (!response.ok) throw new Error(`refresh failed with ${response.status}`);
  const nextValues = await response.json();
  localStorage.setItem("vehicle-history.accessToken", nextValues.accessToken);
  localStorage.setItem("vehicle-history.refreshToken", nextValues.refreshToken);
  console.log("refresh accepted", response.status);
})();
```

Expected result is only `refresh accepted 200`; no values printed. Reload `/vehicles` and confirm session remains authenticated.

For CRUD, create fictitious vehicle, open details, edit make, model, year, color, save and verify each value; then delete and confirm removed from list. On another fictitious vehicle, repeat create, list, edit, delete maintenance, verifying date, odometer, description, cost after each save.

To prove restart persistence, retain vehicle and maintenance and run in Session Manager:

```bash
sudo docker restart vehicle-maintenance-history-app-1
for attempt in $(seq 1 12); do
  if curl -fsS http://localhost/actuator/health >/dev/null; then
    printf 'Health check succeeded\n'
    break
  fi
  if [ "$attempt" -eq 12 ]; then
    printf 'Health check failed\n' >&2
    exit 1
  fi
  sleep 5
done
```

Expected result is `Health check succeeded`. Reopen UI and confirm both records remain present.

After `main` deploy, execute:

```bash
sudo docker inspect --format '{{.Config.Image}}' vehicle-maintenance-history-app-1
curl -fsS http://localhost/actuator/health
```

Image tag must be `sha-` followed by first 7 characters of deployed commit, Actuator must respond `UP`, and retained records must remain available in UI. Record commit, tag, workflow URL, result only; never record identity or credentials.

## 13. Operations and Manual Rollback

Use **Session Manager** session, not SSH, to inspect service:

```bash
cd /opt/vehicle-maintenance-history
sudo docker compose --env-file .env -f compose.prod.yml ps
sudo docker compose --env-file .env -f compose.prod.yml logs --tail=200 app
curl -fsS http://localhost/actuator/health
```

To list available tags in ECR via CloudShell:

```bash
aws ecr describe-images --region us-east-2 --repository-name mmetznerm/vehicle-maintenance-history --query 'sort_by(imageDetails,&imagePushedAt)[].imageTags' --output table
```

For audit, record digest associated with production SHA tag. Read-only command; keep tag in deployment:

```bash
aws ecr describe-images --region us-east-2 --repository-name mmetznerm/vehicle-maintenance-history --image-ids imageTag=<deployed-sha-tag> --query 'imageDetails[0].imageDigest' --output text
```

Select `sha-` from previously accepted run and in EC2 session execute manual rollback:

```bash
sudo /opt/vehicle-maintenance-history/deploy.sh 675244612319.dkr.ecr.us-east-2.amazonaws.com/mmetznerm/vehicle-maintenance-history <previous-sha-tag>
```

Confirm `Health check succeeded` and Actuator again. Project does not perform automatic rollback; if previous tag is not in ECR, publish/recover corresponding image before command execution.

Official references: [GitHub OIDC with AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws),
[ECR Private Registry Authentication](https://docs.aws.amazon.com/AmazonECR/latest/userguide/registry_auth.html),
[AmazonEC2ContainerRegistryPullOnly Policy](https://docs.aws.amazon.com/AmazonECR/latest/userguide/security-iam-awsmanpol.html), and
[Connecting to RDS PostgreSQL](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_ConnectToPostgreSQLInstance.html).

## 14. Continuous Operations and Costs

In the first four weeks, review weekly **Billing**, **Budgets**, **Free Tier**, credits, EC2, RDS, EBS, snapshots, ECR, Elastic IPs, NAT Gateways, and load balancers. After this period, conduct the same review monthly and before enabling any new service.

The budget generates alerts; it does not block or cap spending. If there is unexpected actual or forecasted cost, stop EC2 and RDS, mark demo as unavailable, and investigate **Billing** before resuming. Stopped RDS automatically restarts after seven days, and storage and backups may continue generating charges even with stopped instance. Thus, stopping resources is a temporary response, not permanent cost control.

EC2 public IPv4 is dynamic. After any stop, start, or network change, obtain current address with read-only command:

```bash
aws ec2 describe-instances \
  --region us-east-2 \
  --instance-ids i-0aac27aeabf6e94c3 \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text
```

If address changes, update demo link in README and repeat public UI opening and `http://<Public-IPv4>/actuator/health` verification.

Proven criteria are in [Portfolio Deployment Acceptance](portfolio-acceptance.md). Next infrastructure decisions and triggers are in [Portfolio Infrastructure Roadmap](portfolio-roadmap.md).
