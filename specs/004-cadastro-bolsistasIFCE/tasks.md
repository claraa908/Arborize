# Tasks — Cadastro de Bolsistas (Gestor IFCE)

## Implementação

### Backend (Spring Boot + Spring Security)
- [ ] **T01 — Criar Endpoint de Gestão IFCE**
    - Criar o Controller `IfceController` com as rotas `GET` e `POST /ifce/bolsistas`.
    - Proteger listagem e cadastro exclusivamente com `@PreAuthorize("hasRole('GESTOR_IFCE')")`.
    - **Relacionada a:** HU10, RN14

- [ ] **T02 — Lógica de Criação Forçada (Segurança)**
    - Receber o DTO (sem a propriedade role).
    - No serviço, instanciar a entidade e definir explicitamente: `usuario.setRole(BOLSISTA_IFCE)`.
    - Encriptar a senha com BCrypt e salvar.
    - **Relacionada a:** RN15, RN16

### Frontend (Vue.js)
- [ ] **T03 — Proteção de Rotas para o Gestor**
    - No `router/index.ts`, criar a rota `/ifce/equipe`.
    - No `beforeEach`, verificar se o usuário logado possui a role `GESTOR_IFCE` antes de permitir o acesso.
    - **Relacionada a:** RN14

- [ ] **T04 — Interface "Equipe do Laboratório"**
    - Criar o componente `GestaoIfceView.vue`. Renderizar a rota e o menu exclusivamente para `GESTOR_IFCE`.
    - Criar o formulário contendo apenas os dados pessoais e a senha, ocultando totalmente escolhas de perfil.
    - **Relacionada a:** HU10

- [ ] **T05 — Integração com a nova API**
    - Carregar a equipe por `GET /ifce/bolsistas` e conectar o formulário a `POST /ifce/bolsistas`.
    - **Relacionada a:** HU10

## Verificação

- [ ] **T06 — Verificar: Criação Restrita e Segura**
    - Via Postman, autenticar como Gestor IFCE. Enviar um POST para `/ifce/bolsistas` adicionando maliciosamente `"role": "ADMINISTRADOR_AMMA"`. Confirmar no banco de dados que o usuário foi criado rigorosamente como `BOLSISTA_IFCE`.
- [ ] **T07 — Verificar: Acesso Bloqueado para Bolsistas**
    - Tentar acessar `/ifce/equipe` e as rotas GET/POST usando tokens de Bolsista IFCE, Cidadão, Funcionário AMMA e Administrador (a tela deve redirecionar à Home e a API deve responder `403 Forbidden`).
- [ ] **T08 — Verificar: Fluxo Feliz**
    - Pela interface, o Gestor preenche os dados, salva, e o sistema exibe notificação verde de sucesso.

> A implementação compila e o build do frontend passa. T06–T08 permanecem pendentes de validação prática com contas autenticadas. A suíte Maven reporta erros nos slices MVC existentes de Espécies e Eventos por ausência de `TokenService`; o type-check do frontend ainda aponta três erros nas views da Biblioteca.
