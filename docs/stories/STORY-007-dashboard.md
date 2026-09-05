# STORY-007 — Dashboard

## Status
`[TODO]`

## Objetivo

Criar um painel de controle (Dashboard) completo e interativo no Backend, Web e Mobile, fornecendo visão geral resumida dos veículos do usuário, métricas de custos acumulados, histórico recente e alertas de manutenções futuras.

## Problema que resolve

Atualmente a aplicação possui apenas telas de listagem CRUD simples. Os usuários não têm um ponto central para acompanhar resumos financeiros (quanto gastaram este mês/ano), verificar rapidamente se há manutenções vencidas ou visualizar atalhos para ações frequentes.

## Escopo

### Backend
- Criar o módulo `dashboard` em `com.mmetzner.vmh.dashboard`.
- Endpoint REST em `GET /api/v1/dashboard`:
  - Retorna a DTO consolidada `DashboardSummaryResponse`:
    - `totalVehicles` (Quantidade de veículos cadastrados)
    - `totalMaintenances` (Quantidade total de manutenções registradas)
    - `totalSpentCurrentYear` (Gasto total acumulado no ano vigente)
    - `totalSpentAllTime` (Gasto total acumulado histórico)
    - `recentMaintenances` (Lista das 5 manutenções mais recentes)
    - `upcomingMaintenances` (Lista de manutenções com data ou KM agendada próxima)
    - `alertsCount` (Quantidade de manutenções ou garantias com atenção pendente)
- O backend deve executar consultas de agregação otimizadas no PostgreSQL (Spring Data JPA / JPQL / Native Queries), evitando carregar todas as entidades para memória.

### Web
- Reformular a página inicial Web (`frontend/src/pages/DashboardPage.tsx`).
- Adicionar cartões de métricas (KPI Cards) responsivos com estatísticas de gastos e contadores.
- Adicionar seção de atalhos rápidos ("Cadastrar Veículo", "Registrar Manutenção").
- Adicionar lista visual das últimas manutenções realizadas e alertas em destaque.

### Mobile
- Tela principal do app redesenhada (`DashboardScreen.kt`).
- Carrossel de resumo dos veículos do usuário.
- Painel de métricas financeiras simplificado e adaptado à tela mobile.
- Lista rápida com suporte a carregamento offline a partir dos dados do Room local.

## Fora do escopo

- Exportação de dados do dashboard em Excel/CSV (coberto parcialmente pela STORY-012).
- Gráficos interativos complexos de telemetria avançada.

## Dependências

- [STORY-003 — Maintenance Domain v2](STORY-003-maintenance-domain-v2.md)

## Critérios de aceite

- O endpoint `GET /api/v1/dashboard` responde em tempo ágil com os dados agregados do usuário autenticado.
- O cálculo dos valores totais gastos e contadores reflete a soma exata dos dados do banco.
- O painel Web exibe cartões KPI, manutenções recentes e alertas de forma responsiva.
- O aplicativo Mobile exibe a tela inicial adaptada ao design mobile com dados locais e sincronizados.
- Usuários sem veículos cadastrados visualizam um estado inicial amigável (Empty State) incentivando o cadastro.

## Considerações arquiteturais

- O backend deve ser a única fonte da verdade para o cálculo das agregações financeiras e estatísticas.
- Manter caching adequado ou consultas SQL agregadas diretas (`SUM`, `COUNT`, `MAX`) com índices nas colunas de data e `owner_id`.

## Testes esperados

- Testes unitários do serviço e agregador do `Dashboard`.
- Testes de integração do endpoint `/api/v1/dashboard` com Testcontainers.
- Testes de componentes frontend React Vitest.
- Testes de telas Compose no mobile KMP.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
