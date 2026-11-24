# Tasks

## 1. Interface de gestão com dados fictícios

- [x] 1.1 Substituir o placeholder pela lista de cartões da tela 01. Verificar busca por protocolo, ordenação “Mais recentes”, conteúdo e estados de carregamento, vazio e erro com dados de teste claramente identificados.
- [x] 1.2 Montar detalhes e recebimento conforme as telas 02 e 03, com rotas diretas, breadcrumbs, ação de rejeição da AMMA, confirmações e erros de campo. Conferir no desktop, tablet e mobile a hierarquia visual, o botão “Voltar”, a navegação por teclado, o foco visível e os selos textuais.
- [x] 1.3 Aplicar a permissão de gestão existente ao item “Solicitações” e manter o item ativo nas rotas de detalhe e recebimento. Verificar visibilidade para os sete papéis, ausência para cidadão e a regra atual do item “Equipe do Laboratório”.

## 2. Persistência, API e permissões

- [ ] 2.1 Reaproveitar ou coordenar a base compartilhada do Flyway. Adicionar migração versionada para local de plantio, quantidade recebida e motivo de rejeição opcionais, além de eventual ajuste de restrição dos status. Atualizar `SolicitacaoMudaStatus` e o mapeamento JPA. Verificar a migração em PostgreSQL vazio e com dados legados, a inicialização com `ddl-auto=validate`, a preservação de pedidos existentes, a persistência de `CALCADA` e `QUINTAL` e o erro de entrada para local inválido. Executar os testes de criação e consulta públicas.
- [ ] 2.2 Implementar lista paginada e detalhe sob `/api/solicitacoes-muda/gestao`. Verificar ordenação, normalização do protocolo, busca sem resultado, detalhe inexistente e ausência de CPF e telefone nos DTOs administrativos.
- [ ] 2.3 Implementar recebimento e rejeição no Service e Repository com DTOs de entrada, `ProblemDetail` e transições atômicas. Verificar quantidade igual, superior, inferior e inválida, motivo ausente ou longo, pedido já decidido, operações concorrentes e que as falhas não gravam alterações parciais.
- [ ] 2.4 Aplicar `@PreAuthorize` e acrescentar matcher específico no `SecurityConfig` se necessário. Exercitar os sete papéis e aliases, 401 sem autenticação, 403 para cidadão e para rejeição pelo IFCE. Verificar que criação, conversão e consulta públicas continuam acessíveis.

## 3. Integração do painel e do cidadão

- [ ] 3.1 Ligar as três telas de gestão à API, removendo dados fictícios. Verificar atualização do status após aprovação ou rejeição, exibição de “Não informado” no legado, recuperação após erro, mensagem em conflito e ausência de CPF ou telefone na interface e nos logs.
- [x] 3.2 Propagar o local de plantio do quiz no envio existente do formulário. Conferir os dois caminhos (Calçada e Quintal) até o detalhe e o envio direto sem resposta do quiz.
- [ ] 3.3 Derivar linha do tempo e selo do status real em `AcompanharSolicitacaoView.vue`. Consultar após recarregar a página e verificar os quatro estados, a orientação exata para `APROVADA`, o protocolo reutilizado e o mesmo 404 para CPF incompatível e protocolo inexistente.

## 4. Verificação integrada

- [ ] 4.1 Exercitar em PostgreSQL a jornada quiz → pedido → busca → conferência e aprovação → acompanhamento. Verificar também rejeição da AMMA e leitura de uma solicitação em `RETIRADA_CONCLUIDA` como contrato com a SPEC-S3-01. Executar tipagem/build do frontend e compilação/testes do backend.
