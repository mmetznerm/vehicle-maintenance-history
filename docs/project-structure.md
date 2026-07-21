# Project Structure

AutoLog is organized as three Spring Boot applications with a Vite frontend in the
same repository. Keeping the producer, consumer and event contracts together
makes the portfolio example runnable and reviewable as one coherent system.

## Root Folders

```text
backend/       Transactional API and outbox producer.
event-worker/  Kafka consumer and public read model.
consistency-worker/ Kafka consumer, rule engine and alert outbox producer.
contracts/     Versioned event contracts and examples.
docs/          Project documentation.
frontend/      React + TypeScript + Vite app.
```

Generated folders such as `backend/target/`, `event-worker/target/`, `consistency-worker/target/`,
`frontend/node_modules/`, `frontend/dist/`, and
`backend/src/main/resources/static/` should stay hidden from Git.

## Backend

Backend code uses feature-first packages:

```text
backend/src/main/java/com/mmetzner/vmh/
  auth/
  vehicle/
  maintenance/
  shared/
```

Each feature follows the same internal structure when relevant:

```text
domain/
application/
infrastructure/
presentation/
```

This structure is already good for the current project and should be kept.

## Event Worker

The worker owns its projection database and is organized by responsibility:

```text
event-worker/src/main/java/com/mmetzner/vmh/history/
  event/       Kafka envelope, listener and idempotent processor.
  projection/  JPA read-model entities and repositories.
  api/         Sanitized public HTTP contract.
  config/      CORS and application configuration.
```

The worker must not query the backend database. Kafka events are its only source
of vehicle and maintenance state, which keeps the service boundary explicit.

## Consistency Worker

The consistency worker consumes the same source topic with an independent group
and owns both its snapshot and alert outbox:

```text
consistency-worker/src/main/java/com/mmetzner/vmh/consistency/
  detection/   Deterministic rules and active-alert reconciliation.
  event/       Kafka listener and idempotent source-event processor.
  outbox/      Transactional alert event publication.
  projection/  Worker-owned vehicle and maintenance snapshots.
  config/      Output topic configuration.
```

The backend's `inconsistency/` package is a separate private read model. It does
not invoke the worker directly and learns alert transitions only from Kafka.

## Frontend

Frontend code uses route and responsibility folders:

```text
components/   Reusable UI pieces.
pages/        Screen-level components.
services/     API and storage access.
styles/       Global CSS and visual tokens.
types/        Shared TypeScript contracts.
```

As the frontend grows, prefer adding feature folders only when a feature has
multiple files that belong together. For now, the current shape is simpler and
easier to scan.
