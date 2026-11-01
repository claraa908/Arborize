# Tasks — Cadastro de Usuários Institucionais

## Implementação

### Backend (Spring Boot + Spring Security)
- [x] **T01 — Criar Endpoint Restrito de Cadastro**
    - Criar o Controller `AdminController` com a rota `POST /admin/usuarios`.
    - Proteger a rota com a anotação `@PreAuthorize("hasAuthority('ADMINISTRADOR_AMMA')")` para garantir que apenas administradores acessem.
    - **Relacionada a:** HU08, HU09, RN10

- [x] **T02 — Lógica de Criação Institucional**
    - Implementar a validação do Payload (reutilizando as regras de unicidade de CPF e E-mail).
    - Garantir que a senha inicial fornecida no JSON seja criptografada com `BCrypt` antes de ser salva na base de dados.
    - Validar se a `role` enviada no payload é estritamente `FUNCIONARIO_AMMA` ou `GESTOR_IFCE` (para evitar que enviem payloads forçando novos administradores).
    - **Relacionada a:** RN11, RN12, RN13

### Frontend (Vue.js)
- [x] **T03 — Proteção de Rotas no Vue Router**
    - No ficheiro `router/index.ts`, adicionar a propriedade `meta: { requiresAdmin: true }` na rota `/gestao`.
    - No `beforeEach`, verificar: se a rota exigir admin e a `role` do usuário logado não for `ADMINISTRADOR_AMMA`, redirecionar para `/` (Home).
    - **Relacionada a:** RN10

- [x] **T04 — Criar Componente `GestaoView.vue`**
    - Criar o layout do painel contendo um cabeçalho e o botão "Novo Usuário".
    - Renderizar a navegação (Navbar) para mostrar o link "Painel de Gestão" *apenas* se o usuário logado for administrador.
    - **Relacionada a:** HU08, HU09

- [x] **T05 — Criar Formulário/Modal de Cadastro Institucional**
    - Montar os campos de texto com máscara (CPF, WhatsApp) e o campo `<select>` para escolher entre `FUNCIONARIO_AMMA` e `GESTOR_IFCE`.
    - Conectar o botão "Salvar" ao Axios chamando o endpoint `POST /admin/usuarios`. A sessão com JWT (configurada na spec anterior) já cuidará de injetar o Token no header.
    - **Relacionada a:** HU08, HU09

## Verificação

- [ ] **T06 — Verificar: Segurança de Rota (Backend)**
    - Realizar uma requisição via Postman para `POST /admin/usuarios` usando o Token de um Cidadão ou Gestor do IFCE. O sistema DEVE retornar `403 Forbidden`.
- [ ] **T07 — Verificar: Segurança de Rota (Frontend)**
    - Logar como Cidadão e tentar digitar a URL `/gestao` diretamente na barra do navegador. O Vue Router DEVE bloquear e redirecionar.
- [ ] **T08 — Verificar: Cadastro Bem-Sucedido**
    - Logar como Administrador da AMMA, cadastrar um Gestor do IFCE. Fazer logout e, em seguida, fazer login usando as credenciais do novo gestor criadas pelo Administrador. O acesso deve ser permitido.

## Pronto quando
- [ ] O Administrador consegue criar contas de Funcionários e Gestores pela interface.
- [ ] O sistema aplica as permissões corretas (roles) no banco de dados com base na escolha do formulário.
- [ ] É tecnicamente impossível para qualquer outro tipo de usuário acessar ou disparar a criação dessas contas.

> T01–T05 implementadas. T06–T08 permanecem pendentes de validação prática com contas autenticadas de cada perfil. `mvn compile` e `npm run build-only` passaram; `npm run type-check` ainda aponta três erros preexistentes nas views da Biblioteca (`BibliotecaDetalhesView.vue` e `BibliotecaFormView.vue`), sem erros nas alterações desta spec.
