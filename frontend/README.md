# AutoLog Frontend

React + TypeScript + Vite frontend for AutoLog.

## Folder Guide

```text
src/
  components/   Reusable UI components.
  pages/        Route-level screens.
  services/     API clients and browser storage helpers.
  styles/       Global styles and design tokens.
  types/        Shared TypeScript types.
```

## Scripts

```bash
npm.cmd run dev
npm.cmd run build
npm.cmd run lint
```

`npm.cmd run build` writes the production frontend into:

```text
../backend/src/main/resources/static
```

Spring Boot serves that build from `http://localhost:8080`.
