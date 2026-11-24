# Tasks

## 1. Interface do Viveiro

- [x] 1.1 Redesenhar `ViveiroView.vue` com a hierarquia de `docs/interface/viveiro.png`, mantendo a tabela por espécie; calcular o estoque atual pela soma dos valores carregados e mostrar `—` nos outros três indicadores. Verificar cartões e totais com testes de componente e comparar a tela em desktop e mobile.
- [x] 1.2 Incluir o botão “Registrar Movimentação” desabilitado, com indicação acessível de disponibilidade futura; confirmar em teste que não navega nem altera dados.
- [x] 1.3 Remover a quantidade dos formulários de cadastro e edição, dos tipos e dos payloads do frontend; confirmar por testes que a criação não envia quantidade e a edição não apresenta controle de estoque.

## 2. Contrato de escrita de espécies

- [x] 2.1 Remover a quantidade do DTO de escrita e impedir que o mapper a altere; verificar em testes que novas espécies iniciam em zero e que a atualização preserva o saldo existente mesmo com um campo legado na requisição.

## 3. Verificação integrada

- [ ] 3.1 Executar testes unitários, verificação de tipos e build do frontend, além dos testes do backend; conferir estados de carregamento, erro e catálogo vazio, indicadores sem dados como `—`, leitura do estoque existente e ausência de regressão nos demais campos da espécie.

> Validação pendente: `npm.cmd run type-check` ainda aponta erros em `BibliotecaDetalhesView.vue` (linhas 53–54) e `BibliotecaFormView.vue` (linha 95), fora dos arquivos desta mudança.
