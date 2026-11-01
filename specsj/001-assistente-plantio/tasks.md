# Tasks — Assistente de Plantio

> Ordem: **Frontend com dados fictícios (design first)** → **Backend** → **Integração** → **Verificação**. Siga a ordem numérica.
> Antes de começar, leia `docs/padroes-de-projeto.md` e resolva ou assuma explicitamente as "Questões em aberto" de `requirements.md`.
> Caminhos: frontend em `frontend/src/`; backend em `backend/arborize/src/main/java/br/com/arborize/`.
> Dados fictícios devem ser claramente fictícios (ex.: "Muda de Ipê Amarelo - Teste").

## Frontend (com dados fictícios)

- [ ] **T01 — Rotas das telas do assistente**
  - Criar as rotas nomeadas (lazy) no layout público e as views de quiz e de resultados, com o menu "Arborização" ativo.
  - **Relacionada a:** HU01, HU02

- [ ] **T02 — Perguntas orientadas a dados**
  - Arquivo tipado com a lista de perguntas (id, enunciado, texto de apoio, opções com título, descrição e valor). Incluir as perguntas 1, 2, 3 e 10, deixando as 4–9 prontas para inclusão.
  - **Relacionada a:** HU01, RN01

- [ ] **T03 — Tela do quiz**
  - Componentes de pergunta, progresso e opções; botões Voltar/Próxima/Finalizar; validação de resposta obrigatória; preservar respostas ao voltar; acessibilidade (fieldset/legend, foco, aria).
  - **Relacionada a:** HU01, RN01, RN02, RN03

- [ ] **T04 — Tela de espécies recomendadas**
  - Cartões, selo, etiqueta de porte, botões "Selecionar Espécie" e "Refazer Quiz"; estados carregando, vazio e erro inline.
  - **Relacionada a:** HU02, HU03

- [ ] **T05 — Store e service do assistente**
  - Store Pinia com respostas, passo, recomendações, espécie selecionada e estados; service Axios que preserva status e `ProblemDetail`. Nesta fase o service pode devolver dados fictícios. "Selecionar Espécie" navega para o formulário (spec 002).
  - **Relacionada a:** HU03, HU04, RN07, RN08

## Backend

- [ ] **T06 — Conferir o catálogo de espécies**
  - Ler a entidade e o repositório de espécies existentes e listar quais atributos há (porte, nativa/exótica, ativa, foto, nome científico). **Se faltar atributo necessário, parar e pedir decisão.** Não criar tabela nem coluna sem aprovação.
  - **Relacionada a:** HU02, RN04

- [ ] **T07 — Regras de recomendação (Service)**
  - Implementar RN04, RN05 e RN06 em um service, com os parâmetros de adequação fáceis de ajustar. Testes unitários para cada regra.
  - **Relacionada a:** HU02, RN04, RN05, RN06

- [ ] **T08 — Endpoint, DTOs e erros**
  - `POST /api/recomendacoes-especies` com records de entrada e saída, `@Valid`, mapper e advice próprio limitado ao controller, retornando `ProblemDetail`. Confirmar que a rota não está bloqueada pela configuração de segurança.
  - **Relacionada a:** HU02, RN01, RN09

## Integração

- [ ] **T09 — Ligar frontend e API**
  - Trocar os dados fictícios pela chamada real; tratar erro de rede mantendo as respostas; exibir os estados de carregamento, vazio e erro.
  - **Relacionada a:** HU02, HU03

## Verificação

- [ ] **T10 — Verificar cenário: Fluxo completo com recomendação**
  - Confirmar que o quiz completo leva a até 3 cartões corretos.
  - **Critério relacionado:** cenário 1

- [ ] **T11 — Verificar cenário: Calçada estreita com fiação**
  - Confirmar que só aparecem espécies nativas de porte pequeno.
  - **Critério relacionado:** cenário 2

- [ ] **T12 — Verificar cenários de validação, voltar e refazer**
  - Confirmar resposta obrigatória, preservação ao voltar e limpeza ao refazer.
  - **Critério relacionado:** cenários 3, 4 e 6

- [ ] **T13 — Verificar cenários de vazio, erro, seleção e API**
  - Confirmar mensagem de nenhuma espécie, falha da API com respostas mantidas, navegação para o formulário e `400` em `ProblemDetail`.
  - **Critério relacionado:** cenários 5, 7, 8 e 9

- [ ] **T14 — Verificar responsividade e acessibilidade**
  - Testar em celular e desktop e percorrer o quiz só com o teclado.
  - **Critério relacionado:** critérios de aceitação do design

## Pronto quando

- [ ] Todas as histórias de usuário deste escopo estão implementadas.
- [ ] Todos os critérios de aceitação foram verificados.
- [ ] O fluxo funciona em desktop e mobile.
- [ ] Os estados de sucesso e erro são compreensíveis.
- [ ] Os controles principais são acessíveis pelo teclado.
- [ ] As questões em aberto foram resolvidas ou registradas como decisão.
