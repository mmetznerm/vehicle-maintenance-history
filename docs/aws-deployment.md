# Implantação na AWS

Este runbook cria a implantação pequena do portfólio na região `us-east-2`.
Execute as etapas na ordem e substitua somente os valores marcados como saída de
um comando. Os comandos de consulta são somente leitura; o runbook não executa
mudanças na AWS por você.

## 1. Segurança, custos e limites da demonstração

Esta é uma demonstração pública por HTTP. Ela não tem HTTPS, domínio próprio,
balanceador ou proteção de produção. Use somente dados e credenciais de teste;
nunca coloque dados pessoais, senhas reutilizadas ou segredos reais de clientes
na aplicação.

Antes de iniciar, abra o RDS no console da AWS, selecione
`vehicle-maintenance-history-db` e escolha **Modify**. Em **Storage**,
configure o limite máximo de armazenamento autoscaling como **30 GiB** e
aplique a alteração conforme a janela escolhida. A instância atualmente usa
20 GiB; o limite de 30 GiB evita crescimento acidental em um ambiente de
portfólio. O `db.t4g.micro`, o `t3.micro` e o volume de 10 GiB são escolhas de
baixo custo, mas continuam gerando cobrança enquanto estiverem ativos.

Ao terminar a demonstração, pare ou remova os recursos que não forem mais
necessários e verifique o Billing Dashboard. Não remova o RDS antes de decidir
como preservar os dados de teste.

## 2. Amazon ECR privado

1. Seletor de região: **US East (Ohio) `us-east-2`**.
2. Abra **Amazon ECR > Repositories** e confirme o repositório privado
   `mmetznerm/vehicle-maintenance-history`.
3. Não altere o repositório para público e não crie outro nome. O workflow
   publica o mesmo build no Docker Hub e neste ECR; o EC2 usa somente o ECR
   privado em tempo de execução.
4. Em **Tag immutability**, selecione **Immutable with exclusion** e configure
   a única exclusão com tipo **Wildcard** e valor `latest`. O tag `latest` é
   apenas um ponteiro de conveniência; os deploys e rollbacks usam somente
   `sha-<commit>`.
5. Como alternativa ao console, aplique e verifique a configuração com:

   ```bash
   aws ecr put-image-tag-mutability \
     --region us-east-2 \
     --repository-name mmetznerm/vehicle-maintenance-history \
     --image-tag-mutability IMMUTABLE_WITH_EXCLUSION \
     --image-tag-mutability-exclusion-filters filterType=WILDCARD,filter=latest

   aws ecr describe-repositories \
     --region us-east-2 \
     --repository-names mmetznerm/vehicle-maintenance-history \
     --query 'repositories[0].{mutability:imageTagMutability,exclusions:imageTagMutabilityExclusionFilters}' \
     --output json
   ```

   A saída precisa mostrar `IMMUTABLE_WITH_EXCLUSION` e somente a exclusão
   `latest`. Uma repetição do workflow não pode republicar uma tag SHA que já
   exista; use a imagem SHA existente para deploy ou rollback.

## 3. Rede e grupos de segurança

Use a mesma VPC do RDS. No console, abra **VPC > Security groups** e crie
`vehicle-maintenance-history-app-sg` com uma descrição que identifique a
aplicação. Em **Inbound rules**, adicione somente:

| Tipo | Protocolo | Porta | Origem |
|---|---|---:|---|
| HTTP | TCP | 80 | `0.0.0.0/0` |

Mantenha a saída necessária para o ECR, SSM, Docker Hub e RDS (a saída padrão
permite isso). Não adicione uma regra TCP 22.

Crie ou confirme `vehicle-maintenance-history-db-sg`. No grupo do banco,
adicione somente esta entrada:

| Tipo | Protocolo | Porta | Origem |
|---|---|---:|---|
| PostgreSQL | TCP | 5432 | referência a `vehicle-maintenance-history-app-sg` |

