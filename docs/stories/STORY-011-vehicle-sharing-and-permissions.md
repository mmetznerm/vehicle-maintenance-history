# STORY-011 — Vehicle Sharing and Permissions

## Status
`[TODO]`

## Objetivo

Permitir o compartilhamento seguro de veículos entre múltiplos usuários (ex: familiares, sócios, motoristas ou mecânicos), com papéis de acesso granulares (`OWNER`, `EDITOR`, `VIEWER`).

## Problema que resolve

Veículos frequentemente possuem mais de um condutor ou são gerenciados em conjunto por cônjuges, familiares ou gestores de pequenas frotas. O modelo atual vincula o veículo a um único usuário rígido (`owner_id`). É necessário permitir que outros usuários cadastrados visualizem ou adicionem manutenções sem alterar a propriedade original do veículo.

## Escopo

### Backend
- Criar o módulo `sharing` em `com.mmetzner.vmh.sharing`.
- Tabela `vehicle_memberships`:
  - `id` (UUID)
  - `vehicleId` (UUID do veículo)
  - `userId` (UUID do usuário convidado/membro)
  - `role` Enum:
    - `OWNER`: Proprietário total. Pode gerenciar veículos, editar tudo, compartilhar e revogar acessos, ou excluir o veículo.
    - `EDITOR`: Pode cadastrar e editar manutenções, despesas e anexos, mas não pode excluir o veículo nem alterar membros.
    - `VIEWER`: Acesso somente leitura a todo o histórico do veículo, despesas e relatórios.
  - `createdAt`
- Sistema de Convites:
  - Geração de código/link de convite com validade expirável.
  - Endpoints REST em `/api/v1/vehicles/{id}/shares`:
    - `POST /api/v1/vehicles/{id}/shares/invite`: Cria um convite para o e-mail de outro usuário com determinado `role`.
    - `POST /api/v1/vehicles/shares/accept`: Aceita um convite através do código.
    - `GET /api/v1/vehicles/{id}/shares`: Lista os membros do veículo.
    - `DELETE /api/v1/vehicles/{id}/shares/{userId}`: Revoga o acesso de um membro.
- Segurança e Autorização no Spring Security:
  - Avaliador de segurança customizado `@PreAuthorize("@vehicleSecurity.hasRole(#vehicleId, 'EDITOR')")` para proteger endpoints de edição de veículos, manutenções, despesas e anexos.
  - Consultas JPA atualizadas para retornar veículos onde o usuário é `OWNER` ou `MEMBER`.
- Migration Flyway `V13__vehicle_sharing.sql`.

### Web
- Aba "Compartilhamento" na página de detalhes do veículo.
- Modal de envio de convite por e-mail e seleção de papel (`EDITOR` ou `VIEWER`).
- Lista de membros com opção de alteração de papel ou revogação de acesso pelo `OWNER`.
- Ocultação visual de botões de edição/exclusão para usuários com papel `VIEWER`.

### Mobile
- Exibição de insígnias de papel de acesso (`Dono`, `Editor`, `Leitor`) no cartão do veículo.
- Restrição de formulários de edição e remoção na interface Compose para usuários `VIEWER`.
- Telas de convite e gerenciamento de membros.

## Fora do escopo

- Controle de frotas corporativas avançadas com mais de 100 veículos.

## Dependências

- [STORY-002 — Vehicle Domain v2](STORY-002-vehicle-domain-v2.md)

## Critérios de aceite

- O proprietário (`OWNER`) consegue convidar outro usuário cadastrado por e-mail informando o papel (`EDITOR` ou `VIEWER`).
- O usuário convidado passa a visualizar o veículo compartilhado em sua lista.
- Usuários `EDITOR` conseguem criar e alterar manutenções e despesas, mas não conseguem excluir o veículo nem remover membros.
- Usuários `VIEWER` conseguem apenas ler os dados, sendo bloqueados em qualquer tentativa de mutação pela API e UI.
- Testes de segurança cobrem tentativas não autorizadas de edição e revogação.

## Considerações arquiteturais

- A autorização não deve depender apenas da verificação do token JWT, mas da verificação explicita no banco de dados da relação entre `userId`, `vehicleId` e o `role` exigido para a operação.

## Testes esperados

- Testes unitários do componente de autorização `@vehicleSecurity`.
- Testes de integração enviando requisições com tokens de usuários com diferentes papéis (`OWNER`, `EDITOR`, `VIEWER`, e usuários sem acesso).
- Testes de permissões de UI em Web e Mobile.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
