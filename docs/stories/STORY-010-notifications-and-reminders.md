# STORY-010 — Notifications and Reminders

## Status
`[TODO]`

## Objetivo

Criar um sistema de lembretes e notificações (Reminders) para alertar o usuário sobre trocas de óleo iminentes, vencimento de IPVA/licenciamento, renovação de seguro e revisões recomendadas.

## Problema que resolve

Mesmo visualizando o painel de manutenção preventiva, os usuários precisam ser proativamente notificados antes que prazos críticos expirem, evitando multas de trânsito por licenciamento vencido ou danos mecânicos graves ao motor por atraso na troca de óleo.

## Escopo

### Backend
- Criar o módulo `reminder` em `com.mmetzner.vmh.reminder`.
- Entidade `Reminder`:
  - `id` (UUID)
  - `userId` (UUID do usuário)
  - `vehicleId` (UUID do veículo)
  - `title` (Título do lembrete, ex: "Troca de Óleo Próxima")
  - `message` (Mensagem descritiva, ex: "Faltam 500 km ou 10 dias para a troca de óleo do Corolla")
  - `targetDate` (Data prevista de vencimento)
  - `targetMileage` (Quilometragem prevista de vencimento)
  - `type` Enum (`PREVENTIVE_MAINTENANCE`, `LICENSE_TAX`, `INSURANCE_RENEWAL`, `CUSTOM`)
  - `status` Enum (`ACTIVE`, `DISMISSED`, `COMPLETED`)
  - `createdAt`, `updatedAt`
- Serviço gerador de lembretes que processa periodicamente os veículos dos usuários e gera/atualiza registros de lembretes.
- Endpoints REST em `/api/v1/reminders`:
  - `GET /api/v1/reminders`: Lista lembretes ativos do usuário.
  - `PATCH /api/v1/reminders/{id}/dismiss`: Marca um lembrete como dispensado/lido.
  - `POST /api/v1/reminders`: Permite cadastrar um lembrete personalizado (ex: calibrar pneus a cada 15 dias).
- Migration Flyway `V12__create_reminders.sql`.

### Web
- Central de notificações/lembretes no cabeçalho do sistema Web com indicador numérico de pendências.
- Modal e lista de lembretes com ação de dispensar (`Dismiss`) ou marcar como concluído (`Complete`).

### Mobile
- Integração com Notificações Locais Push nativas:
  - Android: `NotificationManager` e canais de notificação Android.
  - iOS: `UNUserNotificationCenter`.
- Agendamento local de notificações no dispositivo mobile a partir dos lembretes recebidos do backend.
- Notificações são exibidas na barra de status do smartphone mesmo com o aplicativo fechado.

## Fora do escopo

- Integração com Firebase Cloud Messaging (FCM) ou Apple Push Notification service (APNs) remoto via servidores de terceiros (focar em notificações geradas via REST / Notificações Locais no dispositivo).

## Dependências

- [STORY-009 — Preventive Maintenance Rules](STORY-009-preventive-maintenance-rules.md)

## Critérios de aceite

- Lembretes automáticos são gerados para manutenções preventivas com status `DUE_SOON` e `OVERDUE`.
- O usuário consegue criar lembretes personalizados com data limite.
- A API permite consultar e alterar o estado do lembrete (`ACTIVE`, `DISMISSED`, `COMPLETED`).
- A interface Web exibe os lembretes ativos no menu superior.
- O aplicativo Mobile dispara notificações push locais nativas nos momentos adequados em Android e iOS.

## Considerações arquiteturais

- O sistema de lembretes deve ser extensível para no futuro permitir o envio por e-mail ou WhatsApp sem alterar o domínio principal.

## Testes esperados

- Testes unitários do gerador de lembretes e ciclo de vida de status.
- Testes de integração da API REST de lembretes com Testcontainers.
- Testes de UI Web.
- Testes unitários das abstrações de notificação local no mobile.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
