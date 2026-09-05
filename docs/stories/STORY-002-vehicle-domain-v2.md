# STORY-002 — Vehicle Domain v2

## Status
`[TODO]`

## Objetivo

Evoluir o modelo de dados e o domínio de veículo (`Vehicle`) para uma representação completa e realista, incluindo atributos essenciais para identificação, acompanhamento de quilometragem e detalhes de aquisição.

## Problema que resolve

A entidade atual de veículo possui um escopo mínimo (`brand`, `model`, `manufactureYear`, `color`, `plate`). Para suportar funcionalidades avançadas como controle de despesas, histórico de valorização, validação por chassi (VIN) e previsões de manutenção, o domínio precisa capturar informações mais abrangentes.

## Escopo

### Backend
- Evoluir o modelo de domínio `Vehicle` em `com.mmetzner.vmh.vehicle.domain`:
  - `brand` (Marca, ex: Toyota)
  - `model` (Modelo, ex: Corolla)
  - `version` (Versão/Trim, ex: XEi 2.0 Flex)
  - `manufactureYear` (Ano de fabricação)
  - `modelYear` (Ano do modelo)
  - `plate` (Placa com validação de formato Mercosul / Padrão antigo)
  - `vin` (Número de chassi / VIN com 17 caracteres)
  - `currentMileage` (Quilometragem atual acumulada)
  - `purchaseDate` (Data de compra do veículo)
  - `color` (Cor do veículo)
  - `notes` (Observações gerais)
- Criar a migration Flyway `V6__expand_vehicle_domain.sql` para adicionar as novas colunas na tabela `vehicles` de forma compatível.
- Atualizar DTOs (`VehicleCreateRequest`, `VehicleUpdateRequest`, `VehicleResponse`), Mappers e Endpoints da API REST (`/api/v1/vehicles`).
- Adicionar validações Spring Validation (`@NotBlank`, `@Pattern`, `@Min`, etc.).

### Web
- Atualizar formulários de cadastro e edição de veículos em `frontend/src/pages/VehicleForm.tsx`.
- Adicionar máscaras de entrada e validações de cliente para placa, VIN e quilometragem.
- Atualizar cartões de veículo e página de detalhes (`frontend/src/pages/VehicleDetails.tsx`) para exibir os novos atributos.

### Mobile
- Atualizar a entidade Room `VehicleEntity` no repositório KMP (`composeApp/.../core/database`).
- Atualizar modelos de domínio KMP, mapeadores DTO e telas Compose Multiplatform (`VehicleFormScreen`, `VehicleDetailScreen`).

## Fora do escopo

- Consulta externa automática de placa/VIN (coberto na STORY-013).
- Upload de foto do veículo (coberto na STORY-004).

## Dependências

- [STORY-001 — Separar claramente CI de AWS/CD](STORY-001-ci-cd-separation.md)

## Critérios de aceite

- Criação e edição de veículos aceitam e persistem todos os novos campos.
- Placa e VIN possuem validação apropriada.
- O campo `currentMileage` é atualizado e reflete a quilometragem atual do veículo.
- As migrations executam com sucesso no PostgreSQL via Flyway e nos testes de integração.
- Clientes Web e Mobile exibem e manipulam os novos campos corretamente.

## Considerações arquiteturais

- Manter retrocompatibilidade de API onde aplicável.
- Garantir índices apropriados no banco de dados para buscas por `plate` e `vin`.

## Testes esperados

- Testes unitários do repositório, serviço e controller de `Vehicle`.
- Testes de integração do Spring Boot com Testcontainers validando as novas colunas.
- Testes unitários do frontend React com Vitest.
- Testes unitários dos mappers e Room DAO no mobile KMP.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
