# Portfolio Production Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the live AWS portfolio deployment safely, prove its production behavior, present it clearly in the repository, and record the optional infrastructure roadmap without adding paid services.

**Architecture:** Preserve the existing GitHub Actions to Docker Hub and ECR to SSM to EC2 to private RDS flow. Perform console-oriented AWS closeout behind explicit mutation gates, collect only non-sensitive evidence, then publish portfolio documentation and verify one immutable SHA deployment from `main`.

**Tech Stack:** GitHub Actions, GitHub CLI, AWS Console, AWS CloudShell CLI, Systems Manager Session Manager, EC2, RDS PostgreSQL, IAM, Parameter Store, AWS Budgets, Docker Compose, Spring Boot Actuator, React UI, Markdown, PowerShell, and Git Bash.

**Spec:** `docs/superpowers/specs/2026-08-22-portfolio-production-readiness-design.md`

## Global Constraints

- Region remains exactly `us-east-2`.
- EC2 remains Amazon Linux 2023 x86_64 `t3.micro`, encrypted 10 GiB gp3, IMDSv2 only, instance ID `i-0aac27aeabf6e94c3`.
- RDS remains PostgreSQL, Single-AZ `db.t4g.micro`, database identifier `vehicle-maintenance-history-db`.
- RDS allocated storage remains 20 GiB and storage autoscaling is capped at 30 GiB.
- Runtime deploys use immutable ECR tags in the exact `sha-abcdef0` shape; `latest` is never a deploy or rollback target.
- Docker Hub remains public portfolio delivery; EC2 pulls only from private ECR.
- GitHub uses OIDC roles `github-actions-vehicle-maintenance-history-publish` and `github-actions-vehicle-maintenance-history-deploy`; no persistent AWS keys are added.
- EC2 operations use Systems Manager Session Manager; do not create an inbound TCP 22 rule.
- The public endpoint remains HTTP-only and may receive only fictitious data and test-only credentials.
- Do not create ALB, NAT Gateway, Route 53, custom domain, certificate, Aurora, ECS, EKS, RDS Proxy, Multi-AZ, extra EC2, paid monitoring, or automated shutdown resources.
- Do not print, copy into Git, screenshot, or preserve Parameter Store values, passwords, access tokens, refresh tokens, AWS session credentials, or personal data.
- Every AWS or GitHub mutation is a separate approval gate. Parameter deletion, policy edits, security-group edits, RDS modification, workflow rerun, push, PR creation, and merge each require explicit user authorization immediately before execution.
- If a precondition or verification fails, stop that task, preserve the last known healthy state, and do not advance to the next mutation.
- Use `apply_patch` for repository text edits. Stage only explicit paths; never use `git add .`, `git add -A`, or `git add --all`.
- The active documentation branch is `codex/portfolio-production-readiness`, based on `origin/main` at `abc031638f007c294dc87fe87f17896518c19cce` when this plan was written.

## File Structure

- Create `docs/portfolio-acceptance.md`: public, non-sensitive acceptance evidence and operating review cadence.
- Create `docs/portfolio-roadmap.md`: entry criteria for HTTPS, observability, automatic rollback, Terraform, and possible ECS migration.
- Create `docs/images/portfolio/vehicles.png`: sanitized 1440-by-900-or-smaller application vehicle-list capture.
- Create `docs/images/portfolio/maintenance-history.png`: sanitized 1440-by-900-or-smaller vehicle and maintenance capture.
- Optionally create `docs/images/portfolio/login.png`: sanitized login capture only if it adds information not shown by the other two images.
- Modify `docs/aws-deployment.md:358-453`: close bootstrap IAM access, strengthen acceptance, add always-on cost operations, and document dynamic URL maintenance.
- Modify `README.md:8-128`: add the live demo, screenshots, architecture, acceptance evidence, and deployment/security summary.
- Modify `README.md:313-324`: replace the flat out-of-scope list with a link to the trigger-based roadmap while preserving explicit current limitations.
- Do not modify Java, TypeScript, database migrations, Dockerfile, production Compose, deployment shell scripts, or GitHub workflows.

---

### Task 1: Capture the Read-only Production Baseline

**Files:**

- Read: `docs/superpowers/specs/2026-08-22-portfolio-production-readiness-design.md`
- Read: `docs/aws-deployment.md`
- Read: `.github/workflows/ci-cd.yml`
- Verify: live GitHub and AWS resources only; create no repository file yet.

**Interfaces:**

- Consumes: healthy deployment `sha-abc0316`, current EC2/RDS/IAM/SSM/Budget state, and GitHub repository variables.
- Produces: exact non-sensitive baseline facts used by Tasks 2-8 and a captured recovery reference for IAM and network changes.

- [ ] **Step 1: Confirm isolated branch and clean starting point**

Run in the worktree:

```powershell
git status --short --branch
git log -2 --oneline --decorate
git diff --check origin/main...HEAD
```

Expected: branch is `codex/portfolio-production-readiness`; it is ahead of `origin/main` only by the spec and plan commits; status has no unrelated changes; diff check exits zero.

- [ ] **Step 2: Confirm GitHub deployment configuration without exposing secrets**

Run:

```powershell
gh variable get AWS_DEPLOY_ENABLED --repo mmetznerm/vehicle-maintenance-history
gh variable get AWS_REGION --repo mmetznerm/vehicle-maintenance-history
gh variable get EC2_INSTANCE_ID --repo mmetznerm/vehicle-maintenance-history
gh variable get ECR_REPOSITORY --repo mmetznerm/vehicle-maintenance-history
gh run list --repo mmetznerm/vehicle-maintenance-history --workflow ci-cd.yml --branch main --limit 3
```

Expected: values are `true`, `us-east-2`, `i-0aac27aeabf6e94c3`, and `mmetznerm/vehicle-maintenance-history`; the latest completed `main` run is successful. Do not query repository secrets.

- [ ] **Step 3: Capture the EC2 and RDS topology with read-only CloudShell commands**

Run in AWS CloudShell:

