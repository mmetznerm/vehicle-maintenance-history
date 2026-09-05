# STORY-006 — KMP Offline Sync

## Status
`[TODO]`

## Objetivo

Implementar a sincronização real offline-first no aplicativo Kotlin Multiplatform (Android e iOS), integrando a persistência Room local, a fila Outbox, o cliente HTTP Ktor e o agendamento em segundo plano.

## Problema que resolve

Atualmente o aplicativo insere registros na tabela de Outbox do Room local, mas falta o motor de sincronização completo que consome a fila do Outbox, trata reconexão de rede, executa retries com backoff exponencial, atualiza os status visuais na UI e dispara a sincronização em segundo plano nativamente no Android e iOS.

## Escopo

### Backend
- N/A (Consome a API desenvolvida na STORY-005).

### Web
- N/A.

### Mobile (Kotlin Multiplatform)
- Implementar o motor `OutboxSyncEngine` em `composeApp/.../core/sync`:
  - Varredura de operações com `SyncStatus.PENDING` ou `SyncStatus.FAILED` elegíveis para retry.
  - Montagem de lotes `SyncPushRequestDto` e envio via Ktor HTTP client.
  - Atualização dos registros no Room local conforme resposta da API (`PENDING` -> `SYNCED` ou remoção da fila de outbox).
  - Tratamento de retries com backoff exponencial (1s, 2s, 4s, 8s, máx 5 tentativas).
- Monitor de Conectividade Nativo (`NetworkConnectivityObserver`):
  - Android: `ConnectivityManager.NetworkCallback`.
  - iOS: `NWPathMonitor`.
  - Disparo automático da sincronização assim que a conexão com a internet for reestabelecida.
- Execução em Segundo Plano Nativa:
  - Android: Agendador com `WorkManager` (`OutboxSyncWorker`).
  - iOS: Integração com `BGTaskScheduler` / background fetch.
- Indicadores Visuais na Interface Compose Multiplatform:
  - Ícone/badge de estado de sincronização no topo da tela (Sincronizado, Pendente, Sincronizando, Falha).
  - Capacidade de forçar sincronização manual via gesto "Pull to Refresh" ou botão de ação.

## Fora do escopo

- Sincronização peer-to-peer entre dispositivos via Bluetooth/Wi-Fi Direct.

## Dependências

- [STORY-005 — Backend Sync API](STORY-005-backend-sync-api.md)

## Critérios de aceite

- O usuário consegue criar, editar e excluir veículos e manutenções totalmente offline.
- As alterações são persistidas no Room local e enfileiradas no Outbox com status `PENDING`.
- Ao recuperar a conexão com a internet, o aplicativo envia automaticamente as pendências ao backend sem intervenção do usuário.
- Se o aplicativo for fechado e reaberto, as pendências mantêm-se salvas no banco SQLite local e são enviadas na próxima sincronização.
- Nenhuma duplicação de dados ocorre durante retries ou reconexões simultâneas.
- Testes unitários do motor de sincronização cobrem cenários offline, sucesso e falha com Ktor Mock Engine.

## Considerações arquiteturais

- O motor de sincronização deve ser thread-safe, utilizando Kotlin Coroutines com `Mutex` para evitar execuções concorrentes duplicadas do ciclo de sync.
- Manter abstração nativa via `expect`/`actual` para os monitores de rede e workers de background.

## Testes esperados

- Testes unitários do `OutboxSyncEngine` utilizando `kotlin.test`, Coroutines Test e Ktor `MockEngine`.
- Testes de mapeamento entre entidades Room, DTOs e modelos de domínio KMP.
- Validação manual em emuladores Android e simuladores iOS desligando/ligando o Wi-Fi.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
