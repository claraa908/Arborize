# Design

## Context

Consulte `proposal.md` para a motivação e `specs/viveiro-management/spec.md` para os comportamentos esperados. Hoje, `ViveiroView.vue` carrega uma lista de espécies que já contém a quantidade atual; essa quantidade também aparece no formulário compartilhado de criação e edição. `EspecieRequestDTO` e o mapper aceitam e persistem a quantidade enviada. Não há entidade de lotes nem histórico de movimentações que sustente os indicadores de germinação, perdas e saídas.

## Goals / Non-Goals

**Goals:**

- Aplicar a hierarquia da referência `docs/interface/viveiro.png` com os quatro cartões acima da tabela existente por espécie.
- Calcular o estoque atual com os dados já carregados pelo catálogo e comunicar os outros indicadores como indisponíveis até existir sua fonte de dados.
- Separar estoque de metadados botânicos em toda escrita de espécie, sem perder a quantidade que já está armazenada.
- Manter a ação futura “Registrar Movimentação” visível, mas inativa e acessível enquanto não houver fluxo correspondente.

**Non-Goals:**

- Criar lotes, status de germinação por lote, histórico de perdas/saídas, persistência ou endpoints de movimentação.
- Alterar os campos ou ações de espécie além de remover a quantidade dos formulários de escrita.
- Copiar os registros fictícios da imagem ou criar dados de demonstração no sistema.

## Decisions

### Reutilizar o catálogo de espécies para o cartão de estoque atual

Somar as quantidades das espécies já carregadas pela store, mantendo o valor da tabela como leitura. Isso evita uma nova chamada ou endpoint para um agregado que já pode ser obtido dos dados da tela. O total deve usar formatação numérica local e rótulo explícito em unidades.

Considerou-se criar uma API de indicadores para os quatro cartões, mas três deles não têm fonte de dados: criar esse endpoint agora exigiria inventar regras ou antecipar persistência de movimentações. Esses cartões usam `—` até a futura mudança fornecer os dados; não se usa `0`, que poderia comunicar ausência de ocorrências.

### Preservar a tabela por espécie e adotar apenas a composição visual da referência

Reutilizar a listagem e ações existentes de `AppDataTable`, adicionando os cartões e reorganizando o cabeçalho e a busca de acordo com a referência. A imagem apresenta lotes, datas e tratamentos que o sistema ainda não modela; convertê-la integralmente em tabela por lote ampliaria o escopo e exigiria novos dados. Não adicionar filtros ou colunas que dependam desse modelo inexistente.

### Remover quantidade do contrato de escrita e preservar a leitura

Retirar o campo dos formulários, dos payloads e do DTO de requisição. A entidade continua expondo a quantidade na resposta de leitura. A criação usa o valor inicial zero da entidade; a atualização não mapeia quantidade e mantém o valor persistido, mesmo se um cliente antigo continuar enviando esse campo. Isso protege o estoque no backend, em vez de depender apenas da interface.

Considerou-se somente esconder o campo Vue e deixar a API gravar a quantidade; isso ainda permitiria alteração direta por requisição e não cumpriria a separação do estoque.

### Exibir a movimentação como ação futura desabilitada

Adicionar o botão no cabeçalho, no ponto indicado pela imagem, com estado desabilitado e uma indicação acessível de que a ação ficará disponível em uma futura atualização. Não associar rota, endpoint ou alteração de estado nesta mudança. Assim, o layout prepara o ponto de entrada sem apresentar uma ação que não funciona.

### Tratar carregamento, erro e catálogo vazio

Usar o estado de carregamento existente da store para os cartões e a tabela. Enquanto os dados chegam, mostrar skeletons ou indicadores; se a consulta falhar, usar o feedback global existente e manter a tela recuperável; sem espécies cadastradas, preservar o estado vazio claro da tabela e mostrar estoque atual igual a zero. As métricas sem fonte continuam como `—` em todos esses estados, exceto enquanto o carregamento estiver ativo.

## Risks / Trade-offs

- [Clientes antigos podem tentar escrever quantidade pela API] → Remover a propriedade do DTO e garantir no mapper que atualizações preservam a quantidade persistida; incluir teste para payload legado.
- [`—` pode ser confundido com falha] → Acompanhar o traço com rótulo textual ou explicação acessível de que os dados serão disponibilizados após o registro de movimentações.
- [O botão desabilitado pode parecer uma falha] → Exibir indicação curta de disponibilidade futura e usar o estado disabled nativo, sem simular ação.
- [Uma futura especificação pode definir fonte ou janela temporal diferente] → Deixar germinação, período mensal e critérios de perdas/saídas para a spec de movimentações; não inferir esses valores nesta mudança.

## Migration Plan

Não há mudança de banco. Implantar o backend com a quantidade fora do DTO de escrita e, junto dele, o frontend sem o campo. Dados existentes continuam armazenados e são retornados para leitura. A nova versão do formulário cria espécies com quantidade zero e edições preservam o saldo atual. Para reverter, restaurar conjuntamente o DTO, o mapper e os formulários; não é necessário restaurar dados.
