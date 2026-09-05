# STORY-013 — Vehicle External Data

## Status
`[TODO]`

## Objetivo

Integrar a aplicação a provedores externos de dados automotivos para permitir a consulta e o preenchimento automático de dados de veículos a partir da placa ou número do chassi (VIN), além de recuperar especificações técnicas e recomendações de fábrica.

## Problema que resolve

Digitar manualmente a marca, modelo, ano de fabricação, versão e especificações de um veículo durante o cadastro é sujeito a erros e consome tempo do usuário. Uma consulta por placa ou VIN acelera o cadastro e garante precisão nos dados do veículo.

## Escopo

### Backend
- Criar a abstração de integração externa em `com.mmetzner.vmh.vehicle.external`:
  ```java
  public interface VehicleDataProvider {
      VehicleLookupResult lookupByPlate(String plate);
      VehicleLookupResult lookupByVin(String vin);
  }
  ```
- Implementações:
  - `MockVehicleDataProvider`: Provedor padrão para ambiente de desenvolvimento local e testes (retorna dados fictícios determinísticos sem custos).
  - Provedor Concreto Configurável (ex: API Tabela FIPE, NHTSA VIN Decoder API, CarQuery API ou API de Placas nacional): Ativado quando credenciais de API estiverem presentes.
- Cache e Resiliência:
  - Tabela `vehicle_external_cache` via Flyway `V14__external_data_cache.sql` para armazenar consultas já realizadas e evitar chamadas repetidas a APIs pagas/limitadas.
  - Circuit Breaker / Fallback com Resilience4j: se o provedor externo falhar ou estiver indisponível, a aplicação responde normalmente sem interromper a criação manual do veículo.
- Endpoints REST em `/api/v1/vehicles/external-lookup`:
  - `GET /api/v1/vehicles/external-lookup?plate={placa}`
  - `GET /api/v1/vehicles/external-lookup?vin={vin}`

### Web
- Botão "Buscar Dados por Placa" no formulário de cadastro de veículos.
- Preenchimento automático instantâneo dos campos (marca, modelo, ano, versão) ao digitar uma placa válida.

### Mobile
- Funcionalidade de autocompletar dados do veículo via placa/VIN na tela Compose de cadastro.
- Suporte a fallback inteligente caso o aplicativo esteja sem conexão com a internet.

## Fora do escopo

- Assinatura de serviços de API pagos caros para produção sem autorização prévia.
- Leitura de placas via câmera por visão computacional (LPR).

## Dependências

- [STORY-002 — Vehicle Domain v2](STORY-002-vehicle-domain-v2.md)

## Critérios de aceite

- A busca por placa ou VIN válida retorna os dados estruturados do veículo.
- Em ambiente de desenvolvimento local, o `MockVehicleDataProvider` funciona 100% sem requisições para a internet ou chaves de API pagas.
- Dados retornados são armazenados no cache da aplicação (PostgreSQL) para evitar custos e chamadas redundantes.
- Falhas de rede ou timeout na API externa não quebram o cadastro manual de veículos.
- Web e Mobile suportam a funcionalidade de preenchimento automático.

## Considerações arquiteturais

- O domínio da aplicação deve estar completamente desacoplado do provedor externo escolhido. Toda comunicação deve ocorrer exclusivamente através da interface `VehicleDataProvider`.
- Documentar custos, limites de taxa (Rate Limits) e políticas de privacidade das APIs externas em `docs/adr/`.

## Testes esperados

- Testes unitários do `MockVehicleDataProvider` e gerenciador de cache.
- Testes unitários das políticas de fallback/circuit breaker.
- Testes de integração do endpoint `/external-lookup`.
- Testes de UI Web e Mobile.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
