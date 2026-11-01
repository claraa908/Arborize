# Tasks — Autenticação, Sessão e Edição Segura

## Implementação

### Backend (Spring Boot + Spring Security)
- [x] **T01 — Configurar Spring Security e JWT**
    - Adicionar dependências do `spring-boot-starter-security` e `java-jwt` (ou `jjwt`).
    - Criar classe de configuração de segurança (`SecurityConfigurations`) para bloquear todas as rotas de edição e liberar apenas as rotas públicas (`/auth/login`, `/usuarios/cadastro`).
    - **Relacionada a:** HU07, RN07

- [x] **T02 — Serviço de Autenticação e Geração de Token**
    - Criar o `TokenService` para gerar e validar o JWT com expiração de 24h.
    - Implementar a rota `POST /auth/login` que verifica a senha via BCrypt e devolve o Token.
    - **Relacionada a:** HU04, RN05, RN06

- [x] **T03 — Refatorar Endpoints de Perfil para usar a Sessão**
    - Criar o Filtro de Segurança (`SecurityFilter`) para ler o header `Authorization`.
    - Alterar as rotas de `GET /usuarios/{id}` para `GET /usuarios/me` e `PUT /usuarios/me`. O Controller deve extrair o ID do usuário diretamente do `SecurityContextHolder` (da sessão), ignorando IDs enviados pelo front.
    - **Relacionada a:** HU07, RN08

### Frontend (Vue.js)
- [x] **T04 — Implementar Modal de Login**
    - Criar a requisição Axios para `/auth/login`. Capturar o Token retornado e salvar no `localStorage` e num estado global (Pinia ou ref global).
    - **Relacionada a:** HU04

- [x] **T05 — Configurar Axios Interceptors (A Mágica da Sessão)**
    - Configurar o ficheiro do Axios (ex: `api.ts`) para intercetar todas as requisições antes de saírem. Se existir um token no `localStorage`, adicionar automaticamente o cabeçalho `Authorization: Bearer <token>`.
    - **Relacionada a:** HU05, HU07

- [x] **T06 — Refatorar Requisições da Tela de Perfil**
    - Alterar o `usuarioService.ts` para bater em `/usuarios/me` ao invés de enviar um ID explícito.
    - Implementar o Router Guard (`beforeEach`) no `vue-router` para impedir que usuários deslogados acessem a página `/perfil`.
    - **Relacionada a:** HU07

- [x] **T07 — Implementar o Logout**
    - No Navbar, a função `fazerLogout()` deve apagar o token do `localStorage` e forçar o redirecionamento (`router.push('/')`).
    - **Relacionada a:** HU06

## Verificação

- [ ] **T08 — Verificar: Bloqueio de Segurança**
    - Tentar enviar um `PUT /usuarios/me` pelo Postman/Insomnia sem o token no cabeçalho. A resposta deve ser estritamente `403` ou `401`.
- [ ] **T09 — Verificar: Fluxo de Login**
    - Logar pelo frontend, confirmar se o token foi parar no Application Storage do navegador, e se a requisição de carregar o perfil usou esse token com sucesso.
- [ ] **T10 — Verificar: Persistência de Sessão**
    - Fazer login, atualizar a página (F5) com o navegador e confirmar que o usuário continua logado e o Navbar mostra o ícone de perfil em vez do botão "Entrar".

## Pronto quando
- [ ] O Spring Security está a proteger ativamente as rotas da API.
- [ ] O Vue.js gere inteligentemente o estado do Token e injeta nos cabeçalhos.
- [ ] É impossível um utilizador editar ou visualizar dados de terceiros, pois a edição só obedece à identidade validada pelo Token.