```bash
export VMH_REGION=us-east-2
export VMH_INSTANCE_ID=i-0aac27aeabf6e94c3
export VMH_DB_ID=vehicle-maintenance-history-db

aws ec2 describe-instances \
  --region "$VMH_REGION" \
  --instance-ids "$VMH_INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].{State:State.Name,Type:InstanceType,VpcId:VpcId,SubnetId:SubnetId,PublicIp:PublicIpAddress,SecurityGroups:SecurityGroups,Profile:IamInstanceProfile.Arn,MetadataTokens:MetadataOptions.HttpTokens,RootDevice:RootDeviceName}' \
  --output json

export VMH_ROOT_VOLUME_ID="$(aws ec2 describe-instances \
  --region "$VMH_REGION" \
  --instance-ids "$VMH_INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].BlockDeviceMappings[?DeviceName==`/dev/xvda` || DeviceName==`/dev/sda1`].Ebs.VolumeId | [0]' \
  --output text)"

aws ec2 describe-volumes \
  --region "$VMH_REGION" \
  --volume-ids "$VMH_ROOT_VOLUME_ID" \
  --query 'Volumes[0].{Encrypted:Encrypted,SizeGiB:Size,Type:VolumeType}' \
  --output json

aws rds describe-db-instances \
  --region "$VMH_REGION" \
  --db-instance-identifier "$VMH_DB_ID" \
  --query 'DBInstances[0].{Status:DBInstanceStatus,Class:DBInstanceClass,VpcId:DBSubnetGroup.VpcId,Public:PubliclyAccessible,Groups:VpcSecurityGroups[*].VpcSecurityGroupId,AllocatedGiB:AllocatedStorage,MaxGiB:MaxAllocatedStorage,MultiAZ:MultiAZ,Engine:Engine,EngineVersion:EngineVersion}' \
  --output json
```

Expected: EC2 is `running`, `t3.micro`, encrypted gp3 10 GiB, `MetadataTokens` is `required`; RDS is `available`, `db.t4g.micro`, PostgreSQL, Single-AZ, 20 GiB. Record the EC2 VPC ID, app security-group ID, RDS VPC ID, database security-group ID, current `Public` value, and current `MaxGiB` for recovery. Do not store the endpoint.

- [ ] **Step 4: Capture security-group rules before mutation**

Use the exact group IDs returned by Step 3:

```bash
export VMH_APP_SG_ID=sg-0cf10a28c87a5434f
export VMH_DB_SG_ID="$(aws rds describe-db-instances \
  --region "$VMH_REGION" \
  --db-instance-identifier "$VMH_DB_ID" \
  --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' \
  --output text)"

aws ec2 describe-security-groups \
  --region "$VMH_REGION" \
  --group-ids "$VMH_APP_SG_ID" "$VMH_DB_SG_ID" \
  --query 'SecurityGroups[*].{Id:GroupId,Name:GroupName,VpcId:VpcId,Inbound:IpPermissions}' \
  --output json
```

Expected: both groups are in the EC2/RDS VPC; application inbound is TCP 80 from `0.0.0.0/0` with no TCP 22; database inbound can be distinguished precisely before edits. Stop if `sg-0cf10a28c87a5434f` is not attached to the target EC2 instance. The currently observed database group is `sg-037abedbb4edca29f`; always confirm both live attachments before mutation.

- [ ] **Step 5: Capture Parameter Store metadata and EC2 inline policy resources**

Run:

```bash
aws ssm describe-parameters \
  --region "$VMH_REGION" \
  --parameter-filters 'Key=Name,Option=Equals,Values=/vmh/prod/app-env' \
  --query 'Parameters[*].{Name:Name,Type:Type,KeyId:KeyId}' \
  --output json

aws ssm describe-parameters \
  --region "$VMH_REGION" \
  --parameter-filters 'Key=Name,Option=Equals,Values=/vmh/prod/rds-master-password' \
  --query 'Parameters[*].{Name:Name,Type:Type,KeyId:KeyId}' \
  --output json

aws iam list-role-policies \
  --role-name vehicle-maintenance-history-ec2-role \
  --output json
```

Require exactly one project inline policy, capture its name, and inspect it:

```bash
export VMH_EC2_POLICY_NAME="$(aws iam list-role-policies \
  --role-name vehicle-maintenance-history-ec2-role \
  --query 'PolicyNames[0]' \
  --output text)"

test -n "$VMH_EC2_POLICY_NAME"
test "$VMH_EC2_POLICY_NAME" != None

aws iam get-role-policy \
  --role-name vehicle-maintenance-history-ec2-role \
  --policy-name "$VMH_EC2_POLICY_NAME" \
  --query 'PolicyDocument.Statement[*].{Sid:Sid,Action:Action,Resource:Resource,Condition:Condition}' \
  --output json
```

Expected: both parameters are `SecureString`, and one inline statement names both exact parameter ARNs without wildcard access. Never call `ssm:GetParameter --with-decryption` from CloudShell. If more than one project inline policy is present, inspect every returned name and identify the Parameter Store policy by its exact resources before continuing.

- [ ] **Step 6: Confirm the runtime image, health, and application database user through Session Manager**

In the existing Session Manager shell, run:

```bash
sudo docker inspect --format '{{.Config.Image}}' vehicle-maintenance-history-app-1
curl -fsS http://localhost/actuator/health
sudo awk -F= '$1 == "SPRING_DATASOURCE_USERNAME" { print $2 }' /opt/vehicle-maintenance-history/.env
```

Expected: image is the Ohio ECR repository with tag `sha-abc0316`; health contains `"status":"UP"`; the last command prints only `vmh_app`. Stop if any other username appears. Do not print the `.env` file.

- [ ] **Step 7: Confirm the public endpoint**

Open `http://3.128.87.156/` and `http://3.128.87.156/actuator/health` in the browser. If the EC2 public IP returned in Step 3 differs, use that returned IP instead and record it for README editing.

Expected: the application loads and public health contains `"status":"UP"`. A client-side browser blocker is not proof of application failure; confirm internal health and test from a separate browser/network before diagnosing AWS.

---

### Task 2: Remove the Bootstrap Master Credential and Narrow EC2 IAM

**Files:**

- Modify live AWS Parameter Store state.
- Modify live IAM inline policy on `vehicle-maintenance-history-ec2-role`.
- Do not edit repository files in this task.

**Interfaces:**

- Consumes: Task 1 healthy runtime, confirmed `vmh_app`, exact inline policy name, and explicit deletion approval.
- Produces: absent `/vmh/prod/rds-master-password`; EC2 role retains only application-parameter read plus KMS-through-SSM decryption.

- [ ] **Step 1: Re-run the irreversible deletion preconditions**

Immediately before deletion, run in Session Manager:

