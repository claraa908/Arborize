# Spec Delta

## Purpose

Permitir que a AMMA confira o PET e emita a autorização de retirada, e que o IFCE consulte a solicitação pelo protocolo informado no documento.

## ADDED Requirements

### Requirement: Acesso à gestão de solicitações

O sistema SHALL dar acesso à lista e aos detalhes aos papéis AMMA, ADMINISTRADOR_AMMA, FUNCIONARIO_AMMA, BOLSISTA_IFCE, IFCE_BOLSISTA, GESTOR_IFCE e IFCE_GESTOR. As ações de conferência, aprovação e emissão cabem à AMMA no fluxo operacional. O sistema SHALL preservar a autorização existente de gestão, incluindo a rejeição exclusiva da AMMA. A revisão dos acessos por papel pertence à frente responsável pelas permissões.

#### Scenario: Acesso aos três passos do painel

- **WHEN** um usuário com um dos sete papéis entra no painel
- **THEN** vê “Solicitações” no menu e pode acessar lista e detalhes, inclusive por URL direta

#### Scenario: Bloqueio de acesso administrativo

- **WHEN** uma pessoa sem autenticação ou sem papel autorizado chama um endpoint de gestão
- **THEN** a API responde respectivamente 401 ou 403, sem fornecer dados nem alterar o pedido

#### Scenario: IFCE tenta rejeitar

- **WHEN** um usuário do IFCE abre um pedido pendente ou chama diretamente a operação de rejeição
- **THEN** o painel omite a ação e a API retorna 403 para a tentativa

### Requirement: Documento de retirada emitido pela AMMA

A AMMA SHALL poder emitir um PDF de retirada para uma solicitação aprovada e aguardando retirada. O PDF SHALL usar o protocolo existente, informar a quantidade autorizada e orientar a apresentação no laboratório do IFCE.

#### Scenario: Emissão após conferência

- **WHEN** a AMMA confirma o recebimento do PET e abre o pedido aprovado
- **THEN** pode baixar o documento e entregá-lo ao cidadão para retirada no IFCE

#### Scenario: Solicitação fora da etapa de retirada

- **WHEN** a AMMA tenta emitir o documento para um pedido pendente, rejeitado ou com retirada concluída
- **THEN** a API responde 409 e não emite o PDF

### Requirement: Listagem operacional de pedidos

O painel SHALL listar as solicitações de todos os estados na rota `/sistema/solicitacoes`, com busca por protocolo e ordenação “Mais recentes”. Cada cartão SHALL exibir protocolo, solicitante, espécie, quantidade, contrapartida esperada em PET, selo textual de status e botão “Abrir”.

#### Scenario: Ordem e busca

- **WHEN** o usuário abre a página ou busca pelo protocolo completo, ignorando espaços nas extremidades e diferenças entre maiúsculas e minúsculas
- **THEN** o painel lista os pedidos mais recentes primeiro ou mostra a solicitação que corresponde ao protocolo pesquisado

#### Scenario: Estados da lista

- **WHEN** a consulta está em andamento, volta sem pedidos ou falha
- **THEN** o painel diferencia carregamento, ausência de pedidos ou resultados e falha recuperável

### Requirement: Detalhes da solicitação

O painel SHALL exibir cidadão, protocolo, espécie, quantidade, destino, local de plantio, status e cartão “Contrapartida”. A ação “Registrar Recebimento” SHALL aparecer para pedidos pendentes.

#### Scenario: Abertura e navegação

- **WHEN** o usuário clica em “Abrir” ou acessa `/sistema/solicitacoes/:protocolo`
- **THEN** os detalhes exibem os dados do protocolo selecionado ou informam que ele não existe
- **AND** é possível voltar à lista pelo breadcrumb

#### Scenario: Compatibilidade e privacidade

- **WHEN** o usuário abre um pedido antigo sem local de plantio
- **THEN** o painel exibe “Não informado” no local e continua mostrando os demais dados
- **AND** CPF e telefone ficam ausentes das telas, respostas administrativas e logs

### Requirement: Registro de recebimento e aprovação

O sistema SHALL permitir que um operador da AMMA registre um inteiro de garrafas PET recebidas igual ou maior que o total esperado e aprove um pedido em `PENDENTE_RECICLAVEIS` na mesma operação. O esperado SHALL vir do pedido persistido.

#### Scenario: Conferência e autorização da retirada

- **WHEN** o operador acessa `/sistema/solicitacoes/:protocolo/recebimento` para um pedido pendente
- **THEN** o painel apresenta quantidade esperada, quantidade recebida, material “Garrafas PET”, resumo do pedido e botões “Voltar” e “Confirmar recebimento e aprovar”
- **AND** ao confirmar uma quantidade válida, registra o total recebido, muda o pedido para `APROVADA` e informa o sucesso

#### Scenario: Quantidade insuficiente ou inválida

- **WHEN** a quantidade fica vazia, é fracionária, negativa ou menor que a esperada
- **THEN** a interface orienta a correção junto ao campo e a API recusa a entrada inválida sem persistir recebimento ou aprovar

#### Scenario: Pedido processado ou atualização simultânea

- **WHEN** a API recebe aprovação para pedido fora de `PENDENTE_RECICLAVEIS`, inclusive após operação concorrente ou repetida
- **THEN** rejeita a transição com 409 e conserva o resultado já persistido

