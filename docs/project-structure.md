# Project Structure

Vehicle History is organized as a Spring Boot backend with a Vite frontend in the same
repository.

## Root Folders

```text
backend/    Spring Boot backend source code.
docs/       Project documentation.
frontend/   React + TypeScript + Vite app.
```

Generated folders such as `backend/target/`, `frontend/node_modules/`,
`frontend/dist/`, and `backend/src/main/resources/static/` should stay hidden
from Git.

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
