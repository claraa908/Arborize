# Spec Delta

## Purpose

Disponibilizar a página pública institucional "Sobre Nós" da AMMA de Quixadá em `/sobre`, apresentando missão, atuação, parcerias, informações de atendimento e indicadores reais de impacto ambiental consultados publicamente no servidor.

## ADDED Requirements

### Requirement: Acesso público pela navegação superior
O sistema SHALL disponibilizar a página institucional "Sobre Nós" na rota pública `/sobre`, acessível sem autenticação a partir do item "Sobre Nós" na barra de navegação superior pública, mantendo o item destacado como ativo enquanto a página estiver aberta.

#### Scenario: Navegação pelo item Sobre Nós na TopBar
- **WHEN** um visitante ou usuário autenticado clica em "Sobre Nós" na barra de navegação superior (desktop ou menu mobile)
- **THEN** o sistema navega para `/sobre`
- **AND** destaca o item "Sobre Nós" como ativo na barra de navegação

#### Scenario: Acesso direto sem autenticação
- **WHEN** um visitante não autenticado acessa diretamente `/sobre`
- **THEN** o sistema exibe a página normalmente sem redirecionar para login nem exibir modal de autenticação

### Requirement: Conteúdo institucional e atalhos para serviços públicos
O sistema SHALL exibir na página `/sobre` um cabeçalho institucional da Autarquia Municipal do Meio Ambiente de Quixadá, três cartões institucionais (*Nossa missão*, *O que fazemos* e *Parceiros*) e o bloco *Fale com a AMMA* com informações de atendimento presencial em Quixadá/CE e links de navegação para os serviços públicos do portal (*Arborização*, *Eventos* e *Biblioteca Digital*).

#### Scenario: Exibição dos blocos institucionais
- **WHEN** a página `/sobre` é renderizada
- **THEN** o sistema exibe o cabeçalho institucional da AMMA de Quixadá, os três cartões (*Nossa missão*, *O que fazemos*, *Parceiros*) e o bloco *Fale com a AMMA*

#### Scenario: Navegação pelos atalhos de serviços públicos
- **WHEN** o usuário aciona um dos atalhos no bloco *Fale com a AMMA* (*Arborização*, *Eventos* ou *Biblioteca Digital*)
- **THEN** o sistema navega para a respectiva rota pública do portal (`/arborizacao`, `/eventos` ou `/biblioteca`)

### Requirement: Endpoint público de indicadores ambientais
O sistema SHALL expor o endpoint público somente leitura `GET /api/sobre/indicadores` que retorna os valores agregados não negativos `mudasDoadas`, `estoqueViveiro`, `garrafasPetArrecadadas` e `ninsSubstituidos` consultados diretamente nas tabelas existentes do banco de dados, sem exigir autenticação.

#### Scenario: Consulta pública bem-sucedida dos indicadores
- **WHEN** um cliente realiza `GET /api/sobre/indicadores` sem token de autenticação
- **THEN** o servidor responde com status HTTP `200 OK` e payload JSON contendo os quatro campos numéricos (`mudasDoadas`, `estoqueViveiro`, `garrafasPetArrecadadas` e `ninsSubstituidos`)

#### Scenario: Banco sem registros para um ou mais indicadores
- **WHEN** não houver registros correspondentes em uma ou mais tabelas consultadas
- **THEN** o servidor retorna `0` nos respectivos campos numéricos em vez de `null` ou erro

### Requirement: Exibição e estados dos indicadores na página Sobre Nós
O sistema SHALL consultar `GET /api/sobre/indicadores` ao abrir `/sobre`, formatando os quatro valores numéricos em padrão brasileiro (`pt-BR`) nos cartões de impacto (*Mudas doadas*, *Estoque do viveiro*, *Garrafas PET* e *Nins substituídos*), exibindo skeleton durante o carregamento e tratando falhas apenas na seção de indicadores sem ocultar o conteúdo institucional.

#### Scenario: Estado de carregamento dos indicadores
- **WHEN** a página `/sobre` está aguardando resposta de `GET /api/sobre/indicadores`
- **THEN** os blocos institucionais permanecem visíveis
- **AND** a seção de indicadores exibe placeholders de carregamento (skeleton) com indicador acessível

#### Scenario: Exibição dos indicadores carregados
- **WHEN** `GET /api/sobre/indicadores` responde com sucesso
- **THEN** a página exibe os quatro cartões com os valores formatados em `pt-BR`, seus respectivos rótulos de unidade e barra visual proporcional

#### Scenario: Falha ao carregar indicadores
- **WHEN** a requisição `GET /api/sobre/indicadores` falha por erro de rede ou servidor
- **THEN** os blocos institucionais continuam visíveis
- **AND** a seção de indicadores exibe mensagem amigável de erro com botão "Tentar novamente" que refaz a consulta