```bash
curl -fsS http://localhost/actuator/health
sudo awk -F= '$1 == "SPRING_DATASOURCE_USERNAME" { print $2 }' /opt/vehicle-maintenance-history/.env
```

Expected: `UP` and `vmh_app`.

- [ ] **Step 2: Obtain explicit deletion authorization**

Ask exactly:

> The application is healthy with `vmh_app`. May I permanently delete `/vmh/prod/rds-master-password` now? Recovery would require resetting the RDS master password and creating a new temporary parameter.

Expected: an explicit yes. A previous general plan approval is not sufficient. Stop on any other answer.

- [ ] **Step 3: Delete only the temporary master parameter in the console**

In AWS Console, select region **US East (Ohio)**, open **Systems Manager > Parameter Store**, select only `/vmh/prod/rds-master-password`, choose **Delete**, verify the confirmation names exactly that parameter, and confirm.

Expected: `/vmh/prod/app-env` remains and the master parameter disappears. Do not select multiple parameters.

- [ ] **Step 4: Verify deletion without decryption**

Run in CloudShell:

```bash
aws ssm describe-parameters \
  --region us-east-2 \
  --parameter-filters 'Key=Name,Option=Equals,Values=/vmh/prod/rds-master-password' \
  --query 'length(Parameters)' \
  --output text
```

Expected: `0`.

- [ ] **Step 5: Obtain authorization to narrow the EC2 inline policy**

Show the exact current policy name and state that only the deleted master-parameter ARN will be removed. Obtain an explicit yes before saving the IAM edit.

- [ ] **Step 6: Replace the inline policy with the least-privilege document**

Open **IAM > Roles > vehicle-maintenance-history-ec2-role > Permissions**, edit the Parameter Store inline policy, switch to JSON, and save exactly:

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

Expected: IAM validation succeeds; managed policies `AmazonSSMManagedInstanceCore` and `AmazonEC2ContainerRegistryPullOnly` remain attached; no other role is edited.

- [ ] **Step 7: Verify policy and runtime after propagation**

Run in CloudShell with the exact inline policy name captured in Task 1:

```bash
aws iam get-role-policy \
  --role-name vehicle-maintenance-history-ec2-role \
  --policy-name "$VMH_EC2_POLICY_NAME" \
  --query 'PolicyDocument.Statement[*].{Sid:Sid,Action:Action,Resource:Resource,Condition:Condition}' \
  --output json
```

Then run in Session Manager:

```bash
curl -fsS http://localhost/actuator/health
```

Expected: the policy names only `/vmh/prod/app-env`, contains no `rds-master-password`, and health remains `UP`. If normal application health fails, restore the exact Task 1 policy document and investigate before proceeding.

---

### Task 3: Make RDS Private and Cap Storage Growth

**Files:**

- Modify live EC2/RDS security-group state only if Task 1 found extra database ingress.
- Modify live RDS connectivity and storage settings.
- Do not edit repository files in this task.

**Interfaces:**

- Consumes: Task 1 VPC and security-group IDs, captured prior state, healthy EC2-to-RDS connection, and explicit network/RDS authorization.
- Produces: RDS `PubliclyAccessible=false`, TCP 5432 source restricted to the application security group, 20 GiB allocation, 30 GiB maximum, and a successful SSM SHA redeploy.

- [ ] **Step 1: Verify private-path preconditions**

Compare the EC2 and RDS `VpcId` values from Task 1. Confirm they are identical. In **VPC > Security groups**, open the database security group and confirm an inbound PostgreSQL TCP 5432 rule can reference the application group `sg-0cf10a28c87a5434f`.

Expected: same VPC and a valid security-group reference. Stop before mutation if the VPCs differ.

- [ ] **Step 2: Obtain explicit security-group authorization**

Present the exact database group ID and every current inbound rule. If the rule is not already compliant, ask permission to retain only TCP 5432 from `sg-0cf10a28c87a5434f` for this database group. Do not alter shared groups that protect unrelated resources. If it is already the sole inbound rule, record the pass and skip Steps 2-4 without saving any security-group change.

- [ ] **Step 3: Restrict the database security group**

In **VPC > Security groups > database group > Edit inbound rules**, leave exactly:

| Type | Protocol | Port | Source |
|---|---|---:|---|
| PostgreSQL | TCP | 5432 | `sg-0cf10a28c87a5434f` |

Remove IPv4 CIDR, IPv6 CIDR, self-reference, and other-group rules for TCP 5432 only after confirming the database group is dedicated to this project. Save.

Expected: no public CIDR can reach PostgreSQL; application group still has TCP 80 from `0.0.0.0/0` and no TCP 22.

- [ ] **Step 4: Verify application health after the security-group edit**

Run in Session Manager:

```bash
curl -fsS http://localhost/actuator/health
```

Expected: `UP`. If it fails, restore the exact prior database ingress captured in Task 1, wait for propagation, recheck health, and stop the task.

- [ ] **Step 5: Obtain explicit RDS modification authorization**

If the live RDS state is not already compliant, state the exact changes: public access `No`, allocated storage unchanged at 20 GiB, autoscaling maximum set to 30 GiB, attached database security group unchanged. Obtain explicit approval. If all values already match, record the pass and skip Steps 5-7 without submitting the modify form.

- [ ] **Step 6: Apply private-access and storage settings**

Open **RDS > Databases > vehicle-maintenance-history-db > Modify**:

- under **Connectivity**, set **Public access** to **No**;
- keep only the dedicated database security group from Task 3;
- under **Storage**, keep allocated storage at **20 GiB**;
- keep storage autoscaling enabled and set maximum storage threshold to **30 GiB**;
- keep Single-AZ and `db.t4g.micro` unchanged;
- choose **Apply immediately**, review the summary, and confirm.

Expected: no engine, class, Multi-AZ, credential, backup, or deletion setting changes appear in the review summary.

- [ ] **Step 7: Wait for RDS and verify the final state**

Run in CloudShell:

```bash
aws rds wait db-instance-available \
  --region us-east-2 \
  --db-instance-identifier vehicle-maintenance-history-db

aws rds describe-db-instances \
  --region us-east-2 \
  --db-instance-identifier vehicle-maintenance-history-db \
  --query 'DBInstances[0].{Status:DBInstanceStatus,Public:PubliclyAccessible,AllocatedGiB:AllocatedStorage,MaxGiB:MaxAllocatedStorage,MultiAZ:MultiAZ,Groups:VpcSecurityGroups[*].VpcSecurityGroupId}' \
  --output json
```

