# Design

## Contexto

Consulte `proposal.md` para conhecer a motivação e `specs/role-based-access/spec.md` para os requisitos observáveis externamente. O frontend já centraliza as verificações de perfil no Pinia e em `sessionNavigation`; as páginas atuais do Viveiro usam `/especies/**`. No backend, `SecurityConfig` permite amplamente várias rotas de API, e a autenticação JWT é estabelecida por `JwtAuthenticationFilter` antes da execução das regras de autorização.

## Objetivos / Fora do escopo

**Objetivos:**

- Aplicar uma única restrição específica por perfil à navegação Vue, ao acesso direto às rotas e às requisições de API.
- Preservar o fluxo de autorização existente para todos os papéis, exceto `ROLE_BOLSISTA_IFCE`.
- Permitir ao bolsista autenticado a superfície atual da API do Viveiro, o acesso à própria tela de perfil e seus endpoints `GET /usuarios/me`, `PUT /usuarios/me` e `PUT /usuarios/me/senha`, além dos endpoints estritamente necessários aos fluxos públicos de Arborização e Biblioteca Digital.

**Fora do escopo:**

- Alterar como um usuário recebe ou armazena seu papel, adicionar cadastro de contas ou modificar o banco de dados.
- Adicionar baixa de estoque ou outros endpoints futuros do Viveiro; eles exigem contratos de API próprios e inclusão explícita na lista de permissões.
- Substituir a política de autorização existente para visitantes ou outros perfis autenticados.

## Decisões

### Reutilizar o fluxo atual de perfil e navegação do frontend

Adicionar à store de autenticação uma verificação computada para `ROLE_BOLSISTA_IFCE`. Usá-la no destino pós-login existente para sempre abrir o Viveiro e em `sessionNavigation` para permitir explicitamente as rotas públicas de `home`, Eventos, Arborização e Biblioteca Digital, incluindo detalhes de eventos, requerimento e acompanhamento de substituição botânica, as rotas existentes de lista, cadastro, detalhes e edição do Viveiro e a rota institucional de perfil próprio. Redirecionar para o Viveiro tentativas de abrir qualquer outra rota e manter a lista explícita para evitar liberação acidental.

Filtrar `AppSidebar` para mostrar aos bolsistas somente o item Viveiro, inclusive na navegação móvel. Em `AppHeader`, manter os atalhos para a página pública inicial, perfil próprio e saída. O perfil reutiliza a tela institucional existente e não adiciona uma entrada ao menu lateral. A navegação pública pode seguir de `home` para Arborização e Biblioteca Digital, mas outras páginas públicas continuam bloqueadas. Assim, o layout atual é reutilizado sem criar outro layout específico por papel. Considerou-se criar uma árvore de rotas separada, mas isso duplicaria as definições existentes e aumentaria a chance de esquecer páginas futuras do Viveiro.

### Aplicar a restrição de bolsista após a autenticação JWT

Adicionar à cadeia do Spring Security um filtro de requisições que reconheça a autoridade `ROLE_BOLSISTA_IFCE` após `JwtAuthenticationFilter`. Para uma requisição autenticada com esse papel, permitir `/especies/**`; `GET /eventos/**`; `POST /api/substituicoes-botanicas` e `POST /api/substituicoes-botanicas/acompanhar`; `GET /usuarios/me`, `PUT /usuarios/me` e `PUT /usuarios/me/senha`; `GET /api/materials`, `GET /api/materials/{id}`, `GET /api/materials/{id}/cover` e `GET /api/materials/{id}/download`; `POST /api/assistente-plantio/recomendar`; `GET /api/solicitacoes-muda/conversao`; `POST /api/solicitacoes-muda`; `POST /auth/login`; e requisições `OPTIONS` de preflight. Responder com HTTP 403 nos demais casos, inclusive em rotas administrativas da Biblioteca, endpoints de conta diferentes da identidade autenticada e quando uma regra existente marca o endpoint como `permitAll`. Para qualquer outra autoridade ou requisição anônima, manter o fluxo atual de autorização sem alterações.

O serviço atual do Viveiro consulta, cria, atualiza, exclui e carrega imagens por meio de `/especies/**`. Eventos públicos usam métodos GET de `/eventos/**`; o requerimento e o acompanhamento de substituição botânica usam somente POST na raiz e em `/acompanhar`, sem acesso aos endpoints `/gestao/**`. A tela de perfil usa somente os endpoints `/usuarios/me` e `/usuarios/me/senha`, que resolvem o usuário pelo identificador autenticado e não recebem ID de conta no caminho. Arborização usa o catálogo de espécies, o recomendador e os endpoints de conversão e solicitação de mudas; Biblioteca Digital usa listagem pública, detalhes, capa e download de materiais. A lista de métodos e caminhos permitidos mantém as operações administrativas de materiais bloqueadas. Considerou-se reescrever cada regra de `SecurityConfig`, mas isso poderia alterar silenciosamente o comportamento público ou específico por papel para outros usuários. Aplicar a condição após a autenticação JWT também garante que a regra use a autoridade verificada no token, em vez de um papel informado pelo cliente.

### Usar o papel canônico de bolsista

O enum persistido e os tipos do frontend usam `BOLSISTA_IFCE` e a autoridade `ROLE_BOLSISTA_IFCE`. Aplicar as restrições somente a esse papel canônico; aliases legados foram removidos pela consolidação de roles e não devem ser reintroduzidos.

## Riscos e compensações

- [Uma nova operação do Viveiro ou das áreas públicas permitidas pode usar outro caminho de API] → Incluir método e endpoint exatos na lista de permissões de bolsistas somente junto a um contrato revisado; manter o padrão como negado.
- [Frontend e backend podem ser implantados em momentos diferentes] → Implantar primeiro a restrição do backend e depois a atualização da navegação do frontend. Assim, o backend também protege clientes antigos enquanto o frontend melhora a navegação.
- [Uma verificação ampla demais pode expor páginas não relacionadas sob o prefixo do Viveiro] → Comparar explicitamente os nomes das rotas existentes e cobrir, nos testes de navegação, URLs diretas para todas as rotas que não pertencem ao Viveiro.

## Plano de migração

Não é necessária migração de banco de dados ou de tokens. Implantar primeiro o filtro do backend e, em seguida, as mudanças de rota e navegação do frontend. Se for necessário reverter, desfazer as mudanças do frontend e do backend em conjunto; reverter somente o backend remove a proteção no servidor e não deve ser o estado final.

## Questões em aberto

Nenhuma. Os métodos e caminhos públicos atuais de Arborização e Biblioteca Digital foram identificados junto às chamadas do frontend e permanecem limitados ao uso dessas áreas.
