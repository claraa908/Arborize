# Tasks

## 1. Interface do Viveiro — Design First

- [x] 1.1 Adicionar as abas “Espécies” e “Movimentações” ao painel, ativar o botão “Registrar Movimentação” e apresentar os indicadores mensais previstos; validar hierarquia da referência, responsividade desktop/mobile e estados de carregamento, erro e vazio com dados demonstrativos e testes de componente.
- [x] 1.2 Criar o formulário visual de cadastro/edição conforme as duas partes da referência, usando seleção de espécie no lugar de lote e campos condicionais por tipo; validar acessibilidade, mensagens de campo, estados de envio e regras de quantidade/destino com testes de componente.
- [x] 1.3 Criar a tabela visual do histórico com tipo, espécie, quantidade, data, destino/responsável, busca/filtros e ação de edição; validar navegação ao formulário, resposta vazia, carregamento e erro com testes de componente.

## 2. Persistência e regras de estoque

- [x] 2.1 Adotar Flyway para PostgreSQL, criar baseline inicial do esquema atual completo, substituir `ddl-auto=update` por `validate` e documentar o procedimento de baseline para banco existente; verificar inicialização de banco limpo, validação do schema e preservação dos dados existentes.
- [x] 2.2 Criar migration aditiva, entidade, enum, DTOs e repositório de movimentações vinculados à espécie, incluindo vínculo opcional de comprovante; verificar constraints, índices e compatibilidade das migrations em PostgreSQL.
- [x] 2.3 Implementar criação e edição transacionais com bloqueio de espécie e aplicação do delta de estoque; testar entradas, saídas, perdas, edição de tipo/espécie/quantidade, saldo insuficiente, concorrência e rollback sem alteração parcial.
- [x] 2.4 Implementar upload, consulta e substituição de comprovante privado no MinIO com limite de 10 MB e validação de extensão/MIME; testar formatos permitidos, arquivo inválido, falha de upload, falha de persistência e limpeza do objeto substituído.

## 3. API e autorização

- [x] 3.1 Criar endpoints autenticados para lista filtrada/paginada, detalhe, criação, edição, resumo mensal e comprovante, retornando ProblemDetail para erros; testar contratos HTTP, filtros, totais mensais, validações e recursos inexistentes.
- [x] 3.2 Autorizar as rotas somente para os mesmos perfis de escrita do Viveiro, atualizar a allowlist de bolsistas e adicionar rotas/navegação no frontend; testar os dois aliases de bolsista, perfis existentes, visitante anônimo e acessos por URL/API sem alterar permissões não relacionadas.

## 4. Integração e regressão

- [x] 4.1 Integrar formulário, histórico e resumo mensal ao service/store, atualizando estoque e interface após salvar sem recarregar a página inteira; verificar estados de sucesso, erro, vazio, filtro e edição com testes de frontend.
- [x] 4.2 Executar a suíte completa do frontend, `vue-tsc`, build de produção e testes do backend; confirmar inicialização com Flyway/`ddl-auto=validate` em banco novo e existente, ausência de saldo negativo e preservação dos fluxos de espécies, dashboard e perfis não bolsistas.