Expected: `available`, `Public=false`, `AllocatedGiB=20`, `MaxGiB=30`, `MultiAZ=false`, and only the intended database group.

- [ ] **Step 8: Verify internal and public health**

Run in Session Manager:

```bash
curl -fsS http://localhost/actuator/health
```

Open the public health URL returned in Task 1.

Expected: both report `UP`. Do not add public PostgreSQL access if either check fails.

- [ ] **Step 9: Obtain authorization and rerun only the existing deploy job**

Explain that this proves OIDC, SSM, ECR pull, Parameter Store, private RDS, container replacement, and health without republishing the immutable SHA. Obtain explicit GitHub mutation approval.

Run locally:

```powershell
$VmhRunId = gh run list --repo mmetznerm/vehicle-maintenance-history --workflow ci-cd.yml --branch main --status success --limit 1 --json databaseId --jq '.[0].databaseId'
$VmhDeployJobId = gh run view $VmhRunId --repo mmetznerm/vehicle-maintenance-history --json jobs --jq '.jobs[] | select(.name == "Deploy to EC2 through SSM") | .databaseId' | Select-Object -Last 1
gh run rerun $VmhRunId --repo mmetznerm/vehicle-maintenance-history --job $VmhDeployJobId
gh run watch $VmhRunId --repo mmetznerm/vehicle-maintenance-history --exit-status --interval 10
```

Expected: only the selected deploy job is rerun; `Send deployment command`, `Poll deployment command`, and final health gate pass. If GitHub refuses to rerun the selected job, stop and defer the proof to the documentation PR merge in Task 10; do not rerun image publication against an existing immutable SHA.

---

### Task 4: Verify Cost Guardrails for Always-on Operation

**Files:**

- Verify or modify AWS Billing preferences and the existing AWS Budget.
- Do not create application resources or repository files in this task.

**Interfaces:**

- Consumes: user's zero-current-cost constraint, existing budget, EC2/RDS sizes, and Task 3 storage cap.
- Produces: confirmed notification path, scoped resource inventory, documented review cadence, and a stop condition for charges.

- [ ] **Step 1: Verify Free Tier usage alerts**

Open **Billing and Cost Management > Billing preferences > Alert preferences**. Confirm **Receive AWS Free Tier usage alerts** is enabled and the destination address is accessible by the user. If disabled, obtain explicit approval, enable it, and save.

Expected: Free Tier usage alerts are enabled. Do not expose the email address in repository evidence.

- [ ] **Step 2: Review the existing budget**

Open **Billing and Cost Management > Budgets**, select the budget already created, and verify:

- period is monthly;
- budget amount is no more than USD 1.00;
- notifications reach an address the user controls;
- actual-spend notifications exist before or at 50%, 80%, and 100%;
- a forecast notification exists at or before 100%.

If any condition is missing, obtain explicit approval and edit the existing budget rather than creating duplicates. Expected: one understandable portfolio budget with early warning; note that this is not a spending cap.

- [ ] **Step 3: Review current-month Billing and Free Tier usage**

Open **Bills**, **Free Tier**, and **Cost Explorer** for the current month. Review EC2, RDS, EBS, RDS storage and backup, public IPv4, ECR, data transfer, CloudWatch, and Systems Manager lines.

Expected: actual and forecast project cost remain covered by the account's current Free Plan/credits. If any unexplained charge or forecast exists, stop the plan before leaving the environment always-on and obtain user direction.

- [ ] **Step 4: Inventory project-cost multipliers with read-only commands**

Run in CloudShell:

```bash
aws ec2 describe-addresses --region us-east-2 --query 'Addresses[*].{PublicIp:PublicIp,AllocationId:AllocationId,InstanceId:InstanceId}' --output table
aws ec2 describe-nat-gateways --region us-east-2 --filter Name=state,Values=available,pending --query 'NatGateways[*].{Id:NatGatewayId,VpcId:VpcId,State:State}' --output table
aws elbv2 describe-load-balancers --region us-east-2 --query 'LoadBalancers[*].{Name:LoadBalancerName,Type:Type,State:State.Code}' --output table
aws ec2 describe-instances --region us-east-2 --filters 'Name=instance-state-name,Values=pending,running,stopping,stopped' --query 'Reservations[].Instances[].{Id:InstanceId,Name:Tags[?Key==`Name`]|[0].Value,State:State.Name,Type:InstanceType}' --output table
aws rds describe-db-instances --region us-east-2 --query 'DBInstances[*].{Id:DBInstanceIdentifier,Status:DBInstanceStatus,Class:DBInstanceClass,Storage:AllocatedStorage,MaxStorage:MaxAllocatedStorage}' --output table
aws rds describe-db-snapshots --region us-east-2 --snapshot-type manual --query 'DBSnapshots[*].{Id:DBSnapshotIdentifier,Database:DBInstanceIdentifier,GiB:AllocatedStorage,Created:SnapshotCreateTime}' --output table
aws ecr describe-repositories --region us-east-2 --query 'repositories[*].repositoryName' --output table
```

Expected for this project: no project Elastic IP, NAT Gateway, load balancer, extra EC2, extra RDS, or unnecessary manual snapshot. Never delete an unexpected resource from this inventory; identify ownership and request a separate exact deletion approval.

- [ ] **Step 5: Record the operating cadence for later documentation**

Record these approved statements for Tasks 6-8:

- review Billing and Free Tier weekly for four weeks after acceptance;
- review monthly afterward;
- stop EC2 and RDS when a budget alert, eligibility loss, exhausted credits, or unexplained charge appears;
- mark the live demo unavailable while stopped;
- remember that RDS automatically restarts after seven stopped days and storage/backup charges can remain.

Expected: the cadence is factual and does not promise permanent zero cost.

---

### Task 5: Execute Functional, Refresh, Persistence, and Screenshot Acceptance

**Files:**

- Create: `docs/images/portfolio/vehicles.png`
- Create: `docs/images/portfolio/maintenance-history.png`
- Optionally create: `docs/images/portfolio/login.png`
- Do not store the test password or browser tokens.

**Interfaces:**

- Consumes: Task 3 private healthy RDS, current public application URL, Session Manager access, and one disposable test identity.
- Produces: production CRUD/persistence evidence and sanitized UI images consumed by Tasks 6 and 8.

- [ ] **Step 1: Create a disposable test account through the UI**

Open the public `/register` route. Enter:

