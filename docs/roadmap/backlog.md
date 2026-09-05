# Backlog

## P0 — Core

### VMH-001 — Authentication

- VMH-001.01 Review authentication flow
- VMH-001.02 Harden refresh token flow

### VMH-002 — Vehicle Management

- VMH-002.01 Vehicle pagination
- VMH-002.02 Vehicle filtering

### VMH-003 — Maintenance Management

- VMH-003.01 Maintenance pagination
- VMH-003.02 Maintenance filtering

### VMH-004 — Offline Synchronization

- VMH-004.01 Define synchronization contract
- VMH-004.02 Backend sync endpoint
- VMH-004.03 Server version metadata
- VMH-004.04 KMP sync worker
- VMH-004.05 Process Outbox operations
- VMH-004.06 Sync retry/backoff
- VMH-004.07 Sync conflict handling

## P1 — Vehicle Records

### VMH-005 — Images & Documents

- VMH-005.01 Attachment model
- VMH-005.02 Storage abstraction
- VMH-005.03 Local storage
- VMH-005.04 Image upload API
- VMH-005.05 Mobile image upload
- VMH-005.06 Web image upload

### VMH-006 — Mileage

- VMH-006.01 Mileage model
- VMH-006.02 Mileage API
- VMH-006.03 Mileage mobile flow
- VMH-006.04 Mileage web flow

### VMH-007 — Maintenance Reminders

- VMH-007.01 Reminder model
- VMH-007.02 Reminder calculation
- VMH-007.03 Reminder API
- VMH-007.04 Reminder UI

## P1 — Insights

### VMH-008 — Dashboard & Analytics

- VMH-008.01 Vehicle dashboard
- VMH-008.02 Maintenance timeline
- VMH-008.03 Cost summary
- VMH-008.04 Mileage charts

### VMH-009 — Notifications

- VMH-009.01 Notification abstraction
- VMH-009.02 Maintenance notifications
- VMH-009.03 Document expiration notifications

## P0 — Production

### VMH-010 — Production Hardening

- VMH-010.01 API security review
- VMH-010.02 Rate limiting
- VMH-010.03 Error response standardization
- VMH-010.04 Observability improvements
- VMH-010.05 Audit logging
- VMH-010.06 Backup/restore strategy

### VMH-011 — AWS Deployment

- VMH-011.01 Review AWS architecture
- VMH-011.02 Production secrets
- VMH-011.03 S3 integration
- VMH-011.04 Production deployment
- VMH-011.05 Monitoring

## P2 — Product Polish

### VMH-012 — UX / Product Polish

This epic should contain UX improvements discovered during implementation.

Do not add speculative features before the core product is stable.