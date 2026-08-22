# Portfolio Infrastructure Roadmap

## Current baseline

The current workload is intentionally small: GitHub Actions publishes to Docker Hub and private ECR, then deploys one immutable SHA-tagged image to one EC2 instance through Systems Manager. The application connects to a private Single-AZ RDS PostgreSQL database. This baseline is sufficient for a low-traffic portfolio demo and keeps architecture and operating cost understandable.

## HTTPS and stable domain

This is not required while the public endpoint is limited to fictitious test data and the owner is avoiding domain, DNS, and managed load-balancer costs.

- Trigger: the owner accepts domain and DNS cost and wants a stable public URL, or intends to use anything beyond fictitious test credentials.
- Next design: compare a small reverse proxy with automated certificates on the existing EC2 instance against an AWS-managed load balancer and certificate.
- Cost question: what recurring domain, DNS, public IPv4, certificate-supporting infrastructure, and load-balancer charges would each option introduce?

## Observability and alerts

Centralized paid ingestion is not needed while bounded Docker logs and Systems Manager are sufficient for occasional portfolio troubleshooting.

- Trigger: the demo receives recurring traffic, failures cannot be diagnosed from Systems Manager and bounded Docker logs, or an availability target is introduced.
- Next design: compare the minimum useful CloudWatch metrics, logs, alarms, retention, and notification channels with the current on-instance diagnostics.
- Cost question: what monthly ingestion, retention, metric, alarm, and notification ceiling must be enforced before enabling the design?

## Automatic rollback

Manual rollback is simpler while there are few accepted releases and recovery time is not yet a formal target.

- Trigger: multiple accepted SHA releases exist and recovery time becomes more important than the simplicity of manual rollback.
- Next design: preserve the previously healthy immutable tag, roll back only after a bounded health failure, verify restored health, and retain complete diagnostics without exposing secrets.
- Cost question: can rollback remain inside the existing GitHub Actions, SSM, EC2, and ECR footprint, or would added orchestration and monitoring create recurring charges?

## Terraform

Infrastructure as code is not needed solely to make a one-environment portfolio look more complex; the current console-built environment is documented and stable.

- Trigger: a second environment is required, the environment must be recreated, or infrastructure changes become frequent enough that console drift is material.
- Next design: inventory and import existing resources, define remote state and locking, keep credentials out of state, and migrate one resource class at a time without accidental replacement.
- Cost question: what will state storage, locking, CI execution, drift detection, and any replacement resources cost?

## Possible ECS migration

ECS is conditional, not automatically more current or more appropriate than one EC2 instance for this workload. The current EC2 deployment remains the better fit while simplicity and Free Tier-conscious operation are the primary constraints.

- Trigger: the application needs horizontal scaling, managed scheduling, multiple instances, zero-downtime capacity replacement, or availability that one EC2 instance cannot provide.
- Next design: create an architecture decision record comparing the current EC2 deployment with ECS on EC2 and ECS Fargate, including load balancing, networking, observability, deployment behavior, and operations.
- Cost question: what are the complete monthly costs for compute, public IPv4, load balancing, logs, image transfer, storage, and idle capacity in each alternative?