- full name: `Portfolio Reviewer`;
- email or phone: a unique disposable address under `example.invalid`, generated
  only for this acceptance run and never committed literally;
- password: a newly generated test-only value between 16 and 32 characters that is not used anywhere else.

Submit and confirm navigation to `/vehicles`. Keep the password only in the active password-manager/clipboard session; never paste it into chat, terminal, Git, or screenshots.

- [ ] **Step 2: Verify logout and login**

Use the application's logout control, confirm navigation to `/login`, then log in with the disposable identity. Expected: `/vehicles` loads without an authentication error.

- [ ] **Step 3: Verify the refresh endpoint without printing tokens**

In the browser developer console on the same origin, run exactly:

```javascript
void (async () => {
  const refreshToken = localStorage.getItem("vehicle-history.refreshToken");
  if (!refreshToken) throw new Error("refresh token is absent");
  const response = await fetch("/v1/auth/refresh", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });
  if (!response.ok) throw new Error(`refresh failed with ${response.status}`);
  const tokens = await response.json();
  localStorage.setItem("vehicle-history.accessToken", tokens.accessToken);
  localStorage.setItem("vehicle-history.refreshToken", tokens.refreshToken);
  console.log("refresh accepted", response.status);
})();
```

Expected: console prints only `refresh accepted 200`; no token value appears. Reload `/vehicles` and confirm the session remains authenticated. This is an acceptance probe, not a new frontend auto-refresh feature.

- [ ] **Step 4: Create and edit a transient vehicle**

Create a vehicle with plate `TEST2026`, brand `Toyota`, model `Corolla`, year `2021`, color `White`. Open it, edit brand to `Honda`, model to `Civic`, year to `2022`, color to `Blue`, save, and confirm every updated value. Then delete it and confirm it is absent from the list.

Expected: vehicle create/read/update/delete all succeed.

- [ ] **Step 5: Create the retained fictitious portfolio vehicle**

Create and retain:

- plate: `DEMO2026`;
- brand: `Toyota`;
- model: `Corolla`;
- year: `2022`;
- color: `Silver`.

Expected: the vehicle is visible in the list and details page.

- [ ] **Step 6: Verify maintenance create, update, and delete**

On `DEMO2026`, create a transient maintenance record dated `2026-08-22`, odometer `44000`, cost `150.00`, description `Tire rotation`. Edit it to odometer `44500`, cost `175.00`, description `Tire rotation and alignment`; confirm the updated values; delete it; confirm it is absent.

Expected: maintenance create/read/update/delete all succeed.

- [ ] **Step 7: Create the retained portfolio maintenance record**

Create and retain a record dated `2026-08-22`, odometer `45000`, cost `399.90`, description `Oil and filter change`.

Expected: the record appears in `DEMO2026` maintenance history.

- [ ] **Step 8: Capture sanitized application screenshots**

Before capture, ensure no email, password field, browser password prompt, token, developer console, AWS console, or unrelated tab is visible. Capture the application at a maximum of 1440 by 900 and save:

- vehicle list with `DEMO2026` as `docs/images/portfolio/vehicles.png`;
- vehicle detail and retained maintenance history as `docs/images/portfolio/maintenance-history.png`.

Capture `docs/images/portfolio/login.png` only if the login UI materially improves the README. Inspect each file visually before continuing.

- [ ] **Step 9: Verify persistence after container restart**

In Session Manager, run:

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

Expected: health succeeds within 12 attempts. Log in again if required and confirm `DEMO2026` plus `Oil and filter change` are still present.

- [ ] **Step 10: Confirm public health after restart**

Open the public health URL and the vehicle list. Expected: health is `UP`, the UI loads, and retained data remains. If persistence fails, do not create portfolio evidence; open a focused defect and stop.

---

### Task 6: Write Acceptance Evidence and the Trigger-based Roadmap

**Files:**

- Create: `docs/portfolio-acceptance.md`
- Create: `docs/portfolio-roadmap.md`
- Verify: `docs/images/portfolio/vehicles.png`
- Verify: `docs/images/portfolio/maintenance-history.png`

**Interfaces:**

- Consumes: Tasks 1-5 exact commit/tag/run facts, AWS closeout results, cost cadence, CRUD/persistence results, and sanitized screenshots.
- Produces: public acceptance record and roadmap linked by README and the AWS runbook.

- [ ] **Step 1: Write the production acceptance record**

Create `docs/portfolio-acceptance.md` with exactly these headings:

```markdown
# Portfolio Deployment Acceptance

## Scope and safety

## Release evidence

## Infrastructure closeout

## Functional acceptance

## Persistence acceptance

## Cost guardrails

## Known limitations

## Final evidence update
```

Populate each section with literal facts observed in Tasks 1-5. `Release evidence` must include the full `main` commit, short ECR SHA tag, and successful Actions URL. `Infrastructure closeout` must record only pass/fail facts for deleted master parameter, narrowed EC2 policy, private RDS, SG-to-SG TCP 5432, no TCP 22, encrypted EBS, 20/30 GiB storage, and health. `Functional acceptance` must list register, login, logout, refresh probe, vehicle CRUD, and maintenance CRUD. `Persistence acceptance` must list container restart now and leave the post-merge deployment row explicitly as `Pending final documentation merge` until Task 10. Do not include resource endpoints, credentials, tokens, parameter values, command output, or AWS console captures.

- [ ] **Step 2: Write the roadmap**

Create `docs/portfolio-roadmap.md` with these five sections and entry conditions copied from the approved spec:

```markdown
# Portfolio Infrastructure Roadmap

## Current baseline

## HTTPS and stable domain

## Observability and alerts

## Automatic rollback

## Terraform

## Possible ECS migration
```

For each future initiative state: why it is not needed now, the exact trigger from the spec, what the next design must compare, and the cost question that must be answered first. State explicitly that ECS is conditional on scaling/availability needs and is not automatically more current or appropriate than one EC2 instance.

- [ ] **Step 3: Verify the two documents contain no sensitive values or unresolved design markers**

Run:

```powershell
rg -n -i "password|access.?token|refresh.?token|secret|private.?key|rds.*endpoint|10\.[0-9]+\.[0-9]+\.[0-9]+" docs/portfolio-acceptance.md docs/portfolio-roadmap.md
rg -n -i "unfinished requirement|unresolved decision|write this later" docs/portfolio-acceptance.md docs/portfolio-roadmap.md
git diff --check
```

