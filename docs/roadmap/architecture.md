# Architecture Roadmap

## System

```text
                    ┌──────────────────────┐
                    │      Web React       │
                    └──────────┬───────────┘
                               │
                               │ REST
                               │
┌──────────────────┐           ▼
│  KMP Android/iOS │──────► Spring Boot API
└────────┬─────────┘           │
         │                     │
         │ Local-first         │
         ▼                     ▼
    Room / SQLite          PostgreSQL
         │
         │ Outbox
         │
         └──────────── Sync ────────────► API