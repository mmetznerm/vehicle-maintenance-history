# STORY-003 — Maintenance Domain v2

## Status
`[TODO]`

## Objetivo

Transformar a entidade de Manutenção em um registro detalhado e completo, dividindo custos de peças e mão de obra, definindo categorias, informações de oficina/mecanico, dados de garantia e alertas para a próxima manutenção.

## Problema que resolve

Atualmente a manutenção registra apenas descrição, data, hodômetro e um valor de custo único. Na prática, proprietários precisam saber a categoria do serviço (preventiva, corretiva, funilaria, pneus), separar quanto pagaram em peças vs mão de obra, acompanhar prazos de garantia e agendar a próxima revisão.

## Escopo

### Backend
- Evoluir o modelo de domínio `Maintenance` em `com.mmetzner.vmh.maintenance.domain`:
  - `category` Enum (`PREVENTIVE`, `CORRECTIVE`, `INSPECTION`, `EMERGENCY`, `TIRES`, `BODYWORK`, `OTHER`)
  - `maintenanceDate` (Data de realização)
  - `odometer` (Hodômetro / KM no momento da manutenção)
  - `description` (Descrição detalhada do serviço)
  - `laborCost` (Custo de mão de obra)
  - `partsCost` (Custo de peças e insumos)
  - `totalCost` (Custo total calculado: `laborCost + partsCost`, com suporte a ajuste se fornecido)
  - `workshopName` (Nome da oficina / concessionária)
  - `mechanicName` (Nome do mecânico responsável)
  - `warrantyUntilDate` (Data de término da garantia)
  - `warrantyUntilMileage` (KM de término da garantia)
  - `nextMaintenanceDate` (Data sugerida para a próxima revisão)
  - `nextMaintenanceMileage` (KM sugerida para a próxima revisão)
  - `notes` (Observações adicionais)
- Criar a migration Flyway `V7__expand_maintenance_domain.sql`.
- Atualizar DTOs, Mappers, repositórios e endpoints REST (`/api/v1/maintenances`).
- Regra de negócio: se o hodômetro da manutenção for maior que a `currentMileage` do veículo, atualizar a `currentMileage` do veículo automaticamente.

### Web
- Atualizar o formulário de cadastro/edição em `frontend/src/pages/MaintenanceForm.tsx`.
- Seletor de categorias com ícones visuais.
- Campos separados para peças e mão de obra com cálculo automático do total na interface.
- Exibição de insígnias de garantia ativa ou vencida na lista de manutenções.

### Mobile
- Atualizar `MaintenanceEntity` no Room local.
- Atualizar telas Compose (`MaintenanceFormScreen`, `MaintenanceDetailScreen`).
- Suporte a formulário offline com cálculo local de custos.

## Fora do escopo

- Anexo de notas fiscais em PDF ou fotos da manutenção (coberto na STORY-004).
- Motor automatizado de regras preventivas globais (coberto na STORY-009).

## Dependências

- [STORY-002 — Vehicle Domain v2](STORY-002-vehicle-domain-v2.md)

## Critérios de aceite

- Manutenções podem ser cadastradas e consultadas com todas as novas informações.
- Custo total é consistente e validado (`totalCost >= 0`).
- Atualização do hodômetro reflete na quilometragem atual do veículo associado.
- Consultas por categoria e filtro por intervalo de datas/quilometragem funcionam na API.
- Web e Mobile apresentam os novos campos e respeitam o cálculo de custos.

## Considerações arquiteturais

- O cálculo do `totalCost` deve ser centralizado no domínio do backend para evitar inconsistências entre Web e Mobile.

## Testes esperados

- Testes de unidade das regras de domínio (cálculo de total, atualização de hodômetro do veículo).
- Testes de integração de repositório e controller com Testcontainers.
- Testes de componentes frontend React.
- Testes de mapeadores e DAOs no KMP.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
