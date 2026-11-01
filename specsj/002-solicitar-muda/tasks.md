# Tasks — Solicitação de Muda

> Pré-requisito: spec `001-assistente-plantio` com ao menos T01 a T05 concluídas (a espécie selecionada precisa chegar aqui).
> Ordem: **Frontend com dados fictícios (design first)** → **Backend** → **Integração** → **Verificação**. Siga a ordem numérica.
> Antes de começar, leia `docs/padroes-de-projeto.md` e resolva ou assuma explicitamente as "Questões em aberto" de `requirements.md`.
> Caminhos: frontend em `frontend/src/`; backend em `backend/arborize/src/main/java/br/com/arborize/`.
> Dados fictícios devem ser claramente fictícios.

## Frontend (com dados fictícios)

- [ ] **T01 — Rota e tela "Solicitar muda"**
  - Rota nomeada no layout público; cartão da espécie, seletor de quantidade, campos de endereço e identificação e botão "Continuar". Redirecionar ao assistente se não houver espécie.
  - **Relacionada a:** HU01, RN01, RN02

- [ ] **T02 — Validações e máscaras (frontend)**
  - Obrigatórios, máscara e validação de CPF e telefone (utilitários próprios, sem biblioteca nova), limites da quantidade, mensagens junto aos campos e foco no primeiro erro.
  - **Relacionada a:** HU01, RN02, RN03, RN04

- [ ] **T03 — Tela "Conversão Eco Ponto"**
  - Painel verde, resumo e botões "Voltar" e "Enviar Solicitação", preservando os dados ao voltar.
  - **Relacionada a:** HU02, RN08

- [ ] **T04 — Tela "Acompanhar solicitação"**
  - Linha do tempo de 4 etapas com estados concluída, em andamento e bloqueada (com texto) e resumo do pedido, sem botão de comprovante.
  - **Relacionada a:** HU04, RN12

- [ ] **T05 — Store e service da solicitação**
  - Store Pinia com formulário, conversão, resultado do envio e estados por operação; service Axios que preserva status e `ProblemDetail`. Nesta fase o service pode devolver dados fictícios.
  - **Relacionada a:** HU01, HU02, HU03

## Backend

- [ ] **T06 — Entidade, enum e repositório da solicitação**
  - Criar a entidade conforme "Modelo de dados" em `requirements.md`, o enum `AGUARDANDO_RECICLAVEIS` e o repositório, pelo mecanismo de esquema já usado no projeto. Nenhuma outra tabela.
  - **Relacionada a:** HU03, RN07, RN09

- [ ] **T07 — Service: conversão e criação**
  - Cálculo de recicláveis com parâmetro configurável, validação de quantidade máxima e de espécie existente, criação com status inicial e total calculado no servidor. Testes unitários.
  - **Relacionada a:** HU02, HU03, RN02, RN05, RN06, RN07

- [ ] **T08 — Controller, DTOs, validação e erros**
  - `GET /api/solicitacoes-muda/conversao` e `POST /api/solicitacoes-muda` com records, `@Valid`, validador de CPF, mapper e advice próprio em `ProblemDetail`. Resposta sem CPF, telefone e endereço; nada de dados pessoais em logs. Confirmar que as rotas não estão bloqueadas pela configuração de segurança.
  - **Relacionada a:** HU01, HU02, HU03, RN03, RN04, RN09, RN11, RN12

- [ ] **T09 — Documentar configuração**
  - Registrar no README os parâmetros `arborize.solicitacao.garrafas-por-muda` e `arborize.solicitacao.max-mudas`.
  - **Relacionada a:** RN02, RN05

## Integração

- [ ] **T10 — Ligar frontend e API**
  - Conversão vinda do backend, envio da solicitação e tela de acompanhamento montada com a resposta; tratar erro de rede mantendo os dados; botão de envio desabilitado durante a requisição.
  - **Relacionada a:** HU02, HU03, HU04

## Verificação

- [ ] **T11 — Verificar cenário: Pedido completo e envio**
  - Confirmar criação da solicitação com status e número corretos e navegação para o acompanhamento.
  - **Critério relacionado:** cenários 1 e 10

- [ ] **T12 — Verificar cenário: Cálculo da conversão**
  - Confirmar 1, 2 e 3 mudas gerando 10, 20 e 30 garrafas.
  - **Critério relacionado:** cenário 2

- [ ] **T13 — Verificar cenários de validação**
  - Confirmar obrigatórios, CPF/telefone inválidos e limites de quantidade, no navegador e direto na API (`400` em `ProblemDetail`).
  - **Critério relacionado:** cenários 3, 4, 5 e 9

- [ ] **T14 — Verificar cenários de fluxo e falha**
  - Confirmar voltar sem perder dados, acesso sem espécie e falha no envio com dados mantidos.
  - **Critério relacionado:** cenários 6, 7 e 8

- [ ] **T15 — Verificar responsividade, acessibilidade e privacidade**
  - Testar celular e desktop, navegação por teclado e conferir que CPF e telefone não aparecem em logs nem em respostas da API.
  - **Critério relacionado:** critérios de aceitação do design

## Pronto quando

- [ ] Todas as histórias de usuário deste escopo estão implementadas.
- [ ] Todos os critérios de aceitação foram verificados.
- [ ] O fluxo funciona em desktop e mobile.
- [ ] Os estados de sucesso e erro são compreensíveis.
- [ ] Os controles principais são acessíveis pelo teclado.
- [ ] As questões em aberto foram resolvidas ou registradas como decisão.
