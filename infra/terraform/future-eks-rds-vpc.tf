# Future Terraform modules/resources intentionally left out of the first foundation PR:
#
# - VPC/networking for public and private subnets across availability zones.
# - EKS cluster, managed node groups/Fargate profiles, access entries and add-ons.
# - AWS Load Balancer Controller IAM role and Helm installation path.
# - RDS PostgreSQL instances, subnet groups, parameter groups and security groups.
# - Route 53 records and ACM certificates for stage/prod hosts.
#
# Keeping these as documented placeholders makes the first infrastructure change small
# and reviewable. Add each area in a dedicated PR after the target AWS account,
# networking model and cost boundaries are approved.
