# API Contracts

## Standard error response

All API errors follow this structure:

```json
{
  "timestamp": "2026-07-03T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "REQUEST_VALIDATION_FAILED",
  "message": "Request validation failed.",
  "path": "/v1/example",
  "fieldErrors": [
    {
      "field": "example",
      "message": "must not be blank"
    }
  ]
}