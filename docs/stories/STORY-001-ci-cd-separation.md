# STORY-001 — Separar claramente CI de AWS/CD

## Status
`[TODO]`

## Objetivo

Garantir que a integração contínua (CI) do projeto execute e valide 100% das alterações de código (Backend, Frontend Web e Mobile) sem depender de contas, credenciais, segredos ou infraestrutura AWS ativa. A esteira de publicação/deployment AWS (CD) deve continuar existindo e documentada, porém tratada como uma capacidade condicional/opcional.

## Problema que resolve

Atualmente, execuções do pipeline no GitHub Actions na branch `main` tentam realizar login no Amazon ECR (`aws-actions/amazon-ecr-login`) e autenticação OIDC (`vars.AWS_PUBLISH_ROLE_ARN`). Quando a AWS está desativada ou com credenciais omitidas para evitar custos de nuvem durante o desenvolvimento, o pipeline falha injustificadamente, bloqueando o avanço do desenvolvimento.

## Escopo

### Backend / Web
- Revisar o workflow do GitHub Actions em `.github/workflows/ci-cd.yml`.
- Garantir que os jobs de testes unitários do Spring Boot (`backend-unit-tests`) e testes de integração com Testcontainers (`backend-integration-tests`) executem sem qualquer dependência de nuvem.
- Garantir que os testes do frontend Web React (`frontend-quality`), verificação ESLint e build Vite continuem rodando no CI principal.
- Manter as verificações de segurança CodeQL SAST para Java/Kotlin e JavaScript/TypeScript.
- Manter o job de validação de contratos de deployment (`deployment-contracts`: ShellCheck, Actionlint, Compose render).
- Condicionar os jobs de publicação (`publish-docker-image`) e deployment EC2 (`deploy-to-ec2`) à presença explicita de credenciais ou à flag de ambiente `vars.AWS_DEPLOY_ENABLED == 'true'`, evitando que falhas na AWS cancelem o CI principal.
- Garantir que o build local da imagem Docker via `docker compose up --build` continue funcionando.

### Mobile
- Garantir que o workflow `Mobile CI` (`.github/workflows/ci.yml`) no repositório KMP continue validando `:composeApp:assembleDebug` e `:composeApp:allTests` de forma independente.

## Fora do escopo

- Excluir arquivos de deployment AWS, scripts em `deploy/` ou ADRs da AWS.
- Configurar novas instâncias ou recursos na AWS.

## Dependências

Nenhuma estória prévia requerida.

## Critérios de aceite

- O envio de um Pull Request e o push para a branch `main` passam completamente em verde mesmo com nenhuma conta AWS ativa ou configurada.
- A build da imagem Docker continua podendo ser construída localmente via Docker Compose.
- A esteira de deployment AWS (ECR/EC2/SSM) continua totalmente disponível e acionável via configuração quando habilitada.
- Nenhuma documentação existente sobre AWS ou scripts em `deploy/` são apagados.

## Considerações arquiteturais

- O CI deve focar na validação da qualidade de código (testes unitários, integração, linters, SAST, contratos).
- A separação entre **CI (Continous Integration)** e **CD (Continuous Deployment)** garante resiliência e independência de custos.

## Testes esperados

- Execução bem-sucedida de `./mvnw verify` no backend.
- Execução bem-sucedida de `./mvnw verify -Pintegration-tests` com Testcontainers.
- Execução bem-sucedida de `npm run test:coverage` no frontend.
- Execução dos testes de contrato em `deploy/tests/`.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
