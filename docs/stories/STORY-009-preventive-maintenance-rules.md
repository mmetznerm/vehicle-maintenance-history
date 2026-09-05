# STORY-009 — Preventive Maintenance Rules

## Status
`[TODO]`

## Objetivo

Implementar um motor de regras de manutenção preventiva centralizado no backend, capaz de calcular prazos e quilometragens para revisões futuras com base em padrões de intervalo de tempo e distância rodada.

## Problema que resolve

Proprietários frequentemente se esquecem de trocar o óleo do motor, filtro de ar, fluido de freio ou correia dentada nos prazos recomendados pelos fabricantes. O sistema deve calcular automaticamente quando a próxima revisão de cada item deve ser realizada e alertar sobre itens vencidos ou próximos do vencimento.

## Escopo

### Backend
- Criar o motor de regras de manutenção preventiva em `com.mmetzner.vmh.maintenance.preventive`:
  - Modelo de Regra de Manutenção (`PreventiveRule`):
    - `ruleId` / `name` (ex: Troca de Óleo do Motor, Troca do Filtro de Ar, Troca de Fluido de Freio, Troca de Correia Dentada, Rodízio de Pneus)
    - `intervalKm` (Intervalo recomendado em KM, ex: 10.000 km)
    - `intervalMonths` (Intervalo recomendado em meses, ex: 12 meses)
    - `description`
  - Algoritmo de Cálculo de Status por Veículo:
    - O backend analisa a última manutenção realizada para cada regra/categoria no veículo.
    - Calcula: `nextDueMileage`, `nextDueDate`, `remainingKm`, `remainingDays`.
    - Determina o Status:
      - `OK`: Manutenção em dia (mais de 1.000 km ou mais de 30 dias restantes).
      - `DUE_SOON`: Próxima do vencimento (menos de 1.000 km ou menos de 30 dias restantes).
      - `OVERDUE`: Vencida (hodômetro atual ultrapassou a KM limite ou data atual ultrapassou a data limite).
- Migration Flyway `V11__preventive_rules.sql` para tabela de regras customizáveis e vínculos por veículo.
- Endpoints REST em `/api/v1/vehicles/{id}/preventive-status`:
  - Retorna a lista de itens preventivos com status calculado e estimativas de vencimento.

### Web
- Adicionar aba "Manutenção Preventiva" na página do veículo.
- Cartões de saúde do veículo com codificação por cores (Verde = OK, Amarelo = Próxima, Vermelho = Vencida).
- Indicador visual de progresso (barras de percentual de vida útil restante do óleo, filtros, freios).

### Mobile
- Exibir a saúde preventiva do veículo no app mobile.
- Preparar os dados locais no Room para agendamento de lembretes locais (conectado com a STORY-010).

## Fora do escopo

- Envio de e-mails ou mensagens de SMS via provedores pagos externos.

## Dependências

- [STORY-003 — Maintenance Domain v2](STORY-003-maintenance-domain-v2.md)

## Critérios de aceite

- O backend calcula corretamente a próxima manutenção com base na menor margem entre intervalo de KM e tempo em meses.
- Itens sem manutenção registrada utilizam a data de compra e hodômetro inicial do veículo como referência.
- A API retorna o status consolidado (`OK`, `DUE_SOON`, `OVERDUE`) para cada item preventivo.
- Interface Web exibe as barras de saúde preventiva do veículo de forma intuitiva.
- Interface Mobile apresenta os status e prazos de manutenção em dia.

## Considerações arquiteturais

- A lógica de cálculo de prazos deve residir 100% no backend (ou compartilhada em modelo puramente declarativo) para evitar divergências nos critérios de vencimento entre Web e Mobile.

## Testes esperados

- Testes unitários exaustivos do motor de cálculo de regras com variadas combinações de datas, hodômetros e intervalos.
- Testes de integração do endpoint `/preventive-status` com Testcontainers.
- Testes de componentes Web e Mobile.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
