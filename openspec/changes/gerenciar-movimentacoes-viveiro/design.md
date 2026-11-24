# Design

## Context

Consulte `proposal.md` para a motivação e `specs/viveiro-movements/spec.md` para os comportamentos exigidos. O saldo atual está em `Especie.mudasDisponiveis`; as rotas autenticadas do Viveiro incluem `/especies/**`. O botão “Registrar Movimentação” e os indicadores `—` foram preparados pela mudança em andamento `redesenhar-painel-viveiro`.

O backend não tem scripts Flyway e usa `ddl-auto=update` nos perfis local, teste e produção. Há uma entidade `MudaTransaction`, mas ela só registra quantidade, confirmação e data, sem vínculo com espécie; o Dashboard a usa para agregar doações confirmadas. `DocumentService` já grava documentos no MinIO, enquanto `DocumentEntities` representa anexos de outros domínios.

## Goals / Non-Goals

**Goals:**

- Registrar operações detalhadas por espécie sem exigir lote e manter o saldo coerente com o histórico posterior à implantação.
- Integrar formulário, lista, indicadores e edição ao painel Viveiro e aos papéis que já podem escrever espécies.
- Tornar o esquema de produção e desenvolvimento rastreável por Flyway, preservando dados atuais e validando o modelo com Hibernate.

**Non-Goals:**

- Gerenciar etapas/lotes de germinação ou estimar mudas atualmente em germinação.
- Excluir movimentações, aprovar doações de cidadãos, gerar guias externas ou integrar movimentações às entidades `Plantio` e `MudaTransaction`.
- Criar qualquer dado histórico retroativo; o estoque existente permanece como saldo inicial sem movimentação correspondente.

## Decisions

### Manter um registro de movimento separado do agregado de doações

Criar uma entidade `MovimentacaoMuda`, ligada a `Especie`, com tipo, quantidade positiva, data, destino/finalidade, responsável/equipe, observações e referência opcional ao documento. O saldo continua em `Especie.mudasDisponiveis`; movimentos iniciados após a implantação alteram esse saldo. A entidade existente `MudaTransaction` permanece responsável pelos totais de doações confirmadas do Dashboard: ampliá-la exigiria espécie e semântica que ela não possui e poderia mudar esses relatórios.

Representar os três tipos como enum persistido por nome (`ENTRADA_GERMINACAO`, `SAIDA_PLANTIO`, `BAIXA_PERDA`). Entrada aplica efeito positivo; saída e perda aplicam efeito negativo. A seleção de espécie substitui o seletor de lote da referência. O destino é obrigatório somente em saída para plantio; responsável/equipe e observações são opcionais.

### Aplicar saldo e histórico sob transação e bloqueio de linha

Concentrar criação e edição em serviço transacional. Antes de validar saldo, obter bloqueio pessimista da espécie. Em edição, bloquear as espécies antiga e nova em ordem crescente de ID para evitar deadlock, compensar o efeito antigo e aplicar o novo efeito antes de persistir. Se o resultado de alguma espécie ficar negativo, lançar erro de domínio e fazer rollback da movimentação e de todos os saldos.

Não recalcular todo o estoque somando o histórico: os saldos atuais antecedem esta feature e não têm eventos de origem. O saldo armazenado é a linha de base; cada evento novo aplica uma diferença exatamente uma vez. Isso mantém o estoque existente sem inventar movimentos.

### Expor uma API de histórico, formulário e resumo

Adicionar endpoints sob `/api/viveiro/movimentacoes`:

- `GET` lista paginada com filtros por tipo, espécie e período; `GET /{id}` carrega edição.
- `POST` registra e `PUT /{id}` edita dados e efeito de estoque.
- `GET /resumo` retorna soma de saídas e perdas no mês-calendário corrente.
- `GET /{id}/comprovante` fornece o anexo opcional autorizado.

O resumo soma **quantidades**, não número de registros, usando intervalo mensal inclusivo para `LocalDate`. O estoque atual continua vindo do catálogo de espécies. “Em germinação” continua `—`; um evento de entrada não prova que a muda ainda esteja nessa fase. Calcular esse saldo exigiria um evento de conclusão ou controle de etapas, fora desta mudança.

O painel terá abas “Espécies” e “Movimentações”. O botão preparado na mudança anterior abre o formulário. A lista de movimentos usa `AppDataTable` com busca/filtros e ação de edição; cadastro e edição usam uma página de formulário no padrão da referência. Os cartões e o painel mantêm estados de carregamento, erro e vazio usando feedback existente.

### Validar os comprovantes antes e durante o armazenamento

