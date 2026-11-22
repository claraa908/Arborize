# Spec Delta

## Purpose

Organiza a gestão do Viveiro em uma visão por espécie com um resumo do estoque e indicadores operacionais. Mantém a quantidade de mudas separada da edição dos dados botânicos enquanto o registro de movimentações aguarda uma mudança futura.

## ADDED Requirements

### Requirement: Painel do Viveiro apresenta os indicadores de estoque
A tela de gerenciamento do Viveiro MUST exibir cartões para estoque atual, mudas em germinação, perdas no mês e saídas no mês. O estoque atual MUST corresponder à soma das quantidades atuais de todas as espécies.

#### Scenario: Exibição dos indicadores
- **WHEN** um usuário abre a tela de gerenciamento do Viveiro com os dados carregados
- **THEN** a tela exibe os quatro cartões, mostra a soma do estoque atual em unidades e mostra `—` nos indicadores de germinação, perdas e saídas até existir uma fonte de movimentações

### Requirement: Cadastro e edição de espécie não alteram o estoque
Os formulários de criação e edição de espécie MUST omitir campo de quantidade. Uma nova espécie MUST iniciar com estoque zero e a edição MUST preservar a quantidade já registrada.

#### Scenario: Criação de espécie
- **WHEN** um usuário cadastra uma espécie
- **THEN** a espécie é criada com estoque zero sem solicitar quantidade no formulário

#### Scenario: Edição de espécie
- **WHEN** um usuário salva alterações nos dados de uma espécie existente
- **THEN** o estoque existente permanece inalterado, mesmo que a requisição contenha uma quantidade

### Requirement: Tabela do Viveiro permanece organizada por espécie
A tela MUST manter a listagem por espécie e MUST apresentar a quantidade atual apenas como informação de leitura. A tabela MUST seguir a hierarquia visual da referência `docs/interface/viveiro.png` sem apresentar dados de lotes que não existem no sistema.

#### Scenario: Consulta do catálogo de espécies
- **WHEN** o catálogo do Viveiro é exibido
- **THEN** cada linha representa uma espécie e sua quantidade atual é apresentada sem ação de edição direta do estoque

### Requirement: Ação de movimentação aguarda sua funcionalidade futura
A tela MUST exibir o botão “Registrar Movimentação” como ponto de entrada visual, desabilitado até que uma mudança futura implemente o fluxo de movimentação.

#### Scenario: Botão de movimentação ainda não disponível
- **WHEN** um usuário visualiza o painel antes da implementação do fluxo futuro
- **THEN** o botão “Registrar Movimentação” aparece desabilitado e não navega nem altera o estoque

### Requirement: Estados de carregamento e ausência de espécies são claros
A tela MUST comunicar carregamento, erro e ausência de espécies sem confundir dados indisponíveis com estoque igual a zero.

#### Scenario: Catálogo em carregamento
- **WHEN** o catálogo está sendo carregado
- **THEN** a tela apresenta um indicador de carregamento para os dados e cartões

#### Scenario: Catálogo vazio ou indisponível
- **WHEN** não existem espécies cadastradas ou a consulta falha
- **THEN** a tela apresenta estado vazio ou mensagem de erro apropriada, e não converte indicadores sem fonte em zero