### Requirement: Rejeição com motivo

O sistema SHALL permitir que um operador da AMMA rejeite uma solicitação pendente pelo detalhe, mediante motivo curto obrigatório. A rejeição SHALL mudar o status para `REJEITADA` e persistir o motivo.

#### Scenario: Rejeição confirmada

- **WHEN** um operador da AMMA confirma a rejeição com motivo preenchido
- **THEN** a solicitação passa para `REJEITADA`, com motivo acessível no detalhe de gestão

#### Scenario: Motivo vazio ou pedido já processado

- **WHEN** o motivo está ausente, contém apenas espaços, excede 255 caracteres ou o pedido já saiu de `PENDENTE_RECICLAVEIS`
- **THEN** a API sinaliza o campo inválido ou conflito de status e conserva o pedido

### Requirement: Local de plantio informado no assistente

O sistema SHALL preservar a resposta de local de plantio do assistente na criação pelo fluxo existente. Os valores aceitos SHALL ser `CALCADA` e `QUINTAL`, com ausência permitida para compatibilidade.

#### Scenario: Da recomendação ao detalhe

- **WHEN** o cidadão informa Calçada ou Quintal, escolhe a espécie recomendada e envia o formulário
- **THEN** o local é persistido e aparece com o rótulo correspondente no detalhe de gestão

#### Scenario: Criação compatível

- **WHEN** um cliente envia a solicitação sem local, ou envia um valor fora dos aceitos
- **THEN** a ausência é registrada como nula e um valor explícito inválido resulta em erro de campo, respectivamente

### Requirement: Autorização por protocolo e acompanhamento real

O acompanhamento existente SHALL renderizar o status persistido obtido por protocolo e CPF. A autorização de retirada SHALL ficar disponível ao aprovar, usando o protocolo `SOL-AAAA-NNNN` já atribuído ao pedido e sem QR code ou código adicional.

#### Scenario: Pedido aprovado

- **WHEN** o cidadão consulta com protocolo e CPF correspondentes e o pedido está `APROVADA`
- **THEN** a linha do tempo marca a conferência como concluída e mostra “Pronta para retirada. Apresente no IFCE o documento emitido pela AMMA.”

#### Scenario: Demais estados e consulta protegida

- **WHEN** a consulta válida retorna `PENDENTE_RECICLAVEIS`, `REJEITADA` ou `RETIRADA_CONCLUIDA`
- **THEN** o painel público mostra respectivamente espera pelos recicláveis, rejeição ou retirada concluída, sem instrução de retirada pendente
- **AND** uma consulta com CPF incompatível continua retornando o mesmo 404 de um protocolo inexistente

### Requirement: Contrato compartilhado de entrega

O estado inicial SHALL ser `PENDENTE_RECICLAVEIS`, com transição para `APROVADA` ou `REJEITADA`. A SPEC-S3-01 SHALL usar `APROVADA` como precondição e `RETIRADA_CONCLUIDA` como resultado da entrega física, usando o mesmo protocolo.

#### Scenario: Entrega registrada pela SPEC-S3-01

- **WHEN** uma solicitação persistida como `APROVADA` passa a `RETIRADA_CONCLUIDA` pela operação de entrega da SPEC-S3-01
- **THEN** a lista, o detalhe e o acompanhamento mostram a retirada concluída

<!-- Arquivos existentes no escopo desta mudança

- Frontend administrativo: `frontend/src/router/index.ts`, `frontend/src/components/layout/AppSidebar.vue` e `frontend/src/views/auth/SolicitacoesView.vue`.
- Frontend público: `frontend/src/views/public/arborizacao/SolicitarMudaView.vue` para enviar o local do quiz e `frontend/src/views/public/arborizacao/AcompanharSolicitacaoView.vue` para vincular a linha do tempo ao status. `frontend/src/types/solicitacaoMuda.ts`, `frontend/src/services/solicitacaoMudaService.ts` e `frontend/src/stores/solicitacaoMudaStore.ts` acompanham o contrato somente onde necessário.
- Backend: `SolicitacaoMudaController.java`, `SolicitacaoMudaExceptionHandler.java` se necessário para tratar as novas entradas, `SolicitacaoMudaService.java`, `SolicitacaoMudaRepository.java`, `SolicitacaoMudaMapper.java`, `SolicitacaoMuda.java`, `SolicitacaoMudaRequest.java` e `SolicitacaoMudaStatus.java`. `SecurityConfig.java` somente se indispensável para garantir os critérios de acesso.
- Infraestrutura de migração, caso a base compartilhada ainda não esteja integrada: `backend/arborize/pom.xml` e os arquivos `application*.properties` dos perfis PostgreSQL afetados. Os novos scripts Flyway ficam em `backend/arborize/src/main/resources/db/migration/`.
- Testes existentes diretamente relacionados: `SolicitacaoMudaFlowIntegrationTest.java`, `SolicitacaoMudaServiceTest.java` e `SecurityAuthorizationTest.java` se a matriz de autorização for validada nele.
- Orientação do fluxo AMMA–IFCE: `frontend/src/views/public/arborizacao/ConversaoMudaView.vue`.
-->
