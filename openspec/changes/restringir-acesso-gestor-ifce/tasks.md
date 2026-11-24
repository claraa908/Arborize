# Tasks

## 1. Navegação e páginas do gestor IFCE

- [x] 1.1 Atualizar o destino pós-login e a guarda de rotas para `ROLE_GESTOR_IFCE`; verificar que o login abre Equipe do IFCE, que todas as rotas públicas existentes são acessíveis e que rotas privadas/admin redirecionam.
- [x] 1.2 Filtrar o menu lateral para Viveiro e Equipe, preservar a navegação pública integral e manter no cabeçalho autenticado home, equipe, perfil e saída; verificar desktop/móvel e controles dos demais papéis.
- [x] 1.3 Cobrir perfil próprio, rotas públicas permitidas e tentativas de rotas privadas/admin não permitidas em testes de navegação para o gestor.

## 2. Autorização das APIs

- [x] 2.1 Aplicar uma allowlist backend específica para `ROLE_GESTOR_IFCE` após JWT, incluindo APIs públicas e APIs do Viveiro, equipe, perfil próprio, login e preflight; verificar que a política de bolsistas continua separada.
- [x] 2.2 Cobrir com testes de segurança endpoints públicos e institucionais permitidos, HTTP 403 para APIs privadas/admin fora da allowlist, contas convertidas e demais papéis canônicos.

## 3. Verificação integrada

- [x] 3.1 Executar testes de navegação, suíte unitária, `npm run type-check` e `npm run build`; confirmar home e destino pós-login definidos.
- [x] 3.2 Executar testes Maven e `openspec validate restringir-acesso-gestor-ifce --strict`; confirmar que as rotas/API permitidas funcionam e as demais permanecem bloqueadas.
