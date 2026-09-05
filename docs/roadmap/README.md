# Vehicle Maintenance History — Roadmap

## Purpose

This document defines the product and engineering roadmap for Vehicle
Maintenance History.

The project is developed as a full-stack platform composed of:

- Spring Boot backend
- React web application
- Kotlin Multiplatform mobile application
- PostgreSQL
- AWS production infrastructure

## Development Strategy

The preferred development strategy is vertical slicing.

Whenever possible, a feature should be implemented end-to-end:

Backend → Database → API → Web → Mobile → Offline/Sync → Tests

Avoid building large layers in isolation without validating the complete user
workflow.

## Product Roadmap

### Phase 1 — Foundation

- Authentication hardening
- Vehicle management
- Maintenance management

### Phase 2 — Offline First

- Synchronization contract
- Backend synchronization
- Mobile synchronization worker
- Outbox processing
- Retry/backoff
- Conflict handling

### Phase 3 — Vehicle Records

- Images
- Documents
- Mileage tracking

### Phase 4 — Automation

- Maintenance reminders
- Notifications

### Phase 5 — Insights

- Vehicle dashboard
- Maintenance timeline
- Cost analysis
- Mileage charts

### Phase 6 — Production

- Security hardening
- Observability
- Audit logging
- Backup/restore
- AWS deployment
- Production monitoring

## Priority

P0 — Required for product functionality

P1 — Important product capability

P2 — Improvement / optimization

## Task IDs

Tasks use the format:

VMH-XXX.YY

Example:

VMH-004.02 — Backend Sync Endpoint