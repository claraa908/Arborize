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
    - Ao receber o sucesso do login, implementar lógica no Vue: se `role == 'CIDADAO'`, vai para a tela inicial comum. Se `role == 'AMMA'`, redireciona para o Dashboard da AMMA.
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