Expected: the first search finds only safety prose such as “no credentials were recorded,” never a value; the unresolved-marker search finds nothing; diff check exits zero. Manually inspect every match before staging.

- [ ] **Step 4: Visually verify screenshot safety and readability**

Open both PNGs with the local image viewer. Expected: only fictitious vehicle/maintenance content; no email, token, console, browser password prompt, AWS identifier, clipped main heading, or unreadable text.

- [ ] **Step 5: Commit evidence and roadmap**

Run:

```powershell
git add -- docs/portfolio-acceptance.md docs/portfolio-roadmap.md docs/images/portfolio/vehicles.png docs/images/portfolio/maintenance-history.png
git diff --cached --check
git diff --cached --name-only
git commit -m "docs: record portfolio acceptance and roadmap"
```

If `login.png` was deliberately created and reviewed, include that exact path in `git add`. Expected: staged names contain only these evidence artifacts; commit succeeds.

---

### Task 7: Close the AWS Operations Runbook

**Files:**

- Modify: `docs/aws-deployment.md:358-453`
- Read: `docs/portfolio-acceptance.md`
- Read: `docs/portfolio-roadmap.md`

**Interfaces:**

- Consumes: live closeout behavior from Tasks 2-4 and evidence paths from Task 6.
- Produces: one console-oriented post-bootstrap runbook that no longer leaves the master IAM permission behind and accurately describes always-on cost response.

- [ ] **Step 1: Strengthen section 10 bootstrap credential removal**

Update `## 10. Remover a credencial de bootstrap` so it requires, in order: `UP`, `vmh_app`, explicit deletion approval, deletion of only `/vmh/prod/rds-master-password`, removal of that parameter ARN from the EC2 inline policy, retention of `/vmh/prod/app-env`, and post-change health verification. Include the final one-parameter IAM JSON from Task 2. Explain the reset/new-temporary-parameter recovery path without including a password command or value.

- [ ] **Step 2: Make private RDS and storage checks explicit in section 12**

Update `## 12. Verificações de aceite` to require:

- `PubliclyAccessible=false`;
- only the dedicated database security group attached;
- PostgreSQL TCP 5432 source `vehicle-maintenance-history-app-sg`;
- application SG TCP 80 only and no TCP 22;
- 20 GiB allocated and 30 GiB maximum;
- master parameter absent and EC2 IAM policy not naming it;
- functional CRUD, refresh probe, restart persistence, and deployment persistence;
- link to `docs/portfolio-acceptance.md`.

- [ ] **Step 3: Add an always-on cost operations section after rollback**

Append `## 14. Operação contínua e custos` with the approved cadence: weekly for four weeks, monthly afterward, services to review, budget-as-alert warning, and stop condition. State that RDS automatically starts after seven stopped days and storage/backup can remain billable. Include the dynamic public-IP maintenance procedure: retrieve the current EC2 public IP, update README, and repeat public UI plus health checks.

Use this read-only command exactly:

```bash
aws ec2 describe-instances \
  --region us-east-2 \
  --instance-ids i-0aac27aeabf6e94c3 \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text
```

- [ ] **Step 4: Add roadmap and acceptance links to the runbook**

Link `portfolio-acceptance.md` from acceptance and `portfolio-roadmap.md` from the final operations section. Keep paths relative to `docs/aws-deployment.md` as `portfolio-acceptance.md` and `portfolio-roadmap.md`.

- [ ] **Step 5: Verify runbook truthfulness and hygiene**

Run:

```powershell
rg -n "rds-master-password|app-env|PubliclyAccessible|30 GiB|portfolio-acceptance|portfolio-roadmap|sete dias|weekly|semanal" docs/aws-deployment.md
rg -n "0\.0\.0\.0/0.*5432|AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY" docs/aws-deployment.md
git diff --check
```

Expected: required closeout terms and links exist; forbidden credential patterns and public PostgreSQL rule are absent; if `0.0.0.0/0` appears, it applies only to application TCP 80; diff check passes.

- [ ] **Step 6: Commit the runbook closeout**

Run:

```powershell
git add -- docs/aws-deployment.md
git diff --cached --check
git commit -m "docs: close AWS portfolio operations"
```

Expected: only the runbook is staged and committed.

---

### Task 8: Present the Live Deployment in README

**Files:**

- Modify: `README.md:8-128`
- Modify: `README.md:313-324`
- Read: `docs/portfolio-acceptance.md`
- Read: `docs/portfolio-roadmap.md`
- Read: `docs/images/portfolio/vehicles.png`
- Read: `docs/images/portfolio/maintenance-history.png`

**Interfaces:**

- Consumes: current public URL from Task 1, sanitized images from Task 5, accepted architecture, evidence, and roadmap.
- Produces: portfolio-first README with honest availability, security, cost, and architecture narrative.

- [ ] **Step 1: Add Live Demo directly after the project introduction**

Add `## Live Demo` after “REST API and frontend to manage vehicles and their maintenance history.” Link the exact public URL verified in Task 1. State in plain English:

- the demo is HTTP-only;
- use fictitious data and a unique test-only password;
- availability depends on AWS Free Tier/credits and the dynamic EC2 public IP;
- the current acceptance record is linked.

Do not publish shared credentials.

- [ ] **Step 2: Add the sanitized screenshots**

Add a compact `## Screenshots` section with relative Markdown image paths:

```markdown
![Vehicle list in the live portfolio application](docs/images/portfolio/vehicles.png)

![Vehicle maintenance history in the live portfolio application](docs/images/portfolio/maintenance-history.png)
```

Include `login.png` only if Task 5 created it and it improves the story.

- [ ] **Step 3: Refine AWS Deployment around the real production flow**

Keep the existing text diagram and explain this exact flow:

```text
Pull Request -> main -> GitHub Actions -> Docker Hub + ECR -> SSM -> EC2 -> private RDS
```

State that Docker Hub demonstrates delivery, ECR is the private runtime source, OIDC roles are split, SSM replaces SSH, SHA tags are immutable, EBS is encrypted, RDS is private, `vmh_app` is the application database role, and the master bootstrap parameter has been removed.

- [ ] **Step 4: Add acceptance and operational trade-offs**

Link `docs/portfolio-acceptance.md` and summarize the verified CRUD, container-restart persistence, private RDS connectivity, and SHA deploy. State current trade-offs: single EC2, Single-AZ RDS, HTTP, dynamic IPv4, test data only, manual rollback, and Free Tier-dependent availability.

