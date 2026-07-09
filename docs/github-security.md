# GitHub Security Setup

This project uses repository-level security automation plus GitHub security
features that must be enabled in the repository settings.

## Workflows

- `CodeQL`: static analysis for Java and TypeScript.
- `Security / Dependency Review`: blocks pull requests that introduce vulnerable
  dependencies at `moderate` severity or higher.
- `Security / Trivy Repository Scan`: scans the repository for dependency and
  configuration vulnerabilities at `HIGH` and `CRITICAL` severity.

## Dependabot

Dependabot is configured for:

- Maven dependencies in `/backend`.
- npm dependencies in `/frontend`.
- GitHub Actions in `/`.
- Docker base images in `/backend`.

## GitHub Settings

Enable these settings in:

```text
Settings > Code security and analysis
```

- Dependency graph.
- Dependabot alerts.
- Dependabot security updates.
- Code scanning.
- Secret scanning.
- Push protection.

After the new workflows run once, add these checks to the `main` branch
protection rule if you want security gates to be required before merge:

```text
CodeQL (java-kotlin)
CodeQL (javascript-typescript)
Dependency Review
Trivy Repository Scan
```
