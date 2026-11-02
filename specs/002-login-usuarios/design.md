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

## Evolução de navegação por sessão — 2026-11-02

Esta evolução detalha HU08/RN10–RN13 na spec existente de autenticação, em conjunto com o perfil da spec `003-cadastro-cidadaos`. Não altera contratos, papéis ou permissões do backend nem reabre o Inventário congelado.

### Fluxo principal e rotas

1. O visitante acessa qualquer rota pública e pode abrir os modais de entrada/cadastro existentes.
2. Após autenticação, sem destino protegido permitido explícito, `ROLE_CIDADAO` segue para `/dashboard` (`cidadao-dashboard`), dentro de `PublicLayout`; perfis institucionais reconhecidos seguem para `/sistema` (`dashboard`), dentro de `AuthenticatedLayout`.
3. O cidadão acessa `/perfil` (`perfil`); o institucional acessa `/sistema/perfil` (`perfil-institucional`). Links antigos para `/perfil` em sessão institucional redirecionam ao perfil administrativo; o caminho administrativo de perfil em sessão de cidadão redireciona ao perfil público.
4. O cabeçalho administrativo oferece Visitar página pública para `/`. Na navbar pública, o institucional encontra Voltar ao painel para `/sistema` e Sair. Cidadão encontra Dashboard, Meu perfil e Sair; visitante encontra Entrar e Cadastrar-se. Links públicos existentes permanecem.
5. Logout reutiliza a store e remove token, nome e papel, retornando a `/`. Recarregar restaura esses valores antes do primeiro menu/guard. Um papel ausente ou não reconhecido não concede acesso institucional.

### Estrutura, campos e contratos

- Dashboard do cidadão: título, saudação pelo nome da sessão e links para Meu perfil, Arborização e Biblioteca Digital. Não consulta métricas nem simula solicitações; não há estado assíncrono artificial nessa página.
- Perfil pessoal: um componente compartilhado, reutilizando `GET/PUT /usuarios/me` e `PUT /usuarios/me/senha`. Apenas nome, WhatsApp e endereço são enviados no PUT de dados. CPF/e-mail permanecem bloqueados; papel não integra o formulário. Remover indicadores fixos do perfil que não possuam fonte de dados.
- O dashboard administrativo e os controles existentes de administração e equipe IFCE permanecem; `/gestao` continua exclusivo para `ROLE_ADMINISTRADOR_AMMA` e `/ifce/equipe` para `ROLE_GESTOR_IFCE`.
- Valores reais recebidos no frontend: `ROLE_CIDADAO`, `ROLE_ADMINISTRADOR_AMMA`, `ROLE_FUNCIONARIO_AMMA`, `ROLE_AMMA`, `ROLE_GESTOR_IFCE`, `ROLE_IFCE_GESTOR`, `ROLE_BOLSISTA_IFCE`, `ROLE_IFCE_BOLSISTA`. Os aliases existentes não ganham novas permissões específicas nesta evolução.
- Redirecionamento após login aceita somente rota interna existente e autorizada. Sem destino permitido, usa o dashboard correspondente. Guards bloqueiam navegação indevida; não substituem o controle no servidor.

### Estados, responsividade e acessibilidade

- Inicial: navbar coerente com sessão restaurada sincronamente; nenhum painel administrativo exibido para cidadão.
- Perfil carregando: indicador textual e formulário indisponível até os dados reais chegarem.
- Sucesso: formulário com dados da API; salvamento confirmado com toast e nome da sessão atualizado.
- Erro: mensagem local e nova tentativa; falha de salvamento conserva os campos. `401` encerra a sessão inválida e solicita login com retorno ao perfil adequado; `403` informa acesso negado. Sem dados não há formulário vazio fingindo sucesso.
- Navbar adapta links e controles ao espaço disponível, com menu móvel e sem transbordamento. O perfil mantém campos empilhados no celular e em colunas no desktop. O layout administrativo mantém sua estrutura.
- Links e botões com nomes acessíveis, foco visível, navegação por teclado e estados identificados por texto. Não depender apenas de cores ou hover para identificar ações.

### Critérios checáveis

- [ ] Cenários 4–10 de requirements verificados de ponta a ponta com contas fictícias em banco descartável.
- [x] Menus, rotas, reload, logout e perfis cobertos por testes automatizados.
- [ ] Navegação revisada no navegador em desktop e celular; limitações de validação registradas nas tasks.
