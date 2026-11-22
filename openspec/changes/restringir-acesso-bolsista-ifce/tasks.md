# Tasks

## 1. Navegação e páginas do bolsista

- [x] 1.1 Atualizar o estado de autenticação e a guarda de navegação para reconhecer `ROLE_BOLSISTA_IFCE` e `ROLE_IFCE_BOLSISTA`, abrir o Viveiro após login e redirecionar tentativas para outras rotas; verificar ambos os papéis, destinos de retorno e subpáginas permitidas em `sessionNavigation.spec.ts`.
- [x] 1.2 Filtrar o menu lateral e os atalhos do cabeçalho para mostrar somente Viveiro e saída da sessão ao bolsista; verificar que desktop e navegação móvel ocultam as demais opções e que outros perfis mantêm seus controles atuais.
- [x] 1.3 Exibir ao bolsista o atalho padronizado do cabeçalho para a página pública inicial, permitir somente a rota `home` como exceção pública e manter as demais rotas fora do Viveiro protegidas; verificar ambos os aliases e a conservação do redirecionamento pós-login ao Viveiro.
- [x] 1.4 Permitir aos dois aliases de bolsista navegar pelas rotas públicas de Arborização e Biblioteca Digital e seus detalhes/etapas; manter as demais páginas públicas bloqueadas e o destino padrão pós-login no Viveiro.

## 2. Autorização das APIs

- [x] 2.1 Implementar a regra condicional para os dois papéis de bolsista depois da autenticação JWT, permitindo login, preflight CORS, `/especies/**` e a página pública inicial; verificar que outros papéis e requisições anônimas continuam nas regras existentes.
- [x] 2.2 Criar testes de segurança para confirmar HTTP 403 sem execução em endpoints de outras áreas, inclusive endpoints hoje públicos, e acesso funcional aos métodos e imagens usados pelo Viveiro; verificar também a preservação das permissões de visitantes e de perfis não bolsistas.
- [x] 2.3 Liberar apenas os métodos e endpoints públicos consumidos por Arborização e Biblioteca Digital, mantendo operações administrativas de materiais bloqueadas; cobrir os caminhos permitidos e negados no teste de autorização.

## 3. Verificação integrada

- [x] 3.1 Executar os testes unitários do frontend, `npm run type-check` e `npm run build`; confirmar que ambos os papéis chegam ao Viveiro e não acessam páginas fora dele por URL direta.
- [x] 3.2 Executar os testes Maven do backend e verificar o fluxo completo de resposta 403, autorização de `/especies/**` e comportamento inalterado dos demais perfis.
