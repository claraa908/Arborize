# Design

## Context

Consulte `proposal.md` para a motivação e `specs/arborizacao-authentication/spec.md` para o comportamento contratado. `PublicLayout` já controla os modais de login e cadastro e abre o login quando a guarda envia `auth=required`; o redirecionamento da guarda é armazenado em `redirect`. As rotas de Arborização atualmente não exigem autenticação. O cadastro existente, ao concluir, abre o modal de login. Após login, `postLoginTarget` prioriza o destino padrão de bolsistas e gestores antes de validar `redirect`. No backend, as APIs de recomendação, conversão e criação de solicitação estão explicitamente públicas em `SecurityConfig`.

## Goals / Non-Goals

**Goals:**

- Reutilizar a guarda e os modais existentes para impedir a navegação sem sessão em todas as rotas do fluxo de Arborização.
- Preservar a rota solicitada entre a tentativa de acesso, o cadastro, o login e a retomada do fluxo, respeitando as permissões da role autenticada.
- Exigir sessão também nas APIs que produzem recomendações ou processam conversão e solicitação.
- Manter as demais páginas públicas acessíveis sem login e os destinos padrão de login quando não houver redirecionamento pendente.

**Non-Goals:**

- Associar solicitações existentes ao identificador da conta, criar campos ou alterar o esquema do banco.
- Exigir login para a home, a Biblioteca Digital, o catálogo de espécies público ou outras áreas fora de Arborização.
- Alterar os papéis permitidos ou seus destinos padrão pós-login sem uma rota protegida pendente.

## Decisions

### Reusar a guarda de rotas para proteger o fluxo completo

Manter `/arborizacao`, `/arborizacao/assistente` e `/arborizacao/recomendacoes` públicos e marcar com `requiresAuth` as etapas de requerimento e pedido (`/arborizacao/substituicao-nim`, `/arborizacao/solicitar-muda`, `/arborizacao/conversao` e `/arborizacao/acompanhar`). Ao clicar em "Fazer requerimento" em `/arborizacao` ou selecionar uma muda em `/arborizacao/recomendacoes` sem sessão, a guarda permanece na rota de origem com `auth=required`, `motivo` e `redirect` preenchidos; após login ou auto-login imediato ao concluir o cadastro, o usuário segue diretamente para a rota preservada. No backend, `POST /api/assistente-plantio/recomendar` permanece `permitAll`, enquanto `POST /api/substituicoes-botanicas`, `GET /api/solicitacoes-muda/conversao` e `POST /api/solicitacoes-muda` exigem autenticação.

As roles já autorizadas a navegar por Arborização continuam podendo usar o fluxo. A validação de `sessionNavigation` continua sendo a autoridade do frontend para decidir se a role pode retomar a rota.

### Estender o fluxo atual de modais sem perder o destino

O modal de login oferece uma ação para abrir o cadastro controlado por `PublicLayout`. Ao concluir o cadastro, o comportamento existente abre o login; a query de retorno deve permanecer enquanto os modais alternam. Após o login, validar o destino pendente antes do destino padrão de bolsista/gestor. Um destino só será aceito se for uma rota interna existente e permitida por `sessionNavigation`; destinos externos, inválidos ou proibidos continuam usando o padrão seguro da role.

Ao fechar o modal acionado pela guarda, limpar `auth=required` e `redirect` e permanecer na home pública. Isso impede que uma atualização da página reabra o modal para uma tentativa que a pessoa já cancelou. A abertura manual do login pela navegação pública conserva o comportamento atual.

### Exigir autenticação nos endpoints usados pelo fluxo

Trocar a autorização pública por autenticação válida em POST `/api/assistente-plantio/recomendar`, GET `/api/solicitacoes-muda/conversao` e POST `/api/solicitacoes-muda`. Manter verbos, caminhos, payloads e respostas atuais. Os filtros específicos de bolsista e gestor já incluem essas chamadas em suas allowlists; a mudança não amplia as permissões de API desses papéis nem modifica a disponibilidade pública de APIs não relacionadas.

Uma alternativa seria proteger somente as páginas Vue. Ela não foi escolhida porque chamadas diretas ainda poderiam executar a recomendação ou criar uma solicitação sem sessão.

## Risks / Trade-offs

- [A rota pendente pode ser aberta após autenticação por uma role que não tenha permissão] → validar o destino com a guarda atual antes de navegar e usar o destino padrão quando houver bloqueio.
- [A alternância cadastro-login pode apagar a intenção original] → manter a query de retorno até o sucesso do login ou o fechamento explícito dos modais.
- [Uma API pública usada fora da tela Arborização pode passar a exigir login] → restringir a alteração aos três endpoints listados e cobrir home, Biblioteca e APIs públicas independentes nos testes.

## Migration Plan

Não há migração de banco nem dependências novas. Publicar frontend e backend juntos para que a tela e seus endpoints adotem a mesma exigência de sessão. Em rollback, restaurar as permissões `permitAll` apenas para os três endpoints e remover os metadados/fluxo de modal adicionados às rotas de Arborização.
