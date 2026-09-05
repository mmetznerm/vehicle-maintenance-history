# STORY-008 — Vehicle Expenses

## Status
`[TODO]`

## Objetivo

Expandir a plataforma para registrar e controlar custos gerais do veículo além da manutenção, como combustível, IPVA/impostos, seguro, pedágio, estacionamento e financiamento, permitindo o cálculo do custo total de propriedade (TCO) e custo por quilômetro (custo/km).

## Problema que resolve

Manutenção é apenas uma fração do custo total de manter um veículo. Proprietários necessitam controlar gastos com abastecimento, impostos anuais e seguros para entender o impacto financeiro real do veículo e calcular métricas como custo mensal e custo por quilômetro rodado.

## Escopo

### Backend
- Criar o módulo `expense` em `com.mmetzner.vmh.expense`.
- Entidade `Expense`:
  - `id` (UUID)
  - `vehicleId` (UUID do veículo)
  - `category` Enum (`MAINTENANCE`, `FUEL`, `INSURANCE`, `TAX`, `INSPECTION`, `PARTS`, `PARKING`, `TOLL`, `OTHER`)
  - `expenseDate` (Data da despesa)
  - `amount` (Valor da despesa, `numeric(12,2)`)
  - `odometer` (Hodômetro / KM opcional no momento do gasto, ex: para abastecimento)
  - `description` (Descrição ou nome do estabelecimento)
  - `notes` (Observações)
- Migration Flyway `V10__create_expenses.sql`.
- Endpoints REST em `/api/v1/expenses`:
  - CRUD completo para despesas.
  - Endpoint de métricas `/api/v1/expenses/metrics?vehicleId={id}`:
    - Retorna `costPerMonth`, `costPerYear`, `costPerKm` (`totalSpent / currentMileage`), e divisão por categoria.

### Web
- Nova seção "Despesas" na navegação e na página de detalhes do veículo.
- Formulário de lançamento rápido de despesas com categorias visuais.
- Gráfico e cartões de resumo por categoria de custo e cálculo automático de custo/km.

### Mobile
- Criar `ExpenseEntity` no Room local.
- Tela de formulário de despesa (`ExpenseFormScreen`) e aba de despesas na visualização do veículo.
- Suporte a lançamentos offline integrados ao Outbox Pattern.

## Fora do escopo

- Integração com bombas de combustível via IoT ou leitura de QRCode de cupons fiscais.

## Dependências

- [STORY-002 — Vehicle Domain v2](STORY-002-vehicle-domain-v2.md)

## Critérios de aceite

- Despesas gerais podem ser cadastradas, editadas, listadas e removidas por veículo.
- As categorias cobrem combustível, impostos, seguros e gastos diversos.
- O cálculo das métricas de custo por mês, custo por ano e custo por quilômetro é executado com precisão pelo backend.
- As despesas do tipo `MAINTENANCE` são automaticamente integradas ou contabilizadas em conjunto nas métricas financeiras.
- Web e Mobile suportam o cadastro e exibição das despesas.

## Considerações arquiteturais

- O cálculo de `costPerKm` deve lidar com segurança em cenários onde a quilometragem atual é zero ou não informada, evitando erros de divisão por zero.

## Testes esperados

- Testes unitários do cálculo de métricas de despesas.
- Testes de integração CRUD do repositório e controller de despesas.
- Testes frontend Vitest para o formulário e relatórios de despesas.
- Testes mobile Room DAO e sincronização de despesas.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
