# Future Terraform modules/resources intentionally left out of the first foundation PR:
#
# - NAT gateways, private egress routing and VPC endpoints for production-grade private subnet egress.
# - EKS logging, private endpoint hardening, managed add-on versions and Fargate profiles.
# - AWS Load Balancer Controller IAM role and Helm installation path using the optional EKS OIDC provider.
# - RDS parameter groups, enhanced monitoring, production sizing and separate stage/prod database strategy.
# - Route 53 records and ACM certificates for stage/prod hosts.
#
# Keeping these as documented placeholders makes the first infrastructure change small
# and reviewable. Add each area in a dedicated PR after the target AWS account,
# networking model and cost boundaries are approved.
