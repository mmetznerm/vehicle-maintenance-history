# STORY-014 — Production Infrastructure

## Status
`[TODO]`

## Objetivo

Reativar, estabilizar e produzir a infraestrutura de publicação e deployment em nuvem (AWS) quando o produto estiver com todas as suas funcionalidades concluídas, estabilizadas e prontas para usuários reais.

## Problema que resolve

Durante a fase de desenvolvimento, a infraestrutura AWS permaneceu desativada para eliminar custos recorrentes. Com a plataforma pronta para produção, é necessário reativar a esteira automatizada de publicação, garantindo segurança (HTTPS), alta disponibilidade, backups do banco de dados, observabilidade e gerenciamento de segredos.

## Escopo

### Backend / DevOps
- Reativar a estratégia de deployment automatizada documentada em `docs/aws-deployment.md` e `docs/portfolio-roadmap.md`.
- Avaliar através de um ADR formal as opções de compute adequadas ao produto amadurecido:
  - **Opção A**: Manter EC2 de porte adequado com Docker Compose de produção (`deploy/compose.prod.yml`) e NGINX / Caddy para terminação TLS.
  - **Opção B**: Migrar para AWS ECS on EC2 ou ECS Fargate se houver necessidade real de auto-scaling e alta disponibilidade.
- Configuração de Domínio e HTTPS:
  - Configuração de certificado TLS/SSL gratuito via Let's Encrypt (Certbot) ou AWS Certificate Manager (ACM).
  - Redirecionamento forçado de HTTP para HTTPS (porta 443).
- Segurança e Gerenciamento de Segredos:
  - Armazenamento seguro de senhas de banco de dados, JWT secrets e chaves no AWS SSM Parameter Store ou AWS Secrets Manager.
  - OIDC mantido para autenticação do GitHub Actions sem chaves estáticas AWS de longa duração.
- Banco de Dados PostgreSQL em Produção:
  - Verificação de políticas de backup automatizado no Amazon RDS PostgreSQL com retenção de snapshots de no mínimo 7 dias.
  - Verificação de conexões criptografadas SSL/TLS (`sslmode=require`).
- Observabilidade e Alertas:
  - Configuração de métricas essenciais e retenção de logs no AWS CloudWatch.
  - Alarmes CloudWatch para métricas críticas (Uso de CPU > 80%, Espaço em Disco < 10%, Erros 5xx na API).
- Automação de Rollback:
  - Garantir rollback automatizado no GitHub Actions/SSM para a tag Docker saudável anterior caso o deploy falhe no teste de saúde (`actuator/health`).

### Web
- Atualizar a configuração de produção da SPA React 19 para utilizar URLs relativas ou variáveis de ambiente de produção com suporte a HTTPS e cabeçalhos de segurança (CSP, HSTS, X-Content-Type-Options).

### Mobile
- Configurar builds de produção (Release APK / AAB para Android e IPA para iOS).
- Apontamento das chamadas Ktor para o endpoint de produção com HTTPS e armazenamento seguro de tokens no Keychain (iOS) e EncryptedSharedPreferences (Android).

## Fora do escopo

- Migração para Kubernetes (EKS) ou arquitetura serverless complexa sem necessidade justificada.

## Dependências

- [STORY-001 — Separar claramente CI de AWS/CD](STORY-001-ci-cd-separation.md)
- [STORY-012 — Vehicle History Report](STORY-012-vehicle-history-report.md)

## Critérios de aceite

- A aplicação está acessível publicamente com HTTPS válido e domínio configurado.
- O pipeline de CD do GitHub Actions efetua builds e deployments automatizados com zero-downtime ou tempo de inatividade mínimo.
- Banco de dados RDS PostgreSQL possui backups automáticos ativados.
- Métricas e logs do sistema são encaminhados com sucesso para observabilidade.
- Se o teste de saúde da nova versão falhar, o deploy cancela e realiza rollback para a versão estável anterior.
- Web e Mobile conectam-se de forma segura e operacional à infraestrutura de produção.

## Considerações arquiteturais

- Manter rígido controle de custos na AWS, definindo alertas de orçamento (AWS Budgets) para evitar surpresas financeiras.
- Decisões de migração para ECS ou Terraform devem ser formalmente documentadas em um ADR antes de serem executadas.

## Testes esperados

- Execução dos testes automatizados de contrato em `deploy/tests/` (ShellCheck, Actionlint, SSM wait tests, Compose render).
- Teste de carga e estresse moderado na API de produção.
- Teste de simulação de rollback automatizado.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