Na tela **RDS > Databases > vehicle-maintenance-history-db > Modify >
Connectivity**, associe `vehicle-maintenance-history-db-sg` ao RDS e confirme
**Public access: No**. A origem da porta 5432 deve ser a referência do grupo de
segurança, nunca um CIDR público. Revise ambos os grupos e confirme que nenhum
expõe a porta 22.

## 4. Provedor OIDC do GitHub

Em **IAM > Identity providers**, adicione um provedor **OpenID Connect** com:

- URL do provedor: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

Se esse provedor já existir, reutilize-o depois de confirmar exatamente esses
dois valores. O workflow recebe credenciais temporárias por OIDC; não crie nem
grave credenciais AWS de longa duração no GitHub.

## 5. Roles do GitHub Actions

Em **IAM > Roles > Create role**, crie as duas roles abaixo usando o provedor
OIDC do GitHub e a mesma política de confiança, restrita ao repositório e à
branch `main`:

- `github-actions-vehicle-maintenance-history-publish`;
- `github-actions-vehicle-maintenance-history-deploy`.

A política de confiança equivalente, aplicada a ambas, é:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::675244612319:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
          "token.actions.githubusercontent.com:sub": "repo:mmetznerm/vehicle-maintenance-history:ref:refs/heads/main"
        }
      }
    }
  ]
}
```

Para `github-actions-vehicle-maintenance-history-publish`, adicione uma
política inline com apenas as permissões de login e publicação no ECR:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EcrLogin",
      "Effect": "Allow",
      "Action": "ecr:GetAuthorizationToken",
      "Resource": "*"
    },
    {
      "Sid": "EcrPushApplication",
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": "arn:aws:ecr:us-east-2:675244612319:repository/mmetznerm/vehicle-maintenance-history"
    }
  ]
}
```

Para `github-actions-vehicle-maintenance-history-deploy`, adicione uma política
inline com apenas as permissões SSM abaixo. O ARN da instância será completado
com o ID real obtido na etapa 8; `<EC2_INSTANCE_ID>` é um campo obrigatório a
substituir, não um valor de exemplo:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SsmSendToSelectedInstance",
      "Effect": "Allow",
      "Action": "ssm:SendCommand",
      "Resource": [
        "arn:aws:ec2:us-east-2:675244612319:instance/<EC2_INSTANCE_ID>",
        "arn:aws:ssm:us-east-2::document/AWS-RunShellScript"
      ]
    },
    {
      "Sid": "SsmCommandRead",
      "Effect": "Allow",
      "Action": [
        "ssm:GetCommandInvocation",
        "ssm:ListCommandInvocations",
        "ssm:ListCommands"
      ],
      "Resource": "*"
    }
  ]
}
```

O `id-token: write` fica restrito aos jobs que assumem cada role. A role de
publicação não pode receber permissões SSM, e a role de deploy não pode receber
permissões ECR; nenhuma delas pode receber permissões da outra. Não adicione
`sts:AssumeRole` genérico, acesso a outros repositórios, outras branches ou
outros repositórios ECR.

## 6. Role da instância EC2

Em **IAM > Roles > Create role**, escolha **EC2** como serviço confiável e crie
`vehicle-maintenance-history-ec2-role`. Anexe as políticas gerenciadas:

- `AmazonSSMManagedInstanceCore`;
- `AmazonEC2ContainerRegistryPullOnly`.

Adicione uma política inline para ler somente os parâmetros da aplicação e
descriptografar SecureStrings através do Systems Manager em Ohio:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadApplicationParameters",
      "Effect": "Allow",
      "Action": "ssm:GetParameter",
      "Resource": [
        "arn:aws:ssm:us-east-2:675244612319:parameter/vmh/prod/app-env",
        "arn:aws:ssm:us-east-2:675244612319:parameter/vmh/prod/rds-master-password"
      ]
    },
    {
      "Sid": "DecryptOnlyThroughSsmOhio",
      "Effect": "Allow",
      "Action": "kms:Decrypt",
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "kms:ViaService": "ssm.us-east-2.amazonaws.com"
        }
      }
    }
  ]
}
```

