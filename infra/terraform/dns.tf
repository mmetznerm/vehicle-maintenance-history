locals {
  manage_acm_dns_validation = var.enable_acm_certificate && (var.enable_route53_zone || var.route53_zone_id != null)
  selected_route53_zone_id  = var.route53_zone_id != null ? var.route53_zone_id : try(aws_route53_zone.demo[0].zone_id, null)
}

resource "aws_route53_zone" "demo" {
  count = var.enable_route53_zone ? 1 : 0

  name = var.demo_root_domain

  tags = {
    Name = var.demo_root_domain
  }
}

resource "aws_acm_certificate" "demo" {
  count = var.enable_acm_certificate ? 1 : 0

  domain_name               = var.demo_frontend_host
  subject_alternative_names = [var.demo_backend_host]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "${var.deployment_name}-${var.environment}-demo"
  }
}

resource "aws_route53_record" "demo_certificate_validation" {
  for_each = local.manage_acm_dns_validation ? {
    for option in aws_acm_certificate.demo[0].domain_validation_options : option.domain_name => {
      name   = option.resource_record_name
      record = option.resource_record_value
      type   = option.resource_record_type
    }
  } : {}

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = local.selected_route53_zone_id
}

resource "aws_acm_certificate_validation" "demo" {
  count = local.manage_acm_dns_validation ? 1 : 0

  certificate_arn         = aws_acm_certificate.demo[0].arn
  validation_record_fqdns = [for record in aws_route53_record.demo_certificate_validation : record.fqdn]
}
