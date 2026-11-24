# Tasks

## 1. Proteção das rotas e retomada do fluxo no frontend

- [x] 1.1 Exigir autenticação em todas as rotas existentes de Arborização, incluindo assistente, recomendações, solicitação, conversão e acompanhamento; cobrir acesso direto e navegação pública nos testes de `sessionNavigation`.
- [x] 1.2 Permitir abrir o cadastro a partir do modal de login e preservar o destino pendente durante cadastro e login; verificar em testes que o usuário retorna à etapa solicitada após autenticar.
- [x] 1.3 Validar o destino pendente antes dos destinos padrão de perfil e limpar a intenção de autenticação ao fechar os modais; testar destinos válidos, inválidos, externos, não permitidos e fechamento sem login.
- [x] 1.4 Verificar que home, Biblioteca Digital e demais áreas públicas continuam acessíveis sem autenticação e que os redirecionamentos padrão dos perfis continuam funcionando quando não há destino pendente.

## 2. Proteção das APIs do fluxo

- [x] 2.1 Exigir autenticação em POST `/api/assistente-plantio/recomendar`, GET `/api/solicitacoes-muda/conversao` e POST `/api/solicitacoes-muda`, preservando métodos, contratos e permissões dos perfis; validar respostas HTTP 401 sem credencial e o comportamento autenticado em `SecurityAuthorizationTest`.
- [x] 2.2 Verificar que as chamadas do frontend para recomendação, conversão e solicitação enviam a credencial pelo cliente HTTP existente e cobrir o fluxo autenticado integrado sem alterar APIs públicas não relacionadas.

## 3. Verificação integrada

- [x] 3.1 Executar a suíte de testes de navegação/autenticação do frontend e os testes de autorização do backend; confirmar também compilação/tipagem do frontend e compilação do backend.
- [x] 3.2 Verificar em teste integrado o percurso completo: tentativa sem login, alternância para cadastro, login, retomada da rota original e acesso às demais áreas públicas sem autenticação.