O script usa `ssm:GetParameter` com `--with-decryption`; não dê à instância
permissão para apagar ou alterar parâmetros. Acesso ao parâmetro do master
existe apenas para o bootstrap e será removido na etapa 10.

A condição KMS permite somente `kms:Decrypt` invocado pelo Systems Manager em
`us-east-2`. Use as chaves padrão da conta para SSM e EBS; uma chave gerenciada
pelo cliente é opcional e só deve ser escolhida deliberadamente.

## 7. Parâmetros SecureString

Abra **Systems Manager > Parameter Store > Create parameter**. Use tipo
**SecureString**, região `us-east-2` e não registre o conteúdo em arquivos do
repositório ou em logs.

Primeiro gere os dois valores localmente ou no CloudShell. Copie cada saída
diretamente para o formulário do console e não publique a saída:

```bash
APP_DB_PASSWORD="$(openssl rand -hex 32)"
JWT_SECRET="$(openssl rand -hex 64)"
printf '%s\n' "$APP_DB_PASSWORD"
printf '%s\n' "$JWT_SECRET"
```

Para obter o endpoint real, execute no CloudShell, na região correta:

```bash
aws rds describe-db-instances --region us-east-2 --db-instance-identifier vehicle-maintenance-history-db --query 'DBInstances[0].Endpoint.Address' --output text
```

Copie o texto retornado, sem espaços ou aspas, para o valor de `RDS_HOST` e
para o host da URL JDBC abaixo. Crie `/vmh/prod/app-env` com exatamente estas
seis linhas; substitua os três campos entre `<...>` pelas saídas/valores
gerados, sem manter os marcadores:

```text
RDS_HOST=<endpoint retornado pelo comando do RDS>
SPRING_DATASOURCE_URL=jdbc:postgresql://<endpoint retornado pelo comando do RDS>:5432/vehicle_maintenance_history?sslmode=require
SPRING_DATASOURCE_USERNAME=vmh_app
SPRING_DATASOURCE_PASSWORD=<saída de openssl rand -hex 32>
JWT_SECRET=<saída de openssl rand -hex 64>
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70.0
```

O nome das variáveis precisa permanecer exatamente como acima: `RDS_HOST` é
usado pelo bootstrap, e as variáveis `SPRING_DATASOURCE_*` e `JWT_SECRET` são
passadas pelo Compose para a aplicação. Não acrescente `IMAGE_URI` ou
`IMAGE_TAG`; `deploy.sh` acrescenta esses valores ao arquivo local no EC2.

Crie `/vmh/prod/rds-master-password` como **SecureString**, contendo a senha
existente do usuário master `vmh_admin` do RDS. Esse valor não é a senha de
`vmh_app`, não deve ser mostrado no terminal e só ficará disponível até a
criação inicial do banco e da role da aplicação.

## 8. Instância EC2

Em **EC2 > Launch instance**, configure:

- AMI: **Amazon Linux 2023**, arquitetura **x86_64**;
- tipo: `t3.micro`;
- armazenamento raiz: **10 GiB**, `gp3`, **Encrypted: Yes**, usando a chave
  EBS padrão da conta;
- metadados da instância: **Metadata version: V2 only** (IMDSv2);
- rede: a mesma VPC do RDS e uma subnet pública com saída para a internet;
- atribuição automática de IPv4 público: habilitada;
- security group: `vehicle-maintenance-history-app-sg`;
- IAM instance profile: `vehicle-maintenance-history-ec2-role`;
- tags: `Name=vehicle-maintenance-history` e
  `Application=vehicle-maintenance-history`.

Não configure regra de SSH nem dependa de um endereço IP fixo. O acesso
operacional será pelo **Systems Manager > Session Manager**; a troca do IPv4
após uma parada não altera o alvo SSM, que é o ID da instância.

Depois que a instância estiver criada, obtenha o ID real no CloudShell com o
comando somente leitura abaixo:

```bash
aws ec2 describe-instances --region us-east-2 --filters 'Name=tag:Name,Values=vehicle-maintenance-history' 'Name=instance-state-name,Values=pending,running,stopping,stopped' --query 'Reservations[0].Instances[0].InstanceId' --output text
```

