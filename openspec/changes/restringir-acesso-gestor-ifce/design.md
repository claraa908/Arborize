# Design

## Context

Consulte `proposal.md` para a motivação e `specs/role-based-access/spec.md` para o comportamento contratado. Atualmente `sessionNavigation` limita somente bolsistas; gestores IFCE seguem o fluxo de gestão geral e iniciam no dashboard. `AppSidebar` acrescenta Equipe do Laboratório, mas mantém as outras entradas. No backend, `IfceBolsistaAccessFilter` restringe apenas bolsistas, enquanto `SecurityConfig` autoriza `ROLE_GESTOR_IFCE` em várias rotas comuns. O perfil próprio usa `/usuarios/me` e `/usuarios/me/senha`; a equipe IFCE usa GET/POST `/ifce/bolsistas`; o Viveiro usa `/especies/**` e `/api/viveiro/movimentacoes/**`.

## Goals / Non-Goals

**Goals:**

- Aplicar allowlists coerentes nas rotas do frontend e nas requisições HTTP do papel `ROLE_GESTOR_IFCE`, preservando as páginas e APIs públicas disponíveis a visitantes.
- Manter uma rota de destino segura para tentativas bloqueadas e para o login.
- Preservar as permissões dos papéis canônicos diferentes de `ROLE_GESTOR_IFCE`, incluindo `ROLE_BOLSISTA_IFCE`, `ROLE_FUNCIONARIO_AMMA`, `ROLE_ADMINISTRADOR_AMMA`, `ROLE_CIDADAO` e visitantes.

**Non-Goals:**

- Definir o mapeamento ou a migration de aliases legados; isso pertence à mudança `consolidar-roles-usuario`.
- Adicionar operações de edição/exclusão de bolsistas; a tela atual consulta e cadastra.
- Alterar endpoints, banco de dados, identidade visual ou permissões de outros papéis.

## Decisions

### Criar uma política própria para `ROLE_GESTOR_IFCE`

Adicionar a verificação do gestor na guarda de rotas usando a role que já habilita a tela Equipe do IFCE. O destino pós-login e o redirecionamento de rotas não permitidas será `ifce-equipe`. A lista explícita aceita todas as rotas públicas existentes para visitantes (home, Eventos e detalhes públicos, Arborização e seus fluxos públicos, e Biblioteca Digital), páginas e subpáginas do Viveiro, `ifce-equipe` e `perfil-institucional`. Não aceita dashboards ou perfis exclusivos do cidadão, nem páginas administrativas da AMMA. O layout autenticado filtra o menu lateral a Viveiro e Equipe. O cabeçalho mantém home, atalho de equipe, perfil próprio e saída.

Na área pública, a navegação do gestor mantém as mesmas opções acessíveis aos visitantes. O atalho “Voltar ao painel” abre Equipe do IFCE.

Contas legadas `ROLE_IFCE_GESTOR` são convertidas em `ROLE_GESTOR_IFCE` pela mudança `consolidar-roles-usuario`; depois da conversão, todas seguem esta mesma allowlist. Não se mantém uma política separada para a authority removida. A regra restringe tanto as rotas da interface quanto as chamadas ao endpoint de equipe e ao Viveiro.

### Aplicar a allowlist de API após o JWT

Adicionar um filtro backend específico para gestor depois de `JwtAuthenticationFilter`. Ele responde 403 para operações fora dos métodos e caminhos autorizados, inclusive APIs atualmente marcadas como `permitAll`. A allowlist pública contempla GET de `/eventos/**`, `/api/materials` e detalhes públicos de materiais, POST `/api/substituicoes-botanicas` e `/api/substituicoes-botanicas/acompanhar`, POST `/api/assistente-plantio/recomendar`, GET `/api/solicitacoes-muda/conversao` e POST `/api/solicitacoes-muda`. Com ela, também são liberados `/especies/**`; GET/POST/PUT de `/api/viveiro/movimentacoes` e seus subrecursos existentes conforme o método contratado; GET e POST `/ifce/bolsistas`; GET/PUT `/usuarios/me`; PUT `/usuarios/me/senha`; POST `/auth/login`; e OPTIONS de preflight. APIs como dashboard, inventário e administração da Biblioteca permanecem bloqueadas.

Usar filtro próprio, em vez de ampliar `IfceBolsistaAccessFilter`, evita misturar regras de dois papéis distintos. A consulta e edição do perfil permanecem vinculadas à identidade resolvida do token; não são permitidas rotas de perfil por ID.

### Preservar o contrato atual da equipe e do Viveiro

Reutilizar `GestaoIfceView`, `PerfilInstitucionalView` e as telas do Viveiro. A equipe consome GET `/ifce/bolsistas` para carregar a listagem e POST para cadastro. O Viveiro consome `/especies/**` e `/api/viveiro/movimentacoes/**`; não será criada rota adicional. As páginas públicas de Eventos, Arborização e Biblioteca consomem as APIs públicas explicitamente incluídas na allowlist.

## Risks / Trade-offs

- [Uma rota ou chamada necessária pode não estar na allowlist inicial] → Conferir as chamadas das páginas permitidas e cobrir caminhos autorizados e negados em testes de rota e segurança.
- [Uma correspondência ampla pode liberar outra página ou método] → Comparar nomes de rota, verbos e caminhos exatos e manter o padrão como negado.
- [Contas com a role legada podem ter uma sessão com authority antiga] → A mudança `consolidar-roles-usuario` limpa sessões locais incompatíveis e exige novo login, que emite `ROLE_GESTOR_IFCE`; validar a allowlist para essas contas após conversão.

## Migration Plan

Esta restrição não requer migration adicional; a conversão da role legada é coberta por `consolidar-roles-usuario`. Implantar a proteção backend junto à atualização das rotas e menus frontend para que o servidor permaneça a fronteira de segurança mesmo durante uma atualização parcial. Em caso de rollback, reverter filtro e mudanças de navegação do gestor em conjunto.

## Open Questions

Nenhuma. As rotas públicas acessíveis a visitantes são incluídas; o destino pós-login é Equipe do IFCE; `ROLE_GESTOR_IFCE` é a única role de gestor aceita após a consolidação.
