# Tasks

## 1. Navegação e páginas do bolsista

- [x] 1.1 Atualizar o estado de autenticação e a guarda de navegação para reconhecer `ROLE_BOLSISTA_IFCE`, abrir o Viveiro após login e redirecionar tentativas para outras rotas; verificar o papel, destinos de retorno e subpáginas permitidas em `sessionNavigation.spec.ts`.
- [x] 1.2 Filtrar o menu lateral para mostrar somente Viveiro e exibir no cabeçalho os atalhos público, perfil próprio e saída ao bolsista; verificar que desktop e navegação móvel ocultam as demais opções e que outros perfis mantêm seus controles atuais.
- [x] 1.3 Exibir ao bolsista o atalho padronizado do cabeçalho para a página pública inicial, permitir somente a rota `home` como exceção pública e manter as demais rotas fora do Viveiro protegidas; verificar o papel canônico e a conservação do redirecionamento pós-login ao Viveiro.
- [x] 1.4 Permitir ao bolsista navegar pelas rotas públicas de Arborização e Biblioteca Digital e seus detalhes/etapas; manter as demais páginas públicas bloqueadas e o destino padrão pós-login no Viveiro.
- [x] 1.5 Permitir a rota institucional de perfil próprio para o bolsista e validar a navegação pelo ícone do cabeçalho; continuar redirecionando quaisquer outras rotas internas para o Viveiro.

## 2. Autorização das APIs

- [x] 2.1 Implementar a regra condicional para `ROLE_BOLSISTA_IFCE` depois da autenticação JWT, permitindo login, preflight CORS, `/especies/**` e a página pública inicial; verificar que outros papéis e requisições anônimas continuam nas regras existentes.
- [x] 2.2 Criar testes de segurança para confirmar HTTP 403 sem execução em endpoints de outras áreas, inclusive endpoints hoje públicos, e acesso funcional aos métodos e imagens usados pelo Viveiro; verificar também a preservação das permissões de visitantes e de perfis não bolsistas.
- [x] 2.3 Liberar apenas os métodos e endpoints públicos consumidos por Arborização e Biblioteca Digital, mantendo operações administrativas de materiais bloqueadas; cobrir os caminhos permitidos e negados no teste de autorização.
- [x] 2.4 Permitir ao bolsista somente `GET /usuarios/me`, `PUT /usuarios/me` e `PUT /usuarios/me/senha`; verificar que outras operações de conta continuam proibidas.

## 3. Verificação integrada

- [x] 3.1 Executar os testes unitários do frontend, `npm run type-check` e `npm run build`; confirmar que o bolsista chega ao Viveiro e não acessa páginas fora dele por URL direta.
- [x] 3.2 Executar os testes Maven do backend e verificar o fluxo completo de resposta 403, autorização de `/especies/**` e comportamento inalterado dos demais perfis.
- [x] 3.3 Verificar o perfil próprio de bolsista no frontend e a autorização dos endpoints de dados e senha no backend para `ROLE_BOLSISTA_IFCE`, preservando as permissões dos demais perfis.

## 4. Navegação pública acessível ao bolsista

- [x] 4.1 Permitir eventos públicos, seus detalhes e os formulários públicos de requerimento/acompanhamento de substituição botânica na allowlist de rotas do bolsista; cobrir cliques e URLs diretas.
- [x] 4.2 Permitir somente GET `/eventos/**` e POST `/api/substituicoes-botanicas` e `/api/substituicoes-botanicas/acompanhar` ao bolsista; manter APIs administrativas de substituição bloqueadas e cobrir na suíte de segurança.
- [x] 4.3 Verificar as mesmas páginas e APIs para cidadão, administrador, funcionário e gestor, garantindo que o reparo não altere o destino pós-login ou outras restrições dessas roles.