- [ ] **Step 5: Replace Out Of Scope with a trigger-based roadmap link**

Rename `## Out Of Scope` to `## Roadmap and Deliberate Trade-offs`. Link `docs/portfolio-roadmap.md`. Keep HTTPS, observability, automatic rollback, Terraform, and possible ECS listed as future decisions, not promised features. State that ECS becomes relevant only for scaling/availability requirements.

- [ ] **Step 6: Verify README links, image paths, and claims**

Run:

```powershell
Test-Path -LiteralPath 'docs\images\portfolio\vehicles.png'
Test-Path -LiteralPath 'docs\images\portfolio\maintenance-history.png'
Test-Path -LiteralPath 'docs\portfolio-acceptance.md'
Test-Path -LiteralPath 'docs\portfolio-roadmap.md'
rg -n "Live Demo|portfolio-acceptance|portfolio-roadmap|Docker Hub \+ ECR|private RDS|HTTP|Free Tier|ECS" README.md
rg -n "AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|SPRING_DATASOURCE_PASSWORD=|JWT_SECRET=" README.md
git diff --check
```

Expected: every `Test-Path` is `True`; required narrative is present; credential-pattern search finds nothing; diff check passes. Open the README preview and click every new link.

- [ ] **Step 7: Commit the README presentation**

Run:

```powershell
git add -- README.md
git diff --cached --check
git commit -m "docs: present live AWS portfolio deployment"
```

Expected: only README is staged and committed.

---

### Task 9: Run the Complete Repository and Documentation Verification

**Files:**

- Verify: `README.md`
- Verify: `docs/aws-deployment.md`
- Verify: `docs/portfolio-acceptance.md`
- Verify: `docs/portfolio-roadmap.md`
- Verify: `docs/images/portfolio/*.png`
- Verify: `docs/superpowers/specs/2026-08-22-portfolio-production-readiness-design.md`
- Verify: `docs/superpowers/plans/2026-08-22-portfolio-production-readiness.md`
- Verify: existing `deploy/tests/*.sh` and `.github/workflows/*.yml` without modifying them.

**Interfaces:**

- Consumes: all local documentation commits and live acceptance evidence.
- Produces: fresh proof that the branch contains only intended non-sensitive documentation and preserves all deployment contracts.

- [ ] **Step 1: Run every deployment contract with Git Bash**

On Windows use the explicit Git Bash executable:

```powershell
& 'C:\Program Files\Git\usr\bin\bash.exe' -lc 'set -e; bash deploy/tests/compose_prod_test.sh; bash deploy/tests/deploy_test.sh; bash deploy/tests/setup_ec2_test.sh; bash deploy/tests/ssm_wait_test.sh; bash deploy/tests/workflow_contract_test.sh'
```

Expected: all suites print `PASS` and exit zero. Docker config access warnings are acceptable only if the tests still pass; other warnings require investigation.

- [ ] **Step 2: Verify file scope and text hygiene**

Run:

```powershell
git diff --check origin/main...HEAD
git diff --name-only origin/main...HEAD
git status --short
```

Expected: only the approved spec, plan, README, AWS runbook, acceptance, roadmap, and sanitized screenshot paths appear; no application, workflow, Compose, or deployment script changed; worktree is clean.

- [ ] **Step 3: Scan all changed text for credential material**

Run:

```powershell
$VmhChangedTextFiles = @(
  'README.md',
  'docs/aws-deployment.md',
  'docs/portfolio-acceptance.md',
  'docs/portfolio-roadmap.md',
  'docs/superpowers/specs/2026-08-22-portfolio-production-readiness-design.md',
  'docs/superpowers/plans/2026-08-22-portfolio-production-readiness.md'
)
rg -n "AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|SPRING_DATASOURCE_PASSWORD=|JWT_SECRET=|eyJ[A-Za-z0-9_-]+\.eyJ" $VmhChangedTextFiles
```

Expected: no match. The plan may name secret environment-variable keys in prose but must never contain `KEY=value` secret assignments.

- [ ] **Step 4: Verify all new relative links and images**

Run:

```powershell
$VmhRequiredPaths = @(
  'docs/aws-deployment.md',
  'docs/portfolio-acceptance.md',
  'docs/portfolio-roadmap.md',
  'docs/images/portfolio/vehicles.png',
  'docs/images/portfolio/maintenance-history.png'
)
$VmhMissingPaths = $VmhRequiredPaths | Where-Object { -not (Test-Path -LiteralPath $_) }
if ($VmhMissingPaths) { $VmhMissingPaths; exit 1 }
```

Expected: exit zero with no output.

- [ ] **Step 5: Review the complete branch diff**

Run:

```powershell
git diff --stat origin/main...HEAD
git diff origin/main...HEAD -- README.md docs/aws-deployment.md docs/portfolio-acceptance.md docs/portfolio-roadmap.md docs/superpowers/specs/2026-08-22-portfolio-production-readiness-design.md docs/superpowers/plans/2026-08-22-portfolio-production-readiness.md
```

Expected: no unsupported “production-grade,” “free forever,” HTTPS, stable-IP, automatic rollback, Terraform-managed, or ECS-runtime claim; no pending row except the explicitly named post-merge deployment evidence in `docs/portfolio-acceptance.md`.

- [ ] **Step 6: Request an independent read-only review**

Ask a reviewer to compare `origin/main...HEAD` with the approved spec. Critical or Important findings must be fixed; repeat affected checks. The reviewer must inspect screenshot safety as well as text.

---

### Task 10: Publish the Documentation PR and Prove the New Main SHA Deployment

**Files:**

- Publish the committed branch; do not edit files before the post-merge evidence step.
- Verify GitHub Actions and live application state.

**Interfaces:**

- Consumes: clean reviewed branch from Task 9 and explicit authorization for push, PR creation, readiness, and merge.
- Produces: merged documentation release, immutable image in Docker Hub/ECR, successful EC2 deploy, and persistence proof for the new `main` SHA.

- [ ] **Step 1: Invoke the required finishing and GitHub publication skills**

Before any remote write, read and follow `mobiai-create-pr`, `superpowers:finishing-a-development-branch`, and `github:github`. Recheck status, diff, remote, default branch, existing matching PRs, and authentication.

- [ ] **Step 2: Obtain explicit push and draft-PR authorization**

