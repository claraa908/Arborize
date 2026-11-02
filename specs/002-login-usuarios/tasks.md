# Tasks — Autenticação Unificada, Sessão e Edição Segura

## Implementação

### Backend (Spring Boot + Spring Security)
- [x] **T01 — Atualizar a Entidade de Usuário com Roles**
    - Adicionar um campo `role` (Enum ou String) na entidade `Usuario` (ex: `CIDADAO`, `ADMINISTRADOR_AMMA` `FUNCIONARIO_AMMA`, `IFCE_GESTOR`, `IFCE_BOLSISTA`). Fazer a entidade implementar `UserDetails` do Spring Security.
    - **Relacionada a:** HU04, RN06

- [x] **T02 — Serviço de Autenticação Unificado e TokenService**
    - Configurar o `UserDetailsService` para buscar o usuário tanto por E-mail quanto por CPF num único método.
    - Atualizar o `TokenService` para inserir a `role` do usuário como uma *claim* dentro do JWT gerado.
    - Alterar o `LoginResponseDTO` para retornar explicitamente o nível de acesso (`role`), facilitando a vida do frontend.
    - **Relacionada a:** HU04, RN05, RN07

- [x] **T03 — Refatorar Endpoints de Perfil para usar a Sessão**
    - Criar o Filtro de Segurança (`SecurityFilter`) para ler o header `Authorization`.
    - Garantir que as rotas `/usuarios/me` extraiam o ID do usuário diretamente do `SecurityContextHolder` da sessão.
    - **Relacionada a:** HU07, RN09

### Frontend (Vue.js)
- [x] **T04 — Implementar Modal de Login Único**
    - Alterar o modal de login para ter apenas E-mail/CPF e Senha, removendo os botões de seleção de perfil.
    - Criar a requisição Axios para `/auth/login`. Capturar o Token e a `role` retornados e salvar no `localStorage` e num estado global (Pinia).
    - **Relacionada a:** HU04

- [x] **T05 — Roteamento Dinâmico por Perfil**
    - Ao receber o sucesso do login, implementar lógica no Vue: usar os papéis reais retornados pela API. A evolução T11–T17 substitui o destino inicial comum do cidadão por seu dashboard público, preservando o dashboard administrativo institucional.
    - **Relacionada a:** HU04, RN06

- [x] **T06 — Configurar Axios Interceptors**
    - Intercetar todas as requisições para injetar `Authorization: Bearer <token>` caso o token exista no `localStorage`.
    - **Relacionada a:** HU05, HU07

- [x] **T07 — Implementar o Logout**
    - A função `fazerLogout()` deve apagar o token e a `role` do `localStorage` e redirecionar (`router.push('/')`).
    - **Relacionada a:** HU06

## Verificação

- [ ] **T08 — Verificar: Diferenciação de Login**
    - Testar login com uma conta de Cidadão e confirmar que a resposta da API traz `ROLE_CIDADAO`.
    - Testar login com conta da AMMA e confirmar que a resposta traz a role correta e o frontend exibe as opções adequadas.
- [x] **T09 — Verificar: Bloqueio de Segurança**
    - Tentar enviar um `PUT /usuarios/me` sem token. A resposta deve ser `403` ou `401`.
- [ ] **T10 — Verificar: Fluxo Dinâmico**
    - Logar com um perfil administrativo e garantir que a interface reaja exibindo botões e acessos que o cidadão comum não vê, mantendo a mesma sessão ativa mesmo após recarregar a página (F5).

> T09 foi validada na API: `PUT /usuarios/me` sem token retornou HTTP 401. T08 e T10 continuam pendentes de validação prática. O build do frontend e a compilação principal do backend passaram; a compilação dos testes Maven está bloqueada por incompatibilidades existentes em `EventosServiceTest.java`.


## Evolução de navegação, dashboard e perfil — 2026-11-02

Enquadramento: detalhamento de HU04–HU08 e RN06/RN08–RN13; reutiliza a spec 003 para o formulário de perfil. Não cria uma spec transversal duplicada nem encerra 001/T19.

### Implementação

- [x] **T11 — Destinos e guards por sessão**
  - Dashboard público do cidadão; dashboard institucional existente; compatibilidade dos caminhos de perfil; destinos internos autorizados após login; persistência e logout sem loops.
  - **Relacionada a:** HU04, HU05, HU06, HU08, RN06, RN10, RN11, RN13; cenários 4, 5, 8 e 9.
- [x] **T12 — Controles de navegação nos layouts existentes**
  - Visitante, cidadão e institucional no site público; Visitar página pública/Voltar ao painel; preservar controles específicos de gestão de usuários e equipe IFCE.
  - **Relacionada a:** HU08, RN10, RN13; cenários 4 e 7–9.
- [x] **T13 — Dashboard simples do cidadão**
  - Nome real da sessão e links públicos/perfil, sem métricas ou registros fictícios.
  - **Relacionada a:** HU08, RN12; cenário 10.
- [x] **T14 — Perfil pessoal compartilhado nos dois layouts**
  - Reutilizar serviços e formulário, sem edição de papel; retirar métricas fixas; manter validações, loading, retry, falha de sessão e campos preservados após erro.
  - **Relacionada a:** HU07, HU08, RN08, RN09, RN12; cenário 6 e spec 003/HU02–HU03.

