# Portfolio Deployment Acceptance

Acceptance date: 2026-08-22 (`America/Sao_Paulo`).

## Scope and safety

This acceptance covers the public portfolio deployment in AWS `us-east-2` and the delivery path from `main` through GitHub Actions, Docker Hub, ECR, Systems Manager, EC2, and a private RDS PostgreSQL instance. All functional checks used fictitious data and a disposable test identity. No credentials, token values, parameter contents, or database addresses were recorded.

## Release evidence

- Accepted `main` commit: `abc031638f007c294dc87fe87f17896518c19cce`.
- Runtime image: immutable ECR tag `sha-abc0316`.
- Delivery workflow: [successful GitHub Actions run 32536766421](https://github.com/mmetznerm/vehicle-maintenance-history/actions/runs/32536766421).
- The running container reported the accepted SHA-tagged ECR image.

## Infrastructure closeout

- Pass — the temporary RDS master credential parameter was deleted after application connectivity was healthy.
- Pass — the EC2 inline policy can read only the retained application environment parameter; the deleted bootstrap parameter is no longer named.
- Pass — RDS is private and has only the dedicated database security group attached.
- Pass — PostgreSQL TCP 5432 accepts traffic from the application security group, not from a public CIDR.
- Pass — the application security group exposes TCP 80 and has no TCP 22 ingress.
- Pass — the EC2 root EBS volume is encrypted.
- Pass — RDS uses 20 GiB allocated storage with a 30 GiB autoscaling maximum.
- Pass — internal and public application health reported `UP`.

## Functional acceptance

- Pass — registered a disposable user through the public UI.
- Pass — signed out and signed back in with the disposable identity.
- Pass — production login and refresh probes both returned HTTP 200 and returned a complete token pair without printing token values.
- Pass — vehicle create, read, update, and delete were exercised with transient fictitious data.
- Pass — maintenance create, read, update, and delete were exercised with transient fictitious data.
- Pass — the retained fictitious vehicle `DEMO2026` and its retained maintenance record are visible in the live UI.

## Persistence acceptance

- Pass — the application container was restarted through AWS Systems Manager and returned to healthy state.
- Pass — `DEMO2026` and `Oil and filter change` remained available after the container restart, confirming RDS-backed persistence.
- Post-merge deployment: Pending final documentation merge.

## Cost guardrails

- The account currently uses the AWS Free Plan and promotional credits; this is not a guarantee of permanent zero cost.
- Free Tier usage alerts are enabled.
- The monthly USD 1 budget has email notifications at 50%, 80%, and 100% of actual cost, plus 100% of forecast cost.
- At acceptance time there was one EC2 instance, one RDS instance, no NAT Gateway, no load balancer, no Elastic IP, and no manual RDS snapshot.
- Any detected charge is a stop condition: stop EC2 and RDS, mark the demo unavailable, inspect Billing, and resume only after understanding the charge.

## Known limitations

- The demo uses HTTP and a dynamic public IPv4 address.
- Compute is a single EC2 instance and the database is Single-AZ.
- Availability depends on AWS Free Tier eligibility and remaining credits.
- The environment is for fictitious portfolio data only.
- Rollback is manual and uses a previously accepted immutable SHA tag.

## Final evidence update

After this documentation is merged, record the final `main` commit, successful workflow run, deployed SHA tag, public health result, and post-deployment persistence result in an evidence-only follow-up change.
