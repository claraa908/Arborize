# Proposal

## Why

A tela atual do Viveiro lista espécies, mas não apresenta um resumo do estoque e dos indicadores operacionais visíveis na referência `docs/interface/viveiro.png`. A quantidade de mudas também pode ser alterada junto aos dados botânicos da espécie, embora o controle de estoque deva ocorrer por movimentações em uma funcionalidade futura.

## What Changes

- Redesenhar a tela de gerenciamento com a hierarquia visual da referência, mantendo a tabela por espécie e os dados já existentes; não introduzir lotes neste escopo.
- Exibir cartões para estoque atual, mudas em germinação, perdas no mês e saídas no mês. Calcular o estoque atual pela soma de `mudasDisponiveis`; exibir `—` nos outros três cartões até que a futura funcionalidade de movimentações forneça dados, sem usar valores fictícios ou apresentar ausência de dados como zero.
- Remover a quantidade dos formulários de cadastro e edição de espécie. Novas espécies começam com estoque zero; atualizar uma espécie preserva seu estoque atual, inclusive quando a API recebe um campo de quantidade.
- Exibir o botão “Registrar Movimentação” como ponto de entrada desabilitado. A ação e o fluxo de registro de entradas, saídas e perdas ficam para uma futura mudança OpenSpec.
- **BREAKING:** deixar de aceitar quantidade inicial ou alterações de estoque nas operações de escrita da espécie; manter o estoque existente disponível somente para leitura nesta mudança.

## Capabilities

### New Capabilities

- `viveiro-management`: Define a apresentação do resumo do estoque e da tabela por espécie, além da separação entre dados botânicos e quantidade de mudas.

### Modified Capabilities

Nenhuma. O projeto ainda não possui especificações base em `openspec/specs/`.

## Impact

- Frontend: tela `ViveiroView.vue`, formulários de espécie, tipos, serviço e estado do Viveiro.
- Backend: contrato de escrita e mapeamento de espécie para iniciar estoque em zero e preservar a quantidade existente durante edições; a quantidade continua exposta em leituras.
- Não requer nova tabela, migration ou endpoint de movimentação. A futura mudança “Registrar Movimentação” fornecerá os dados dos indicadores atualmente sem fonte.
- Referência visual: `docs/interface/viveiro.png`.