### Verificação

- [x] **T15 — Regressão automatizada de sessão, menus, rotas e perfil**
  - Visitante; todos os papéis reais; login e destinos; URLs administrativas e de perfil; logout; reload; falhas de perfil; payload sem privilégios; ausência de dados fictícios.
  - **Relacionada a:** cenários 4–10.
- [ ] **T16 — Inspeção no navegador, desktop e celular**
  - Conferir os layouts, navegação por teclado, perfil de cidadão/institucional, visita ao site público e retorno ao painel.
  - **Relacionada a:** cenários 4–10 e design.
- [ ] **T17 — Integração real de login e perfil em infraestrutura descartável**
  - Autenticar contas fictícias por papel; GET/PUT próprios e restrições; distinguir evidência API, navegador e mocks. Não usar bancos persistentes anteriores.
  - **Relacionada a:** cenários 1, 3–9. Aceite de segurança do Inventário permanece em 001/T19.


### Evidências e limites da evolução

Validação em 2026-11-02, no worktree integration local, sem commits ou publicação:

- `npx vitest run --config vitest.session.config.ts`: **36 testes passaram em 3 arquivos**, usando store Pinia, guards/rotas reais, navbar/cabeçalho e formulário compartilhado Vue. A infraestrutura cliente em Node reutiliza `vitest.mapa.config.ts`; não foi instalado jsdom. Tokens, contas e serviços simulados existem somente nos testes. Os testes cobrem os oito valores reais de papel, destinos, compatibilidade de perfil, sessão restaurada/incompleta, logout, menus, erro de leitura/salvamento, payload restrito e preservação do preenchimento.
- Regressão compartilhada: `npx vitest run --environment node src/__tests__/apiFeedback.spec.ts src/__tests__/phone.spec.ts`: **5 testes passaram em 2 arquivos**. Total desta rodada: 41 testes automatizados passando.
- `npm run build-only`: passou. ESLint e Oxlint dos arquivos alterados, sem correção automática: passaram. `npm run type-check` permanece bloqueado apenas pelos três erros anteriores em `BibliotecaDetalhesView.vue` (linhas 53–54) e `BibliotecaFormView.vue` (linha 95); não foram corrigidos nesta evolução.
- API real com Spring Boot local em loopback e PostgreSQL 15/MinIO novos e descartáveis, banco `arborize_navigation_validation`, sem dados institucionais: **43 requisições de validação** após preparar cinco contas fictícias. Login, GET/PUT de perfil e preservação de ID/CPF/e-mail/papel confirmados para cidadão, administrador, funcionário, gestor IFCE e bolsista. Ausência de token e senha incorreta foram rejeitadas. Campos extras de identidade/papel enviados ao PUT não elevaram privilégios. Isso valida API e persistência; não equivale a fluxo completo no navegador.
- Em oito consultas com papel insuficiente a `/admin/usuarios` ou `/ifce/bolsistas`, o backend existente bloqueou com **401 em vez de 403**. A diferença semântica permanece para estabilização do backend; nenhuma permissão ou configuração de segurança foi alterada. Os aliases legados de papéis foram cobertos no frontend, sem criar contas por caminhos que a API não oferece.
- Navegador real, sessão institucional já existente, somente leitura: página pública sem controles de visitante/cidadão, Voltar ao painel para `/sistema`, perfil pessoal em `/sistema/perfil` com dados reais, Visitar página pública e navegação móvel a 375 × 812 conferidos. Também foi conferido foco de teclado no link inicial. A conta e o preenchimento do usuário não foram alterados.
- **T16 segue pendente:** inspeção visual de cidadão/visitante e percurso completo por teclado nos dois layouts. A evidência institucional parcial não encerra esse item.
- **T17 segue pendente:** fluxo integral navegador/frontend/API/PostgreSQL com os diferentes papéis e esclarecimento do status de negação acima. A origem CORS fixa da API não permite simplesmente mudar a porta do Vite para ligar um segundo frontend ao backend descartável; não foi contornada nem alterada. Os testes API não são apresentados como aceite fullstack.
- A configuração CORS preexistente também rejeita a origem `http://127.0.0.1:5173`; a inspeção utilizou `http://localhost:5173`. Esta evolução não corrige CORS nem fecha 001/T19, T08/T10 ou verificações de cadastro/senha da spec 003.

- Os recursos descartáveis desta rodada foram encerrados após a validação; suas credenciais temporárias foram apagadas. Na conferência final os processos originais de frontend/backend já não escutavam; foram restabelecidos com a configuração local da integration, JDBC `jdbc:postgresql://127.0.0.1:55432/arborize_integration_db` e bootstrap administrativo desabilitado. Frontend respondeu 200 em `http://localhost:5173/`; backend respondeu 401 na raiz protegida em `http://localhost:8080/`. Não foram alteradas contas ou configurações versionadas para isso.
- Conferência por hashes: nenhum arquivo anterior perdido, nenhuma alteração nos módulos fora desta evolução e índice Git staged preservado, incluindo todas as renomeações anteriores. Nenhuma operação remota foi executada.
