# Proposal

## Why

Atualmente o cidadão autenticado é redirecionado para uma tela intermediária de dashboard (`/dashboard` — `CidadaoDashboardView.vue`) que funciona apenas como uma lista de 3 links genéricos, enquanto a tela de perfil (`/perfil` — `MeuPerfil.vue`) possui visual defasado (cores fora da paleta oficial, tipografia superdimensionada e sem identidade visual do Arborize) e não permite ao cidadão acompanhar suas solicitações de mudas e substituições botânicas. Unificar a experiência do cidadão em um **Meu Perfil** reestruturado remove um passo desnecessário na navegação e centraliza seus dados e acompanhamentos reais em um só lugar.

## What Changes

- **Remoção da Dashboard do Cidadão:**
  - Remoção da rota `/dashboard` (`cidadao-dashboard`), da view `CidadaoDashboardView.vue` e do botão "Dashboard" do cidadão na `TopBar.vue` (desktop e mobile).
  - Atualização do destino padrão pós-login e fallback do cidadão em `authStore.ts` (`dashboardRouteName`) para `'perfil'` (`/perfil`).
- **Novo endpoint autenticado para acompanhamentos do cidadão:**
  - Criação de `GET /usuarios/me/acompanhamentos` (protegido por JWT, utilizando o CPF do usuário autenticado), retornando em um único payload suas **Solicitações de Mudas** (`solicitacoes_muda`) e suas **Substituições Botânicas — Nim Indiano** (`solicitacoes_substituicao`, incluindo histórico público de eventos em `eventos_solicitacao_substituicao`), ordenadas da mais recente para a mais antiga, sem criar novas tabelas nem migrations Flyway.
- **Reestruturação completa da tela `Meu Perfil` (`/perfil`):**
  - Novo layout moderno e simétrico em **duas colunas com altura fixa ocupando a área visível do container principal** em desktop e rolagem interna apenas no conteúdo (alinhado a `docs/padroes-de-projeto.md`):
    - **Coluna esquerda fixa:** avatar orgânico no padrão do Perfil Institucional, nome, badge `Cidadão`, e-mail, CPF, contadores simétricos de acompanhamentos e navegação interna entre **Meus Acompanhamentos**, **Dados Pessoais** e **Segurança e Senha** (sem atalhos externos).
    - **Coluna direita interativa:** exibe o conteúdo correspondente à seção selecionada — abrindo por padrão em **Meus Acompanhamentos** (com filtros *Todos*, *Solicitações de Mudas* e *Substituição de Nim*, cartões simplificados em acordeão único que se expandem para mostrar todas as etapas do processo até o momento e as próximas etapas bloqueadas, além de skeleton, estado vazio limpo e erro isolado com "Tentar novamente"), **Dados Pessoais** (formulário modernizado com tokens oficiais do Arborize) e **Segurança e Senha** (abertura do `ModalAlterarSenha.vue`).
- **Redirecionamento dos links de acompanhamento de Arborização para o Perfil:**
  - Os botões *Acompanhar requerimento* e *Acompanhar pedido de muda* nas telas de Arborização (e o retorno pós-envio de solicitações do cidadão) direcionam diretamente para `/perfil` já pré-filtrado (`?filtro=MUDAS` ou `?filtro=SUBSTITUICOES` e `?protocolo=...`), eliminando o preenchimento manual de CPF e protocolo para cidadãos logados.
  - Ajuste do botão ativo **Meu perfil** na `TopBar.vue` para preservar o formato `rounded-btn` padronizado.
- **Fora de escopo:** alterações no perfil institucional (`/sistema/perfil`), alterações em rotas/painéis de gestão da AMMA ou IFCE, novas tabelas/colunas no banco de dados e geração/download de comprovante de retirada pelo cidadão.

## Capabilities

### New Capabilities

- `perfil-cidadao`: Experiência unificada do cidadão autenticado em `/perfil`, com redirecionamento pós-login direto para o perfil (sem dashboard intermediária), layout em duas colunas com barra lateral fixa e painel interativo para acompanhamento real de solicitações de mudas e substituições botânicas, edição de dados pessoais e segurança da conta.

### Modified Capabilities

<!-- Nenhuma especificação existente em openspec/specs/ sofre alteração de requisitos. -->

## Impact

- **Backend (Java/Spring Boot):**
  - Novos métodos de consulta por CPF em `SolicitacaoMudaRepository` (`findByCpfOrderByCriadaEmDesc` com `@EntityGraph("especie")`) e `SolicitacaoSubstituicaoRepository` (`findByCpfOrderByCriadaEmDesc`).
  - Novo DTO `CidadaoAcompanhamentosResponseDTO`, novo método/serviço `CidadaoAcompanhamentoService` e novo endpoint `GET /usuarios/me/acompanhamentos` em `UsuarioController` (já coberto por `/usuarios/me/**` `.authenticated()` em `SecurityConfig.java`).
  - Testes unitários em JUnit/Mockito para o serviço e controller.
- **Frontend (Vue 3 / TypeScript):**
  - Remoção de `frontend/src/views/public/CidadaoDashboardView.vue` e da rota `cidadao-dashboard` em `frontend/src/router/index.ts`.
  - Atualização de `frontend/src/stores/authStore.ts` (`dashboardRouteName` do cidadão → `'perfil'`) e `frontend/src/components/layout/TopBar.vue` (remoção do botão Dashboard para cidadão).
  - Atualização de `frontend/src/services/usuarioService.ts` e `frontend/src/types/usuario.ts` (ou novo tipo dedicado) para `buscarMeusAcompanhamentos()`.
  - Reestruturação completa de `frontend/src/components/profile/MeuPerfil.vue` (e subcomponentes de acompanhamentos, se necessário).
  - Atualização de `frontend/src/__tests__/sessionNavigation.spec.ts` e criação de testes em Vitest para o novo `MeuPerfil.vue`.
