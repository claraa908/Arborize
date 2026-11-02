# Design — Autenticação Unificada, Sessão e Edição Segura

## Objetivo da experiência
Garantir uma experiência fluida onde um único formulário de login serve para todos os usuários. O token de sessão atua silenciosamente nos bastidores para proteger as requisições e orientar o frontend sobre quais menus exibir (ex: Painel da AMMA vs. Meu Perfil do Cidadão).

## Arquitetura de Sessão (JWT + Axios)

1. **Identificação e Geração:** O Spring Security busca o usuário pelo e-mail/CPF, identifica sua `Role` (permissão) e gera um JWT que inclui essa `Role` nas *claims* (dados embutidos no token).
2. **Armazenamento:** O Vue.js salva o JWT no `localStorage` e o estado/nível do usuário no `Pinia` (ou reatividade padrão).
3. **Injeção:** Um `Axios Interceptor` no frontend injeta automaticamente o cabeçalho `Authorization: Bearer <token>` em todas as requisições.

## Estrutura da interface e Contratos da API

1. **Modal de Login Unificado**
- Inputs: Usuário (E-mail ou CPF) e Senha. Botão "Entrar". Remove a necessidade de botões separados para "Login AMMA" ou "Login IFCE".
- *Endpoint Backend:* `POST /auth/login`
- *Request:* `{"login": "admin@amma.gov.br", "senha": "123"}`
- *Response (200 OK):* `{"token": "eyJhbGciOiJIUz...", "tipo": "Bearer", "nome": "João", "role": "ROLE_AMMA"}`
- *Response (401/403):* Credenciais inválidas.

2. **Tela "Meu Perfil" (Leitura e Edição Segura)**
- Rota genérica baseada em quem está acessando.
- *Endpoint de Leitura:* `GET /usuarios/me`
  - *Header Exigido:* `Authorization: Bearer <token>`
- *Endpoint de Edição:* `PUT /usuarios/me`
  - *Header Exigido:* `Authorization: Bearer <token>`
  - *Request:* `{"nome":"", "whatsapp":"", "endereco":""}`

## Estados da interface

- **Navegação (Navbar):** O layout reage à propriedade `role` retornada no login. Se for `ROLE_CIDADAO`, exibe o "Meu Perfil" e Dashboard pessoal. Se for `ROLE_AMMA`, pode exibir links para o "Painel de Gestão".
- **Logout:** Ação de clicar em "Sair" limpa o `localStorage`, reseta o estado do Vue e redireciona para a Tela Inicial.