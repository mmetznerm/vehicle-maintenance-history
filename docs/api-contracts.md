# Contratos HTTP da Vehicle History API

## Autenticação

- Rotas `/v1/auth/**`, health, métricas e OpenAPI são públicas.
- Demais rotas exigem `Authorization: Bearer <access-token>`.
- O access token JWT identifica o usuário pelo `sub`, expira em 15 minutos e não é armazenado no servidor.
- O refresh token é opaco, armazenado somente como hash, rotacionado no refresh e revogado no logout. Sua validade padrão é 30 dias.
- Não existem papéis administrativos neste estágio; toda leitura e escrita de veículos/manutenções é isolada pelo usuário autenticado.

## Erros

Todos os erros HTTP usam o mesmo envelope:

```json
{
  "timestamp": "2026-06-28T17:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "REQUEST_VALIDATION_FAILED",
  "message": "Request validation failed",
  "path": "/v1/vehicles",
  "fieldErrors": [
    {"field": "plate", "message": "must not be blank"}
  ]
}
```

Principais códigos estáveis:

| HTTP | Códigos |
| --- | --- |
| `400` | `REQUEST_VALIDATION_FAILED`, `MALFORMED_REQUEST_BODY` |
| `401` | `INVALID_CREDENTIALS`, `INVALID_REFRESH_TOKEN`, `UNAUTHENTICATED` |
| `403` | `ACCESS_DENIED` |
| `404` | `USER_NOT_FOUND`, `VEHICLE_NOT_FOUND`, `MAINTENANCE_NOT_FOUND` |
| `409` | `DATA_INTEGRITY_CONFLICT`, `USER_ALREADY_REGISTERED`, `VEHICLE_ALREADY_REGISTERED`, `VEHICLE_HAS_MAINTENANCE_HISTORY`, `MAINTENANCE_ID_ALREADY_USED` |
| `500` | `UNEXPECTED_SERVER_ERROR` |

Clientes devem reagir ao campo `code`, não ao texto de `message`.

## Paginação de manutenções

`GET /v1/vehicles/{plate}/maintenances` aceita:

- `page`: índice iniciado em zero, default `0`;
- `size`: entre `1` e `100`, default `20`;
- `sortBy`: campo permitido pelo serviço, default `date`;
- `direction`: `asc` ou `desc`, default `desc`.

Resposta:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

## Idempotência de manutenção

`POST /v1/vehicles/{plate}/maintenances` aceita `id` UUID opcional gerado pelo cliente:

- sem `id`, o servidor cria um UUID;
- com `id` novo, a manutenção é criada e o identificador é preservado;
- repetindo o mesmo `id` para o mesmo veículo, a API retorna a manutenção já existente sem duplicar o histórico;
- reutilizando o `id` em outro veículo, a API retorna `409 MAINTENANCE_ID_ALREADY_USED`.

O replay devolve o estado persistido originalmente; o corpo repetido não atualiza a manutenção. Alterações usam `PUT /{maintenanceId}`.

## CORS e request ID

- CORS é fechado por padrão.
- Origens permitidas são configuradas por `app.security.cors.allowed-origins` ou `APP_SECURITY_CORS_ALLOWED_ORIGINS`.
- O header `X-Request-Id` pode ser enviado pelo cliente e é devolvido pela API; quando ausente, a aplicação gera um identificador.