Summarize exact commits, files, verification evidence, and the fact that a merge triggers image publication and EC2 deployment. Ask permission to push `codex/portfolio-production-readiness` and create a draft PR targeting `main`.

- [ ] **Step 3: Push the branch and create the draft PR**

After approval, create a reviewed temporary PR body at
`$env:TEMP\vmh-portfolio-production-readiness-pr.md` using `apply_patch`. Give
it the headings `Summary`, `AWS closeout`, `Portfolio documentation`,
`Validation`, `Security`, and `Known limitations`. Then run:

```powershell
$VmhPrBodyPath = Join-Path $env:TEMP 'vmh-portfolio-production-readiness-pr.md'
git push -u origin codex/portfolio-production-readiness
gh pr create --repo mmetznerm/vehicle-maintenance-history --base main --head codex/portfolio-production-readiness --draft --title "docs: close and present AWS portfolio deployment" --body-file $VmhPrBodyPath
```

The body must summarize AWS closeout, acceptance, README/runbook/roadmap changes, test results, known HTTP/Free Tier limitations, and confirm no secret or application behavior change. The temporary body remains outside the repository and is never staged.

Expected: one draft PR targeting `main`; remote diff matches local paths exactly.

- [ ] **Step 4: Monitor PR checks and inspect the remote diff**

Run:

```powershell
gh pr checks --repo mmetznerm/vehicle-maintenance-history --watch --interval 10
gh pr diff --repo mmetznerm/vehicle-maintenance-history --name-only
```

Expected: every required check passes; remote names match Task 9 scope; GitGuardian or equivalent reports no secret.

- [ ] **Step 5: Obtain explicit ready-for-review and merge authorization**

Mark the PR ready only after the user approves. Merge only after a second explicit approval and all checks are green. Do not enable auto-merge implicitly.

- [ ] **Step 6: Monitor the main CI/CD run after merge**

Find and watch the run for the exact merge commit:

```powershell
git fetch origin main
$VmhMainSha = git rev-parse origin/main
$VmhMainRunId = gh run list --repo mmetznerm/vehicle-maintenance-history --workflow ci-cd.yml --branch main --event push --limit 10 --json databaseId,headSha --jq ".[] | select(.headSha == `"$VmhMainSha`") | .databaseId" | Select-Object -First 1
gh run watch $VmhMainRunId --repo mmetznerm/vehicle-maintenance-history --exit-status --interval 10
```

Expected: unit, integration, frontend, CodeQL, deployment contracts, Docker Hub/ECR publication, and SSM EC2 deployment pass. The deploy tag is `sha-` plus the first seven characters of `$VmhMainSha`.

- [ ] **Step 7: Verify post-deploy health and persistence**

In Session Manager, run:

```bash
sudo docker inspect --format '{{.Config.Image}}' vehicle-maintenance-history-app-1
curl -fsS http://localhost/actuator/health
```

Expected: image tag equals the merged main short SHA and health is `UP`. In the browser, log in with the disposable identity and confirm `DEMO2026` plus `Oil and filter change` survived the deployment. Record the merge commit, short tag, Actions run URL, and pass result for Task 11 without recording credentials.

---

### Task 11: Finalize and Publish the Post-merge Acceptance Evidence

**Files:**

- Modify: `docs/portfolio-acceptance.md`
- Do not modify screenshots, README, runbook, application, deployment, or workflow files.

**Interfaces:**

- Consumes: Task 10 exact merge commit, SHA tag, Actions URL, health, and retained-data result.
- Produces: final acceptance record with no pending checks and a small evidence-only follow-up PR.

- [ ] **Step 1: Start a fresh evidence branch from the accepted main**

After updating `origin/main`, create an isolated worktree or branch named `codex/portfolio-acceptance-evidence` according to `superpowers:using-git-worktrees`. Do not reuse a branch whose PR is already merged.

- [ ] **Step 2: Replace the pending deployment evidence with literal results**

Update `docs/portfolio-acceptance.md`:

- replace `Pending final documentation merge` with `Passed`;
- record the exact Task 10 full main commit;
- record its exact seven-character ECR tag;
- link the exact successful Actions run;
- state internal and public health were `UP`;
- state `DEMO2026` and `Oil and filter change` survived the deployment;
- preserve the warning that credentials, tokens, endpoints, and private data are absent.

- [ ] **Step 3: Verify the evidence-only diff**

Run:

```powershell
git diff --check
git diff --name-only
rg -n "Pending|AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|SPRING_DATASOURCE_PASSWORD=|JWT_SECRET=|eyJ[A-Za-z0-9_-]+\.eyJ" docs/portfolio-acceptance.md
```

Expected: only `docs/portfolio-acceptance.md` changed; no `Pending` or sensitive pattern appears.

- [ ] **Step 4: Commit the final evidence**

Run:

```powershell
git add -- docs/portfolio-acceptance.md
git diff --cached --check
git commit -m "docs: finalize AWS portfolio acceptance"
```

Expected: one evidence-only commit.

- [ ] **Step 5: Obtain authorization and publish the evidence PR**

Follow `mobiai-create-pr` and `github:github`; ask before push and PR creation. Create a draft PR targeting `main`, verify the one-file remote diff, monitor checks, then obtain separate ready and merge approvals.

- [ ] **Step 6: Perform final completion checks**

After merge, confirm:

- `docs/portfolio-acceptance.md` on `main` has no pending row;
- README live link still matches the current EC2 IPv4;
- latest GitHub Actions run is green;
- public and internal health remain `UP`;
- `/vmh/prod/rds-master-password` remains absent;
- RDS remains private with 20/30 GiB storage;
- Billing shows no unexpected actual or forecast cost.

Expected: every acceptance criterion in the spec is satisfied. If the evidence-only merge triggers another SHA deployment, verify its health; the acceptance record may continue to reference the first fully accepted documentation merge to avoid an infinite evidence-update cycle.

---

## Plan Completion Review

Before execution, compare every spec workstream and acceptance criterion with the tasks above:

- Workstreams 1-4: Tasks 1-4.
- Workstream 5: Tasks 5, 10, and 11.
- Workstreams 6-7: Tasks 6-8.
- Workstream 8: Task 6 roadmap only.
- Repository verification: Task 9.
- Remote release and final evidence: Tasks 10-11.

No application behavior, new AWS service, paid dependency, infrastructure-as-code system, HTTPS endpoint, monitoring pipeline, automatic rollback, or ECS migration is part of this plan.
