# Tasks

## 1. Backend — Endpoint autenticado de acompanhamentos do cidadão (`GET /usuarios/me/acompanhamentos`)

- [x] 1.1 Adicionar `findByCpfOrderByCriadaEmDesc(String cpf)` em `SolicitacaoMudaRepository` (com `@EntityGraph(attributePaths = "especie")`) e em `SolicitacaoSubstituicaoRepository`, verificando compilação via `mvn test-compile` em `backend/arborize`.
- [x] 1.2 Criar `CidadaoAcompanhamentosResponseDTO`, `CidadaoAcompanhamentoService` (`@Transactional(readOnly = true)`, normalizando CPF do usuário autenticado e usando `SolicitacaoMudaMapper.protocolo`) e o endpoint `GET /usuarios/me/acompanhamentos` em `UsuarioController`, com testes unitários (`CidadaoAcompanhamentoServiceTest`) passando via `mvn test -Dtest=CidadaoAcompanhamentoServiceTest`.

## 2. Frontend — Remoção da Dashboard do Cidadão e redirecionamento pós-login

- [x] 2.1 Remover `frontend/src/views/public/CidadaoDashboardView.vue`, remover a rota `cidadao-dashboard` em `frontend/src/router/index.ts`, remover o botão "Dashboard" de cidadão em `frontend/src/components/layout/TopBar.vue` (desktop e mobile) e atualizar `dashboardRouteName` em `frontend/src/stores/authStore.ts` para `'perfil'`; atualizar e validar `frontend/src/__tests__/sessionNavigation.spec.ts` passando em Vitest.

## 3. Frontend — Serviço de acompanhamentos e novo layout de `MeuPerfil.vue`

- [x] 3.1 Adicionar tipos de acompanhamentos e a função `buscarMeusAcompanhamentos()` (`GET /usuarios/me/acompanhamentos`) em `frontend/src/services/usuarioService.ts` com teste unitário em Vitest validando a chamada e retorno tipado.
- [x] 3.2 Criar `frontend/src/components/profile/CidadaoAcompanhamentosPanel.vue` (filtros *Todos*, *Solicitações de Mudas* e *Substituição de Nim*, cartões com protocolo, status com ícone + texto explícito, resumo, data e ação de acompanhar, além de estados de skeleton, vazio com CTAs e erro isolado com "Tentar novamente") e reestruturar `frontend/src/components/profile/MeuPerfil.vue` com o layout em 2 colunas (painel lateral esquerdo fixo com identidade/resumo/menu + coluna direita interativa para *Meus Acompanhamentos*, *Dados Pessoais* e *Segurança*), cobrindo todos os fluxos com testes em Vitest (`perfilCidadao.spec.ts`).

## 4. Verificação integrada

- [x] 4.1 Executar `mvn test -Dtest=CidadaoAcompanhamentoServiceTest` no backend e `npm run type-check`, `npm run build-only` e `npx vitest run src/__tests__/perfilCidadao.spec.ts src/__tests__/sessionNavigation.spec.ts` no frontend, confirmando tudo passando sem erros.

## Workflow follow-up

- Revisar visualmente `/perfil` em desktop e mobile após login como cidadão.
- Integrar na `integration` e arquivar a change no OpenSpec (`openspec archive`) conforme combinado pela equipe.