Copie a saída para o campo `EC2_INSTANCE_ID` nas variáveis do repositório e
substitua `<EC2_INSTANCE_ID>` no ARN da política da role do GitHub (etapa 5).
Esse mesmo ID deve aparecer como instância gerenciada no Systems Manager antes
do primeiro bootstrap.

## 9. Primeiro deploy manual

Antes do primeiro push, crie as variáveis básicas da seção 11, deixando
`AWS_DEPLOY_ENABLED` ausente. Assim o workflow pode publicar no ECR, mas não
tenta enviar SSM para uma instância ainda não preparada.

1. Em **Systems Manager > Fleet Manager**, confirme que a instância está
   **Online** e abra uma sessão em **Session Manager**.
2. Em **ECR**, copie o primeiro tag `sha-<commit>` publicado para
   `mmetznerm/vehicle-maintenance-history`.
3. Abra o workflow de `main` que publicou esse tag e copie o commit completo de
   40 caracteres hexadecimais. O tag curto e esse commit precisam ser da mesma
   execução.
4. Na sessão do EC2, baixe os três arquivos do commit completo. O diretório
   abaixo é temporário para o bootstrap; o script copiará os arquivos de
   produção para `/opt/vehicle-maintenance-history`:

   ```bash
   sudo mkdir -p /opt/vehicle-maintenance-history/bootstrap
   cd /opt/vehicle-maintenance-history/bootstrap
   sudo curl -fsSL https://raw.githubusercontent.com/mmetznerm/vehicle-maintenance-history/<full-commit>/deploy/compose.prod.yml -o compose.prod.yml
   sudo curl -fsSL https://raw.githubusercontent.com/mmetznerm/vehicle-maintenance-history/<full-commit>/deploy/deploy.sh -o deploy.sh
   sudo curl -fsSL https://raw.githubusercontent.com/mmetznerm/vehicle-maintenance-history/<full-commit>/deploy/setup-ec2.sh -o setup-ec2.sh
   ```

   Substitua `<full-commit>` pelo SHA completo copiado no passo anterior.
5. Execute o bootstrap como root, usando a URI completa do ECR e o tag que
   você copiou:

   ```bash
   sudo bash setup-ec2.sh 675244612319.dkr.ecr.us-east-2.amazonaws.com/mmetznerm/vehicle-maintenance-history <sha-tag>
   ```

   O `<sha-tag>` deve ter o formato `sha-` seguido do valor hexadecimal
   publicado. O script instala Docker/Compose, cria o swap de 1 GiB, usa as
   SecureStrings, cria `vmh_app` e `vehicle_maintenance_history` se necessário
   e chama `deploy.sh` com a imagem imutável.
6. Na saída da sessão, confirme `Health check succeeded` e a mensagem final
   `Bootstrap complete. After confirming the application is healthy, delete
   /vmh/prod/rds-master-password.`. Se o health check falhar, não apague o
   parâmetro do master; corrija a causa e repita o bootstrap.

## 10. Remover a credencial de bootstrap

Somente depois de confirmar a mensagem de saúde e a aplicação funcionando,
abra **Systems Manager > Parameter Store**, selecione
`/vmh/prod/rds-master-password` e escolha **Delete**. Um administrador também
pode executar a exclusão pelo CloudShell com `ssm:DeleteParameter`. Não dê essa
permissão à role da instância.

Mantenha `/vmh/prod/app-env`. Deploys posteriores precisam dele, enquanto o
aplicativo nunca recebe a senha master. Se o bootstrap ainda não foi validado,
preserve o parâmetro para permitir uma nova execução segura.

## 11. Variáveis e secrets do GitHub

Em **GitHub > repositório > Settings > Secrets and variables > Actions >
Variables**, crie:

