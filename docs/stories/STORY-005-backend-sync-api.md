# STORY-005 — Backend Sync API

## Status
`[TODO]`

## Objetivo

Criar uma API REST dedicada e robusta no backend para suportar a sincronização bidirecional, previsível e idempotente com clientes local-first (Mobile KMP).

## Problema que resolve

O aplicativo mobile opera em modo offline-first, acumulando alterações locais no Outbox Pattern. Enviar requisições CRUD padrão individualmente sem suporte a lote, controle de versão ou identificadores estáveis pode causar falhas sob conectividade instável, duplicação de dados e dificuldades na resolução de conflitos.

## Escopo

### Backend
- Projetar o módulo de sincronização `sync` em `com.mmetzner.vmh.sync`.
- Implementar endpoints de sincronização em `/api/v1/sync`:
  - `POST /api/v1/sync/push`: Recebe um lote de operações do outbox cliente (`SyncPushRequest`).
  - `GET /api/v1/sync/pull?since={timestamp}`: Retorna alterações ocorridas no servidor desde o último timestamp de sincronização do cliente.
- Mecanismos de Idempotência e Identificadores Estáveis:
  - Garantir suporte a IDs gerados no cliente (UUID v4) para `Vehicle`, `Maintenance` e `Expense`.
  - Controle de duplicidade: se um registro com o mesmo `id` já existir, avaliar timestamps (`clientUpdatedAt` vs `serverUpdatedAt`) para decidir se é uma atualização válida ou operação repetida (idempotente).
- Tratamento de Estados de Sincronização e Resposta:
  - Estrutura de resposta `SyncPushResponse` contendo o status por operação:
    - `ACCEPTED`: Operação processada e aplicada com sucesso.
    - `IGNORED`: Operação duplicada ou obsoleta ignorada com sucesso (idempotente).
    - `CONFLICT`: Conflito detectado (resolvido por regra Server-Wins ou Last-Write-Wins).
    - `FAILED`: Erro de validação ou processamento com mensagem explicativa.
- Adicionar colunas `client_updated_at` e `server_updated_at` / `version` nas tabelas principais via Flyway `V9__sync_support.sql`.

### Web
- N/A (A interface Web consome a API REST diretamente online).

### Mobile
- Projetar DTOs de sincronização no KMP (`SyncPushRequestDto`, `SyncPushResponseDto`, `SyncItemDto`).
- Atualizar a abstração do cliente Ktor para consumir os novos endpoints de push e pull.

## Fora do escopo

- Resolução complexa de conflitos campo a campo usando CRDTs.
- Sincronização em tempo real via WebSockets (focar em sincronização REST baseada em timestamps).

## Dependências

- [STORY-003 — Maintenance Domain v2](STORY-003-maintenance-domain-v2.md)

## Critérios de aceite

- O endpoint `POST /api/v1/sync/push` aceita lotes de operações do outbox contendo criações, edições e exclusões.
- Reenviar o mesmo lote múltiplas vezes produz o mesmo resultado no servidor sem duplicar registros no banco.
- O endpoint `GET /api/v1/sync/pull` retorna apenas registros modificados após o timestamp informado.
- Respostas da API contêm mapeamento detalhado do status de cada operação.
- Testes cobrem cenários de payload repetido, concorrência e dados inválidos.

## Considerações arquiteturais

- Operações de sincronização em lote devem ser executadas dentro de transações de banco de dados isoladas ou com política de falha parcial granular por item.
- Garantir alto desempenho nos índices por `owner_id` e `updated_at`.

## Testes esperados

- Testes de unidade das regras de idempotência e resolução de conflitos.
- Testes de integração enviando lotes de sincronização simulados com Testcontainers.
- Testes de concorrência garantindo consistência sob requisições simultâneas.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
