# Future Terraform modules/resources intentionally left out of this networking PR:
#
# - EKS cluster, managed node groups/Fargate profiles, access entries and add-ons.
# - AWS Load Balancer Controller IAM role and Helm installation path.
# - RDS PostgreSQL instance, subnet group and parameter group.
# - Route 53 records and ACM certificates for demo hosts.
#
# Keeping these as documented placeholders makes each infrastructure change small
# and reviewable. Add each area in a dedicated PR after the target AWS account
# and cost boundaries are approved.
