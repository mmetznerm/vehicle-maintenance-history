# STORY-004 — Attachments, Photos and Documents

## Status
`[TODO]`

## Objetivo

Permitir o upload, visualização e gerenciamento de arquivos anexos (fotos de comprovantes, notas fiscais, recibos, fotos do veículo ou relatórios) associados a veículos ou manutenções.

## Problema que resolve

Usuários precisam armazenar documentos fiscais e fotos das peças/serviços realizados junto com o histórico de manutenção para fins de garantia, comprovação em futuras vendas ou organização pessoal.

## Escopo

### Backend
- Criar o módulo `attachment` em `com.mmetzner.vmh.attachment`.
- Entidade `Attachment`:
  - `id` (UUID)
  - `ownerId` (UUID do usuário proprietário)
  - `vehicleId` (UUID opcional do veículo)
  - `maintenanceId` (UUID opcional da manutenção)
  - `fileName` (Nome original do arquivo)
  - `fileType` Enum (`PHOTO`, `INVOICE`, `RECEIPT`, `DOCUMENT`, `OTHER`)
  - `contentType` (MIME type, ex: `image/jpeg`, `application/pdf`)
  - `fileSizeBytes` (Tamanho em bytes)
  - `storageKey` (Chave de referência do armazenamento)
  - `createdAt` (Data de upload)
- Abstração de Armazenamento de Arquivos:
  ```java
  public interface FileStorage {
      String store(InputStream inputStream, String originalFilename, String contentType);
      Resource load(String storageKey);
      void delete(String storageKey);
  }
  ```
  - `LocalFileStorage`: Implementação para ambiente local/desenvolvimento (armazena no sistema de arquivos local ou volume Docker).
  - `S3FileStorage`: Implementação pronta para AWS S3 (ativada via Spring profile `prod`/`aws` no futuro).
- Endpoints REST em `/api/v1/attachments`:
  - `POST /api/v1/attachments` (Upload multipart com validações de tamanho máx. 10MB e MIME types permitidos)
  - `GET /api/v1/attachments/{id}/content` (Download/stream do arquivo com verificação de autorização)
  - `GET /api/v1/attachments?vehicleId={id}&maintenanceId={id}` (Listagem de anexos)
  - `DELETE /api/v1/attachments/{id}` (Remoção do anexo e exclusão do arquivo no storage)
- Migration Flyway `V8__create_attachments.sql`.

### Web
- Componente de upload de arquivos (Drag and Drop ou seletor de arquivos) com barra de progresso.
- Galeria de pré-visualização de imagens e visualizador integrado de documentos PDF na página de manutenção e veículo.
- Ação de exclusão com confirmação.

### Mobile
- Integração com Câmera e Galeria de Fotos nativas no Android e iOS via Kotlin Multiplatform.
- Armazenamento em cache local de imagens para suporte offline.
- Fila de upload de imagens integrada ao Outbox Pattern (upload executado em background quando houver conectividade).

## Fora do escopo

- Reconhecimento óptico de caracteres (OCR) em notas fiscais.
- Compressão/redimensionamento automático de imagem no backend (focar no armazenamento direto).

## Dependências

- [STORY-003 — Maintenance Domain v2](STORY-003-maintenance-domain-v2.md)

## Critérios de aceite

- Usuário consegue anexar imagens e PDFs a uma manutenção ou veículo.
- Download e visualização funcionam corretamente respeitando as permissões de acesso do usuário.
- O storage local armazena os arquivos no disco em ambiente de desenvolvimento sem requerer AWS S3.
- Abstração `FileStorage` permite chavear para S3 via propriedade de configuração.
- Exclusão de um registro de anexo remove o arquivo físico correspondente.
- Mobile permite capturar foto pela câmera ou galeria e sincronizar.

## Considerações arquiteturais

- O domínio não deve ter qualquer dependência direta de SDKs da AWS (ex: `S3Client`). Toda interação com armazenamento deve ocorrer via interface `FileStorage`.
- Validação rigorosa de tipos de arquivo para prevenir vulnerabilidades de upload de código malicioso.

## Testes esperados

- Testes unitários do `LocalFileStorage` e serviço de anexos.
- Testes de integração enviando arquivos fictícios (`MockMultipartFile`) para a API REST.
- Testes frontend de upload e renderização de galeria.
- Testes mobile de captura e persistência no Room.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