| Nome | Valor |
|---|---|
| `AWS_REGION` | `us-east-2` |
| `AWS_PUBLISH_ROLE_ARN` | `arn:aws:iam::675244612319:role/github-actions-vehicle-maintenance-history-publish` |
| `AWS_DEPLOY_ROLE_ARN` | `arn:aws:iam::675244612319:role/github-actions-vehicle-maintenance-history-deploy` |
| `ECR_REPOSITORY` | `mmetznerm/vehicle-maintenance-history` |
| `EC2_INSTANCE_ID` | saída real do comando `describe-instances` da etapa 8 |

Durante a preparação, deixe `AWS_DEPLOY_ENABLED` **ausente**. Depois que o
primeiro deploy manual e todas as verificações forem aprovados, crie essa
variável com valor `true`. O job de deploy só roda em push para `main` quando
ela está exatamente como `true`.

Em **Secrets**, mantenha os secrets existentes `DOCKER_USERNAME` e
`DOCKER_ACCESS_TOKEN`, usados apenas para publicar no Docker Hub. Não adicione
segredos AWS persistentes: a role OIDC fornece credenciais temporárias aos
jobs de publicação e deploy. A publicação assume `AWS_PUBLISH_ROLE_ARN` e o
deploy assume `AWS_DEPLOY_ROLE_ARN`.

Deploys de produção são serializados no GitHub Actions e novamente no EC2 por
`/var/lock/vehicle-maintenance-history-deploy.lock`. O workflow pode consultar
o estado SSM por até 15 minutos antes de concluir. A varredura de
vulnerabilidades do ECR e uma política de ciclo de vida são melhorias opcionais
e não são pré-requisitos para o aceite desta demonstração.

## 12. Verificações de aceite

Considere a implantação aceita somente quando todos os itens abaixo forem
verdadeiros:

- o mesmo tag `sha-<commit>` existe no Docker Hub e no ECR privado;
- o job de deploy do GitHub Actions termina com sucesso;
- o frontend público abre por `http://<IPv4-público>/` (sem HTTPS nesta fase);
- `http://<IPv4-público>/actuator/health` responde com status `UP`;
- registro e login funcionam somente com dados de teste;
- um dado criado continua presente depois de reiniciar o container;
- o volume raiz EBS de 10 GiB `gp3` está com **Encrypted: Yes** antes da
  instalação de segredos da aplicação;
- o security group da aplicação não tem entrada TCP 22;
- o RDS permanece com **Public access: No** e a porta 5432 aceita somente o
  grupo da aplicação;
- o GitHub não contém credenciais AWS persistentes;
- o limite de autoscaling do armazenamento RDS continua em 30 GiB.

## 13. Operação e rollback manual

Use uma sessão do **Session Manager**, não SSH, para consultar o serviço:

```bash
cd /opt/vehicle-maintenance-history
docker compose --env-file .env -f compose.prod.yml ps
docker compose --env-file .env -f compose.prod.yml logs --tail=200 app
curl -fsS http://localhost/actuator/health
```

Para listar tags disponíveis no ECR pelo CloudShell:

```bash
aws ecr describe-images --region us-east-2 --repository-name mmetznerm/vehicle-maintenance-history --query 'sort_by(imageDetails,&imagePushedAt)[].imageTags' --output table
```

Escolha um `sha-` de uma execução que foi aceita anteriormente e, na sessão do
EC2, execute o rollback manual:

```bash
sudo /opt/vehicle-maintenance-history/deploy.sh 675244612319.dkr.ecr.us-east-2.amazonaws.com/mmetznerm/vehicle-maintenance-history <sha-tag-anterior>
```

Confirme novamente `Health check succeeded` e o Actuator. O projeto não faz
rollback automático; se o tag anterior não estiver no ECR, publique/recupere a
imagem correspondente antes de executar o comando.

Referências oficiais: [GitHub OIDC com AWS](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws),
[autenticação de registro privado do ECR](https://docs.aws.amazon.com/AmazonECR/latest/userguide/registry_auth.html),
[política AmazonEC2ContainerRegistryPullOnly](https://docs.aws.amazon.com/AmazonECR/latest/userguide/security-iam-awsmanpol.html) e
[conexão com RDS PostgreSQL](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_ConnectToPostgreSQLInstance.html).
