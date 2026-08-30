# Security Policy

## Security & Static Analysis

This repository enforces automated security checks on every pull request and push to the `main` branch:

- **CodeQL Security Scanning:** Static Application Security Testing (SAST) for Java/Kotlin backend and JavaScript/TypeScript frontend.
- **Dependency Reviews:** Automatic scanning of project dependencies for known vulnerabilities.
- **Passwordless Cloud Deployments:** AWS deployments utilize GitHub Actions OIDC role assumption, eliminating long-lived cloud credentials in repository secrets.

## Reporting a Vulnerability

If you discover a security vulnerability within this project, please do not open a public issue. Send an email or private report detailing:

1. Type of issue (e.g., buffer overflow, SQL injection, unauthorized access, cross-site scripting).
2. Step-by-step instructions to reproduce the issue.
3. Affected versions or endpoints.

We will acknowledge your report within 48 hours and provide updates on resolution progress.
