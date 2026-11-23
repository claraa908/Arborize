# Spec Delta

## Purpose

Define a autenticação exigida para iniciar e utilizar o fluxo público de Arborização, com login ou cadastro e retorno seguro à etapa solicitada após autenticar.

## ADDED Requirements

### Requirement: O fluxo de Arborização exige autenticação
O sistema MUST exigir uma sessão autenticada para exibir as páginas existentes de quiz, recomendações, solicitação de muda, conversão e acompanhamento, inclusive quando acessadas por URL direta.

#### Scenario: Visitante seleciona Arborização na navegação pública
- **WHEN** uma pessoa sem sessão seleciona a opção Arborização
- **THEN** o sistema exibe o modal de login sobre a página pública e não exibe o fluxo até a autenticação

#### Scenario: Visitante abre diretamente uma rota do fluxo
- **WHEN** uma pessoa sem sessão abre diretamente uma rota de quiz, recomendação, solicitação, conversão ou acompanhamento
- **THEN** o sistema bloqueia a rota, exibe o modal de login e preserva a rota solicitada para depois da autenticação

#### Scenario: Usuário autenticado abre Arborização
- **WHEN** um usuário autenticado seleciona Arborização ou abre diretamente uma rota permitida do fluxo
- **THEN** o sistema exibe a etapa solicitada, respeitando as permissões atuais do perfil

### Requirement: O modal permite cadastro e retoma a navegação
O modal de autenticação MUST permitir alternar para cadastro e, após cadastro e login, continuar na rota permitida de Arborização originalmente solicitada.

#### Scenario: Visitante cria uma conta durante a autenticação
- **WHEN** a pessoa seleciona cadastro no modal, conclui o cadastro e entra com a nova conta
- **THEN** o sistema retorna à rota de Arborização solicitada inicialmente

#### Scenario: Usuário fecha o modal sem autenticar
- **WHEN** a pessoa fecha o modal de login ou cadastro
- **THEN** o sistema permanece na página pública sem abrir o fluxo protegido e limpa a intenção pendente para que uma atualização não reabra o modal

#### Scenario: Destino pendente não é permitido para o perfil autenticado
- **WHEN** o usuário conclui login com um destino pendente que sua role não pode acessar
- **THEN** o sistema ignora o destino e aplica o redirecionamento padrão e as restrições atuais dessa role

### Requirement: APIs de recomendação e solicitação exigem autenticação
O backend MUST exigir uma sessão válida para processar as APIs usadas para recomendar espécies, calcular conversão e criar uma solicitação no fluxo de Arborização.

#### Scenario: Usuário autenticado utiliza as APIs do fluxo
- **WHEN** um usuário autenticado envia POST `/api/assistente-plantio/recomendar`, GET `/api/solicitacoes-muda/conversao` ou POST `/api/solicitacoes-muda`
- **THEN** o sistema processa a requisição conforme o contrato atual e as permissões existentes

#### Scenario: Visitante chama diretamente uma API protegida
- **WHEN** uma pessoa sem sessão chama uma das APIs de recomendação, conversão ou criação de solicitação
- **THEN** o backend responde HTTP 401 sem processar a operação

### Requirement: Outras áreas públicas permanecem disponíveis sem login
A exigência de autenticação MUST ficar limitada ao fluxo de Arborização e às APIs que o processam, preservando as páginas e APIs públicas não relacionadas.

#### Scenario: Visitante acessa outra área pública
- **WHEN** uma pessoa sem sessão abre a página inicial ou a Biblioteca Digital
- **THEN** o sistema mantém o acesso público existente
