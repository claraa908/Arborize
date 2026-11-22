# Spec Delta

## Purpose

Permite registrar, consultar e editar entradas, saídas e perdas de mudas por espécie, mantendo o saldo do Viveiro consistente e oferecendo histórico verificável das alterações.

## ADDED Requirements

### Requirement: Movimentações são vinculadas a espécies, sem lotes
Usuários autorizados MUST registrar movimentações dos tipos Entrada / Germinação, Saída para Plantio e Baixa por Perda, vinculadas a uma espécie existente, sem exigir ou apresentar lote de mudas.

#### Scenario: Registrar movimento por espécie
- **WHEN** o usuário seleciona um tipo, uma espécie, uma quantidade e uma data válidos
- **THEN** o sistema cria a movimentação vinculada à espécie escolhida e não solicita lote

#### Scenario: Espécie inexistente
- **WHEN** a requisição referencia uma espécie inexistente
- **THEN** o sistema rejeita a operação sem criar movimentação ou alterar estoque

### Requirement: Formulário valida os dados da movimentação
O formulário MUST exigir tipo, espécie, quantidade inteira positiva e data válida; Saída para Plantio MUST exigir finalidade/destino, e responsável/equipe, comprovante e observações MUST ser opcionais.

#### Scenario: Dados obrigatórios válidos
- **WHEN** o usuário envia o formulário com os campos obrigatórios válidos
- **THEN** o sistema aceita a movimentação conforme as regras do tipo selecionado

#### Scenario: Quantidade inválida ou destino ausente
- **WHEN** a quantidade não é um inteiro positivo ou uma saída para plantio não tem finalidade/destino
- **THEN** o sistema informa os campos inválidos e não altera o estoque

### Requirement: Criação atualiza o saldo da espécie atomicamente
A criação MUST somar entradas e subtrair saídas para plantio ou baixas por perda do saldo atual da espécie; gravação e atualização do saldo MUST concluir ou falhar em conjunto, sem permitir saldo negativo.

#### Scenario: Entrada aumenta o saldo
- **WHEN** uma entrada válida de 30 mudas é registrada para uma espécie com saldo 20
- **THEN** a movimentação é gravada e o saldo da espécie passa a 50

#### Scenario: Saída ou perda não pode exceder o saldo
- **WHEN** uma saída para plantio ou baixa por perda solicita mais mudas do que o saldo atual
- **THEN** o sistema rejeita a movimentação e mantém inalterados o saldo e o histórico

### Requirement: Edição recalcula o efeito sem duplicar o estoque
Ao editar tipo, espécie ou quantidade, o sistema MUST compensar o efeito anterior e aplicar o novo efeito em uma operação atômica; se o novo saldo ficar negativo, MUST preservar a movimentação e os saldos originais.

#### Scenario: Alteração válida de uma movimentação
- **WHEN** um usuário autorizado altera uma movimentação existente
- **THEN** o sistema atualiza o registro e aplica somente a diferença entre os efeitos antigo e novo ao estoque correspondente

#### Scenario: Edição resultaria em saldo negativo
- **WHEN** a edição de uma saída ou perda deixaria qualquer espécie com saldo negativo
- **THEN** o sistema rejeita a edição e mantém o registro e os saldos anteriores

### Requirement: Histórico permite consultar e editar registros
O painel do Viveiro MUST oferecer uma aba de movimentações com histórico por data, tipo, espécie e quantidade, busca/filtros e ação de edição; a interface MUST manter acesso à lista de espécies.

#### Scenario: Consultar e editar pelo histórico
- **WHEN** o usuário abre a aba Movimentações e escolhe editar um registro
- **THEN** o sistema exibe os dados atuais no formulário e permite salvar a edição conforme as regras de saldo

#### Scenario: Histórico sem registros
- **WHEN** não há movimentações cadastradas
- **THEN** o sistema apresenta estado vazio informativo e mantém disponível a ação de registrar a primeira movimentação

### Requirement: Indicadores refletem o estoque e os totais do mês
O painel MUST refletir o estoque atual e a soma das quantidades de saídas para plantio e perdas registradas no mês-calendário corrente; “Em germinação” MUST continuar como `—` enquanto não houver registro de conclusão dessa etapa.

#### Scenario: Atualização dos indicadores após movimento
- **WHEN** uma movimentação é criada ou editada
- **THEN** o estoque atual e os indicadores mensais correspondentes refletem o saldo e os registros válidos após a operação

#### Scenario: Germinação sem ciclo de conclusão
- **WHEN** o painel é exibido sem um mecanismo que registre a conclusão da germinação
- **THEN** o indicador Em germinação apresenta `—`, sem inferir um saldo a partir do total de entradas

### Requirement: Comprovante opcional tem acesso e conteúdo validados
O sistema MUST aceitar comprovante opcional em PDF, PNG ou JPG de até 10 MB, validar extensão, MIME e tamanho no servidor e disponibilizar o arquivo somente a usuários autorizados.

#### Scenario: Anexar comprovante válido
- **WHEN** o usuário envia um arquivo de formato e tamanho permitidos
- **THEN** o sistema armazena o arquivo de forma privada e associa o comprovante à movimentação

#### Scenario: Rejeitar arquivo inválido
- **WHEN** o arquivo excede 10 MB ou sua extensão/MIME não corresponde aos formatos permitidos
- **THEN** o sistema informa a rejeição e não associa um arquivo inválido à movimentação

### Requirement: Movimentações respeitam as permissões do Viveiro
Consulta, criação e edição de movimentações MUST exigir os mesmos perfis autorizados a escrever espécies no Viveiro, incluindo `ROLE_BOLSISTA_IFCE` e `ROLE_IFCE_BOLSISTA`; visitantes e perfis não autorizados MUST receber acesso negado.

#### Scenario: Bolsista autorizado acessa o fluxo
- **WHEN** um usuário autenticado com qualquer um dos dois papéis de bolsista consulta, registra ou edita uma movimentação
- **THEN** o sistema permite a operação se as validações do movimento forem satisfeitas

#### Scenario: Acesso anônimo ou perfil não autorizado
- **WHEN** um visitante ou usuário sem permissão tenta consultar, criar ou editar movimentações por rota ou API
- **THEN** o sistema nega o acesso sem expor o histórico nem alterar estoque
