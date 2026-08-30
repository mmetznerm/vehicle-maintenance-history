# ADR 0001: Stateless JWT Authentication and Refresh Token Flow

## Status
Accepted

## Context
The Spring Boot backend serves both a React web application and a Kotlin Multiplatform mobile application. Traditional session-based authentication requires server-side session state, complicating horizontal scalability and mobile client integration.

## Decision
We implemented stateless **JSON Web Token (JWT)** authentication using Spring Security:

1. **Short-Lived Access Tokens:** Signed JWT tokens carrying user identity and claims, validated on every incoming HTTP request by a custom Security Filter.
2. **Refresh Tokens:** Stored securely to issue new access tokens without requiring the user to re-enter credentials repeatedly.
3. **Stateless Security Filter Chain:** Spring Security configured with `SessionCreationPolicy.STATELESS`.

## Consequences

### Positive
- Completely stateless backend architecture ready for horizontal scaling behind load balancers.
- Universal authentication mechanism for Web, Android, and iOS clients.

### Negative
- Requires token revocation strategies (token blacklist or refresh token invalidation in DB) upon user logout.
