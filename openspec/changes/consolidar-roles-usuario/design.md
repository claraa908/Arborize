# Design

## Context

Consulte `proposal.md` para a motivação e `specs/role-based-access/spec.md` para o comportamento contratado. Hoje `UsuarioRole` declara oito valores, o `SecurityConfig` e filtros aceitam aliases em conjunto, serviços consultam ambos os papéis IFCE, e o frontend aceita e apresenta os oito. `TokenService` valida a autoridade do JWT contra o enum. A coluna `usuarios.role` tem um `CHECK` no baseline Flyway V1; V2 já está aplicada no fluxo atual, portanto migrations existentes devem permanecer imutáveis.

Há ainda uma proposta de restrição do gestor IFCE em andamento que diz preservar `ROLE_IFCE_GESTOR`. A consolidação torna `IFCE_GESTOR` uma representação legada de `GESTOR_IFCE`, então a regra em andamento deve deixar de tratar os nomes como perfis distintos.

## Goals / Non-Goals

**Goals:**

- Manter os cinco valores canônicos na persistência, tokens, autorização e estado do frontend.
- Converter contas antigas de forma rastreável sem alterar identidade, credenciais ou demais dados.
- Preservar as regras dos papéis canônicos; contas legadas passam a usar a política do papel de destino.

**Non-Goals:**

- Redesenhar a matriz geral de permissões ou introduzir novos perfis.
- Remover referências históricas de migrations já aplicadas ou de mudanças OpenSpec concluídas; essas referências explicam a origem dos dados legados.

## Decisions

### Normalizar os dados em uma nova migration Flyway

Criar uma migration após V2 que remova temporariamente a restrição antiga de `usuarios.role`, converta `AMMA` para `FUNCIONARIO_AMMA`, `IFCE_GESTOR` para `GESTOR_IFCE` e `IFCE_BOLSISTA` para `BOLSISTA_IFCE`, e reinstale a restrição permitindo somente os cinco valores canônicos. A migration deve executar como uma única transação quando suportado pelo PostgreSQL. Não alterar V1: Flyway depende da imutabilidade dos scripts já registrados.

Atualizar primeiro os registros e depois instalar a nova restrição evita que os valores legados violem o contrato final. O identificador, senha e demais campos não participam do `UPDATE`.

### Remover aliases do contrato da aplicação

Manter `UsuarioRole` como enum persistido em texto, mas com somente os cinco nomes canônicos. Atualizar as regras Spring Security, o filtro de bolsista e as consultas dos serviços para referenciar apenas os valores resultantes do mapeamento. Atualizar `USER_ROLES`, os mapas de rótulo e os tipos de respostas do frontend da mesma forma. Manter o formato atual de autoridade `ROLE_<PAPEL>`.

Não manter uma tradução permanente de authorities antigas em `fromAuthority`: ela perpetuaria nomes que deixam de existir no contrato. Como tokens duram até 24 horas, tokens antigos com authorities removidas serão recusados após a implantação; o usuário deverá autenticar de novo e receber token canônico.

### Limpar sessões locais cujo papel não é mais reconhecido

Ao carregar uma sessão local, validar o papel contra a lista canônica. Se inválido, limpar token, nome e papel armazenados e seguir o fluxo de usuário não autenticado. Isso impede que uma interface com papel nulo continue carregando credenciais antigas.

### Tratar cada alias pela política do papel canônico

As equivalências são `AMMA` → `FUNCIONARIO_AMMA`, `IFCE_GESTOR` → `GESTOR_IFCE` e `IFCE_BOLSISTA` → `BOLSISTA_IFCE`. Uma conta antiga `IFCE_GESTOR` passará a ter exatamente a política vigente para `GESTOR_IFCE`, incluindo a restrição específica do gestor IFCE quando a mudança em andamento for aplicada. Isso resolve a distinção anterior entre os dois nomes sem criar uma segunda política.

Alternativa considerada: conservar alias só na validação de tokens ou em filtros para reduzir interrupções. Rejeitada porque manteria roles removidas em runtime e permitiria persistir sessões com autoridades fora do conjunto solicitado.

### Atualizar testes e documentos vivos

Substituir testes que usam aliases como papéis ativos por casos canônicos e cobrir a migração de dados, o conjunto aceito pelo frontend, a limpeza de sessão e as permissões dos cinco papéis. Atualizar o diagrama de classes. Reconciliar os artefatos ainda não concluídos de `restringir-acesso-gestor-ifce`; preservar textos de mudanças concluídas como registro histórico.

## Risks / Trade-offs

- [Usuários com JWT antigo perdem a sessão antes do vencimento normal] → Limpar sessão local quando o papel salvo for inválido e conduzir o usuário ao login; o login em seguida emite papel canônico.
- [Contas `IFCE_GESTOR` passam a seguir as permissões de `GESTOR_IFCE`] → Atualizar a proposta em andamento do gestor e verificar os cenários de acesso antes de concluir a implementação.
- [Uma migration mal ordenada pode falhar ao alterar o `CHECK`] → Validar nomes/definição da constraint V1 e testar a migration sobre banco PostgreSQL migrado por Flyway antes de implantação.
- [Rollback de dados não recupera automaticamente qual valor legado cada conta possuía] → Fazer backup antes da migration; o rollback de código pode continuar lendo valores canônicos, e recuperar a distinção anterior exige restaurar o backup ou uma conversão baseada nele.

## Migration Plan

1. Fazer backup do banco e confirmar que Flyway reconhece V1 e V2 como aplicadas.
2. Aplicar a nova migration, normalizar as contas e conferir que nenhum valor fora dos cinco canônicos permaneceu.
3. Implantar backend e frontend atualizados; a inicialização aplica Flyway antes da validação do schema pelo Hibernate.
4. Validar login com contas migradas dos três aliases, autoridade dos novos tokens, autorização por perfil e descarte de sessões locais antigas.
5. Se a implantação de código precisar ser revertida, manter os valores canônicos no banco, que também são aceitos pelo código anterior. Para recuperar aliases e sua distinção, restaurar o backup.
