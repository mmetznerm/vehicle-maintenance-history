# STORY-012 — Vehicle History Report

## Status
`[TODO]`

## Objetivo

Gerar um relatório consolidado e elegível para impressão/compartilhamento em formato **PDF** (Dossiê do Veículo / Vehicle History Report), apresentando a linha do tempo completa de manutenções, resumo de despesas, histórico de proprietários e comprovantes de saúde do veículo.

## Problema que resolve

Ao vender um veículo ou comprovar seu histórico de revisões para seguradoras ou futuros compradores, o proprietário precisa de um documento PDF unificado e profissional contendo todo o histórico organizado cronologicamente, com carimbo de autenticidade e custos agrupados, sem requerer exportações manuais ou planilhas soltas.

## Escopo

### Backend
- Criar o módulo `report` em `com.mmetzner.vmh.report`.
- Motor de Geração de PDF (usando OpenPDF / iText / Thymeleaf + Flying Saucer / PDFBox):
  - Cabeçalho: Dados do Veículo (Marca, Modelo, Versão, Ano, Placa, VIN, Cor, Hodômetro Atual, Foto principal se houver).
  - Seção 1: Resumo do Veículo e Estatísticas Gerais (Total gasto, média de custo/km, data de cadastro).
  - Seção 2: Linha do Tempo Cronológica de Manutenções (Data, hodômetro, categoria, descrição, oficina, mecânico, custo de peças, custo de mão de obra e total).
  - Seção 3: Resumo de Despesas por Categoria (Gráfico/Tabela de custos com combustível, seguro, impostos).
  - Seção 4: Status da Manutenção Preventiva (Itens em dia vs itens pendentes).
  - Rodapé: Data e hora de geração do relatório, hash SHA-256 de verificação de autenticidade do documento.
- Endpoint REST em `GET /api/v1/vehicles/{id}/report/pdf`:
  - Retorna o fluxo binário do PDF com header `Content-Disposition: attachment; filename="relatorio-veiculo-{placa}.pdf"`.

### Web
- Botão "Exportar Relatório PDF" na página de detalhes do veículo.
- Modal de confirmação com opções de inclusão de seções (ex: "Incluir despesas financeiras?", "Incluir fotos de comprovantes?").
- Visualizador e download direto do PDF no navegador.

### Mobile
- Botão "Gerar Relatório PDF" no app mobile.
- Abertura do PDF gerado no visualizador nativo do Android/iOS e compartilhamento via menu nativo do sistema (Share Sheet / Intent).

## Fora do escopo

- Envio físico via correios ou impressão gráfica em papel.

## Dependências

- [STORY-004 — Attachments, Photos and Documents](STORY-004-attachments-photos-documents.md)
- [STORY-008 — Vehicle Expenses](STORY-008-vehicle-expenses.md)
- [STORY-009 — Preventive Maintenance Rules](STORY-009-preventive-maintenance-rules.md)

## Critérios de aceite

- O PDF é gerado corretamente no backend contendo o layout formatado e profissional.
- O relatório inclui todas as manutenções, despesas e status preventivos selecionados.
- O arquivo PDF pode ser baixado e aberto em qualquer leitor de PDF padrão.
- Web e Mobile fornecem atalhos amigáveis para gerar e compartilhar o relatório.
- Testes verificam a geração do relatório e tratamento de dados nulos/vazios sem exceção.

## Considerações arquiteturais

- O gerador de PDF deve ser eficiente em termos de memória (streaming de bytes sem carregar grandes arrays em heap desnecessariamente).
- Garantir formatação adequada de moedas (BRL/USD) e datas de acordo com o locale.

## Testes esperados

- Testes unitários do construtor do relatório PDF verificando tamanho de dados gerados e seções.
- Testes de integração do endpoint `/report/pdf` verificando tipo de conteúdo `application/pdf`.
- Testes de integração no frontend e mobile para download de arquivo binário.

## Definition of Done

- [ ] Backend implementado
- [ ] Web implementado
- [ ] Mobile implementado
- [ ] Testes implementados
- [ ] CI passando
- [ ] Documentação atualizada
- [ ] Story marcada como concluída