Aceitar apenas PDF, PNG e JPG até 10 MB. Validar tamanho, extensão e MIME/conteúdo antes do upload; limitar o multipart no servidor e repetir a validação na fronteira HTTP. Reutilizar `DocumentService`/MinIO e manter bucket, caminho e credenciais fora da resposta pública. A relação do comprovante deve apontar para o documento associado à movimentação; substituir um comprovante atualiza a referência e remove o objeto anterior depois que o novo registro estiver persistido. Falhas de limpeza não podem apagar o novo vínculo.

O banco e MinIO não compartilham transação. Enviar o novo objeto antes de gravar a referência e removê-lo em caso de falha no banco; após sucesso, remover o objeto anterior. Se a compensação falhar, registrar erro técnico sem dados pessoais e deixar a referência válida anterior intacta.

### Proteger as rotas novas sem abrir `/api/**`

Exigir os mesmos papéis usados nas operações de escrita do Viveiro: `AMMA`, `ADMINISTRADOR_AMMA`, `FUNCIONARIO_AMMA`, `GESTOR_IFCE`, `IFCE_GESTOR`, `BOLSISTA_IFCE` e `IFCE_BOLSISTA`. Colocar as regras explícitas de `GET`, `POST` e `PUT` antes do matcher amplo de `"/api/**".permitAll()` em `SecurityConfig`; as rotas de consulta também exigem autorização, pois o histórico contém responsáveis e comprovantes.

Adicionar os métodos/caminhos exatos ao `IfceBolsistaAccessFilter` para os dois aliases de bolsista, mantendo o filtro como negação padrão para outras rotas. Acrescentar as rotas internas ao conjunto permitido de `sessionNavigation`. Visitantes não recebem acesso a histórico, resumo ou comprovantes; demais perfis preservam as regras não relacionadas.

Retornar erros estruturados `ProblemDetail`: entrada inválida como 400, recurso inexistente como 404, saldo insuficiente como 409, anexo acima do limite como 413 e autorização por 401/403. O frontend apresenta validação junto ao campo e feedback global sem apagar os valores digitados.

### Adotar Flyway com baseline e validação do Hibernate

Adicionar Flyway para PostgreSQL. Criar uma migration inicial de baseline que represente o esquema atual completo, seguida da migration aditiva da movimentação e do vínculo com documento. A migration da feature terá chave estrangeira para `especies`, enum/string restrito aos tipos aceitos, `CHECK (quantidade > 0)`, índices de data, espécie e tipo e integridade da relação opcional do comprovante.

Em banco novo, Flyway aplica baseline e migration da feature. Em banco existente, a implantação deve verificar o schema, criar o histórico Flyway na versão da baseline sem executar DDL de baseline e, em seguida, aplicar somente a migration aditiva. Só depois os perfis mudam para `ddl-auto=validate`. Não ativar baseline automático que possa marcar um banco vazio como já migrado. Os testes de migrations devem usar PostgreSQL, não um novo banco em memória.

O baseline é apenas de estrutura e não altera nem copia os dados atuais. Não criar eventos retroativos para o saldo existente. Revisar o baseline contra entidades, constraints e schema real antes de habilitar `validate` em produção.

## Risks / Trade-offs

- [Baseline incompleto impede inicialização em banco novo ou com schema divergente] → Comparar a migration com o schema PostgreSQL atual, testar criação limpa e a sequência de baseline de uma base existente antes de trocar `ddl-auto`.
- [Movimentações concorrentes causam saldo negativo] → Serializar alterações por espécie com bloqueio de linha e cobrir concorrência e rollback nos testes.
- [Alterar o tipo ou espécie na edição afeta dois saldos] → Bloquear ambos os IDs em ordem estável e validar todo o resultado antes de concluir a transação.
- [Banco e MinIO podem falhar em momentos diferentes] → Aplicar compensação de upload e limpeza posterior do objeto substituído; preservar sempre a última referência válida.
- [Endpoint sob `/api/**` pode herdar acesso anônimo] → Definir regras autenticadas antes do matcher permissivo e testar chamadas anônimas, perfis autorizados e ambos os papéis de bolsista.
- [O cartão de germinação continuar sem valor pode parecer incompleto] → Manter `—` com a indicação de que o ciclo de germinação ainda não é acompanhado; não exibir entradas acumuladas como saldo atual.

## Migration Plan

1. Conferir e validar a migration baseline completa contra o schema atual; em base existente, executar backup e baseline Flyway na versão correspondente sem executar novamente o DDL inicial.
2. Implantar a migration aditiva de movimentações e comprovantes; confirmar que o estoque existente foi preservado.
3. Habilitar Flyway e `ddl-auto=validate` nos perfis; verificar o startup contra banco novo e banco existente antes de liberar o frontend.
4. Implantar backend com autorização, API e operações transacionais; depois habilitar as rotas e telas do frontend.

Rollback de código deve manter as tabelas e o histórico já gravado. Não executar uma migration destrutiva nem restaurar `ddl-auto=update`; desabilitar o fluxo novo enquanto se corrige uma versão incompatível.
