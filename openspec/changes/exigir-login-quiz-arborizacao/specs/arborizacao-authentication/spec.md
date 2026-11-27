# Spec Delta

## Purpose

Define a autenticação exigida para iniciar e utilizar o fluxo público de Arborização, com login ou cadastro e retorno seguro à etapa solicitada após autenticar.

## ADDED Requirements

### Requirement: As ações do fluxo de Arborização exigem autenticação no momento do pedido
O sistema MUST manter públicos `/arborizacao`, `/arborizacao/assistente` e `/arborizacao/recomendacoes`, e MUST exigir sessão autenticada para requerer retirada de Nim, solicitar muda, calcular conversão e acompanhar pedidos, inclusive por URL direta.

#### Scenario: Visitante seleciona Arborização e utiliza o assistente até as recomendações
- **WHEN** uma pessoa sem sessão acessa `/arborizacao`, entra no assistente de plantio e conclui o questionário
- **THEN** o sistema permite preencher o quiz e visualizar as espécies recomendadas sem exigir login antecipado

#### Scenario: Visitante seleciona uma muda recomendada ou solicita retirada de Nim sem sessão
- **WHEN** uma pessoa sem sessão seleciona uma muda em `/arborizacao/recomendacoes` ou clica em "Fazer requerimento" em `/arborizacao`
- **THEN** o sistema permanece na tela atual, exibe o modal de login com aviso contextual do motivo da autenticação e preserva a rota solicitada para depois da autenticação

#### Scenario: Visitante abre diretamente uma rota protegida do fluxo
- **WHEN** uma pessoa sem sessão abre diretamente uma rota protegida (requerimento de Nim, solicitação de muda, conversão ou acompanhamento)
- **THEN** o sistema bloqueia a rota, exibe o modal de login e preserva a rota solicitada para depois da autenticação

#### Scenario: Usuário autenticado abre Arborização
- **WHEN** um usuário autenticado seleciona Arborização ou abre diretamente uma rota permitida do fluxo
- **THEN** o sistema exibe a etapa solicitada, respeitando as permissões atuais do perfil

### Requirement: O modal permite cadastro com login automático e retoma a navegação
O modal de autenticação MUST permitir alternar para cadastro e, após concluir o cadastro, autenticar automaticamente o usuário e continuar na rota permitida de Arborização originalmente solicitada (preservando os query params, como a espécie escolhida).

#### Scenario: Visitante cria uma conta durante a autenticação
- **WHEN** a pessoa seleciona cadastro no modal e conclui o formulário de cadastro
- **THEN** o sistema autentica automaticamente a nova conta criada e redireciona para a rota de Arborização solicitada inicialmente

#### Scenario: Usuário fecha o modal sem autenticar
- **WHEN** a pessoa fecha o modal de login ou cadastro
- **THEN** o sistema permanece na página pública atual sem abrir o fluxo protegido e limpa a intenção pendente para que uma atualização não reabra o modal

#### Scenario: Destino pendente não é permitido para o perfil autenticado
- **WHEN** o usuário conclui login com um destino pendente que sua role não pode acessar
- **THEN** o sistema ignora o destino e aplica o redirecionamento padrão e as restrições atuais dessa role

### Requirement: APIs de requerimento, conversão e solicitação exigem autenticação
O backend MUST exigir uma sessão válida para processar as APIs usadas para enviar requerimento de retirada de Nim, calcular conversão e criar uma solicitação de muda, mantendo pública a recomendação do assistente de plantio.

#### Scenario: Visitante ou usuário autenticado consulta recomendações do assistente
- **WHEN** um visitante ou usuário autenticado envia POST `/api/assistente-plantio/recomendar`
- **THEN** o sistema calcula e retorna as espécies recomendadas conforme o contrato atual

#### Scenario: Usuário autenticado utiliza as APIs de requerimento e solicitação
- **WHEN** um usuário autenticado envia POST `/api/substituicoes-botanicas`, GET `/api/solicitacoes-muda/conversao` ou POST `/api/solicitacoes-muda`
- **THEN** o sistema processa a requisição conforme o contrato atual e as permissões existentes

#### Scenario: Visitante chama diretamente uma API protegida
- **WHEN** uma pessoa sem sessão chama POST `/api/substituicoes-botanicas`, GET `/api/solicitacoes-muda/conversao` ou POST `/api/solicitacoes-muda`
- **THEN** o backend responde HTTP 401 sem processar a operação

### Requirement: Outras áreas públicas permanecem disponíveis sem login
A exigência de autenticação MUST ficar limitada ao fluxo de Arborização e às APIs que o processam, preservando as páginas e APIs públicas não relacionadas.

#### Scenario: Visitante acessa outra área pública
- **WHEN** uma pessoa sem sessão abre a página inicial ou a Biblioteca Digital
- **THEN** o sistema mantém o acesso público existente
