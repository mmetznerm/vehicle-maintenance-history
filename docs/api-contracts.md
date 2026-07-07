# API Contracts

Base URL:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

## Authentication

Protected endpoints require:

```http
Authorization: Bearer <accessToken>
```

## Auth

### Register

```http
POST /v1/auth/register
```

Request:

```json
{
  "fullName": "Maycon Metzner",
  "emailOrPhone": "maycon@email.com",
  "password": "StrongPassword123"
}
```

Response `201`:

```json
{
  "accessToken": "jwt",
  "refreshToken": "refresh-token"
}
```

### Login

```http
POST /v1/auth/login
```

Request:

```json
{
  "emailOrPhone": "maycon@email.com",
  "password": "StrongPassword123"
}
```

Response `200`:

```json
{
  "accessToken": "jwt",
  "refreshToken": "refresh-token"
}
```

### Refresh

```http
POST /v1/auth/refresh
```

Request:

```json
{
  "refreshToken": "refresh-token"
}
```

Response `200`:

```json
{
  "accessToken": "new-jwt",
  "refreshToken": "new-refresh-token"
}
```

### Logout

```http
POST /v1/auth/logout
```

Response:

```text
204 No Content
```

## Vehicles

### Create vehicle

```http
POST /v1/vehicles
```

Request:

```json
{
  "plate": "abc-1234",
  "brand": "Honda",
  "model": "Civic",
  "manufactureYear": 2020,
  "color": "Silver"
}
```

Response `201`:

```json
{
  "id": "vehicle-id",
  "plate": "ABC1234",
  "brand": "Honda",
  "model": "Civic",
  "manufactureYear": 2020,
  "color": "Silver",
  "createdAt": "2026-07-07T10:15:30Z",
  "updatedAt": "2026-07-07T10:15:30Z"
}
```

### List vehicles

```http
GET /v1/vehicles
```

Response `200`:

```json
[
  {
    "id": "vehicle-id",
    "plate": "ABC1234",
    "brand": "Honda",
    "model": "Civic",
    "manufactureYear": 2020,
    "color": "Silver"
  }
]
```

### Find vehicle

```http
GET /v1/vehicles/{vehicleId}
```

### Update vehicle

```http
PUT /v1/vehicles/{vehicleId}
```

### Delete vehicle

```http
DELETE /v1/vehicles/{vehicleId}
```

Response:

```text
204 No Content
```

## Maintenances

### Create maintenance

```http
POST /v1/vehicles/{vehicleId}/maintenances
```

Request:

```json
{
  "maintenanceDate": "2026-07-07",
  "odometer": 35000,
  "description": "Oil change",
  "cost": 250.00
}
```

Response `201`:

```json
{
  "id": "maintenance-id",
  "vehicleId": "vehicle-id",
  "maintenanceDate": "2026-07-07",
  "odometer": 35000,
  "description": "Oil change",
  "cost": 250.00,
  "createdAt": "2026-07-07T10:15:30Z",
  "updatedAt": "2026-07-07T10:15:30Z"
}
```

### List maintenances

```http
GET /v1/vehicles/{vehicleId}/maintenances
```

### Find maintenance

```http
GET /v1/vehicles/{vehicleId}/maintenances/{maintenanceId}
```

### Update maintenance

```http
PUT /v1/vehicles/{vehicleId}/maintenances/{maintenanceId}
```

### Delete maintenance

```http
DELETE /v1/vehicles/{vehicleId}/maintenances/{maintenanceId}
```

Response:

```text
204 No Content
```

## Error response

Standard format:

```json
{
  "timestamp": "2026-07-07T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "code": "REQUEST_VALIDATION_FAILED",
  "message": "Request validation failed.",
  "path": "/v1/vehicles",
  "fieldErrors": [
    {
      "field": "plate",
      "message": "must not be blank"
    }
  ]
}
```

Common codes:

| HTTP | Code |
|---|---|
| 400 | `REQUEST_VALIDATION_FAILED` |
| 400 | `MALFORMED_REQUEST_BODY` |
| 401 | `UNAUTHENTICATED` |
| 403 | `ACCESS_DENIED` |
| 404 | `USER_NOT_FOUND` |
| 404 | `VEHICLE_NOT_FOUND` |
| 404 | `MAINTENANCE_NOT_FOUND` |
| 409 | `USER_ALREADY_REGISTERED` |
| 409 | `VEHICLE_ALREADY_REGISTERED` |
| 500 | `UNEXPECTED_SERVER_ERROR` |