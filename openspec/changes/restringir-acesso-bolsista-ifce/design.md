# Design

## Contexto

Consulte `proposal.md` para conhecer a motivação e `specs/role-based-access/spec.md` para os requisitos observáveis externamente. O frontend já centraliza as verificações de perfil no Pinia e em `sessionNavigation`; as páginas atuais do Viveiro usam `/especies/**`. No backend, `SecurityConfig` permite amplamente várias rotas de API, e a autenticação JWT é estabelecida por `JwtAuthenticationFilter` antes da execução das regras de autorização.

## Objetivos / Fora do escopo

**Objetivos:**

- Aplicar uma única restrição específica por perfil à navegação Vue, ao acesso direto às rotas e às requisições de API.
- Preservar o fluxo de autorização existente para todos os papéis, exceto os dois aliases de bolsista.
- Permitir ao bolsista autenticado a superfície atual da API do Viveiro e os endpoints estritamente necessários aos fluxos públicos de Arborização e Biblioteca Digital.

**Fora do escopo:**

- Alterar como um usuário recebe ou armazena seu papel, adicionar cadastro de contas ou modificar o banco de dados.
- Adicionar baixa de estoque ou outros endpoints futuros do Viveiro; eles exigem contratos de API próprios e inclusão explícita na lista de permissões.
- Substituir a política de autorização existente para visitantes ou outros perfis autenticados.

## Decisões

### Reutilizar o fluxo atual de perfil e navegação do frontend

Adicionar à store de autenticação uma verificação computada que reconheça os dois aliases de bolsista. Usá-la no destino pós-login existente para sempre abrir o Viveiro e em `sessionNavigation` para permitir explicitamente a rota `home`, as rotas públicas de Arborização e Biblioteca Digital e as rotas existentes de lista, cadastro, detalhes e edição do Viveiro. Redirecionar para o Viveiro tentativas de abrir qualquer outra rota e manter a lista explícita para evitar liberação acidental.

Filtrar `AppSidebar` para mostrar aos bolsistas o item Viveiro já existente, inclusive na navegação móvel. Em `AppHeader`, manter o atalho existente para a página pública inicial e a opção de sair; ocultar o atalho do perfil. A navegação pública pode seguir de `home` para Arborização e Biblioteca Digital, mas outras páginas públicas continuam bloqueadas. Assim, o layout atual é reutilizado sem criar outro layout específico por papel. Considerou-se criar uma árvore de rotas separada, mas isso duplicaria as definições existentes e aumentaria a chance de esquecer páginas futuras do Viveiro.

### Aplicar a restrição de bolsista após a autenticação JWT

Adicionar à cadeia do Spring Security um filtro de requisições que reconheça o papel do usuário após `JwtAuthenticationFilter`. Para uma requisição autenticada com uma das autoridades de bolsista, permitir `/especies/**`; `GET /api/materials`, `GET /api/materials/{id}`, `GET /api/materials/{id}/cover` e `GET /api/materials/{id}/download`; `POST /api/assistente-plantio/recomendar`; `GET /api/solicitacoes-muda/conversao`; `POST /api/solicitacoes-muda`; `POST /auth/login`; e requisições `OPTIONS` de preflight. Responder com HTTP 403 nos demais casos, inclusive em rotas administrativas da Biblioteca e quando uma regra existente marca o endpoint como `permitAll`. Para qualquer outra autoridade ou requisição anônima, manter o fluxo atual de autorização sem alterações.

O serviço atual do Viveiro consulta, cria, atualiza, exclui e carrega imagens por meio de `/especies/**`. Arborização usa o catálogo de espécies, o recomendador e os endpoints de conversão e solicitação de mudas; Biblioteca Digital usa listagem pública, detalhes, capa e download de materiais. A lista de métodos e caminhos permitidos mantém as operações administrativas de materiais bloqueadas. Considerou-se reescrever cada regra de `SecurityConfig`, mas isso poderia alterar silenciosamente o comportamento público ou específico por papel para outros usuários. Aplicar a condição após a autenticação JWT também garante que a regra use a autoridade verificada no token, em vez de um papel informado pelo cliente.

### Tratar os dois valores de papel como um único perfil

O enum persistido e os tipos do frontend já reconhecem `BOLSISTA_IFCE` e `IFCE_BOLSISTA`, e a interface atual identifica ambos como Bolsista IFCE. Aplicar as mesmas restrições aos dois valores sem migrar contas ou tokens existentes. Adicionar um terceiro valor de papel ou consolidar esses aliases está fora do escopo desta mudança.

## Riscos e compensações

- [Uma nova operação do Viveiro ou das áreas públicas permitidas pode usar outro caminho de API] → Incluir método e endpoint exatos na lista de permissões de bolsistas somente junto a um contrato revisado; manter o padrão como negado.
- [Frontend e backend podem ser implantados em momentos diferentes] → Implantar primeiro a restrição do backend e depois a atualização da navegação do frontend. Assim, o backend também protege clientes antigos enquanto o frontend melhora a navegação.
- [Uma verificação ampla demais pode expor páginas não relacionadas sob o prefixo do Viveiro] → Comparar explicitamente os nomes das rotas existentes e cobrir, nos testes de navegação, URLs diretas para todas as rotas que não pertencem ao Viveiro.

## Plano de migração

Não é necessária migração de banco de dados ou de tokens. Implantar primeiro o filtro do backend e, em seguida, as mudanças de rota e navegação do frontend. Se for necessário reverter, desfazer as mudanças do frontend e do backend em conjunto; reverter somente o backend remove a proteção no servidor e não deve ser o estado final.

## Questões em aberto

Nenhuma. Os métodos e caminhos públicos atuais de Arborização e Biblioteca Digital foram identificados junto às chamadas do frontend e permanecem limitados ao uso dessas áreas.
