# Design — Autenticação, Sessão e Edição Segura

## Objetivo da experiência
Garantir uma experiência fluida onde o login desbloqueia a navegação privada (Meu Perfil) e o token de sessão atua silenciosamente nos bastidores (cabeçalhos HTTP) para proteger as requisições.

## Arquitetura de Sessão (JWT + Axios)

1. **Geração:** O Spring Security valida o login e gera um JWT.
2. **Armazenamento:** O Vue.js salva o JWT no `localStorage` e o estado do usuário no `Pinia` (ou reatividade padrão).
3. **Injeção:** Um `Axios Interceptor` no frontend injeta automaticamente o cabeçalho `Authorization: Bearer <token>` em todas as requisições subsequentes (como a de salvar o perfil).

## Estrutura da interface e Contratos da API

1. **Modal de Login (Interface)**
- Inputs: Usuário (E-mail ou CPF) e Senha. Botão "Entrar".
- *Endpoint Backend:* `POST /auth/login`
- *Request:* `{"login": "maria@email.com", "senha": "123"}`
- *Response (200 OK):* `{"token": "eyJhbGciOiJIUzI1NiIsInR...", "tipo": "Bearer", "nome": "Maria"}`
- *Response (401/403):* Credenciais inválidas.

2. **Tela "Meu Perfil" (Leitura e Edição Segura)**
- **MUDANÇA DE PARADIGMA:** Não passamos mais o `{id}` na URL, pois isso permite que o usuário A tente editar o usuário B alterando a URL. Usamos a rota `/me`.
- *Endpoint de Leitura:* `GET /usuarios/me`
  - *Header Exigido:* `Authorization: Bearer <token>`
- *Endpoint de Edição:* `PUT /usuarios/me`
  - *Header Exigido:* `Authorization: Bearer <token>`
  - *Request:* `{"nome":"", "whatsapp":"", "endereco":""}`

## Estados da interface

- **Navegação (Navbar):** Alterna reativamente entre [Entrar/Cadastrar] e [Ícone do Perfil/Sair] baseado na presença do Token no frontend.
- **Logout:** Ação de clicar em "Sair" limpa o `localStorage`, reseta o estado do Vue e redireciona para a Tela Inicial.

## Acessibilidade e UX
- Retorno de tecla `Enter` nos inputs de login deve disparar a submissão do formulário.
- Redirecionamento inteligente: Se a pessoa tentar acessar a URL `/perfil` sem estar logada, o roteador do Vue (`vue-router`) deve intercetar a navegação e abrir o modal de login automaticamente.