# Spec Delta

## Purpose

Permitir que cidadãos consultem publicamente, sem necessidade de autenticação, os eventos socioambientais confirmados e o histórico de eventos encerrados da AMMA, com filtragem por texto e status e visualização completa dos detalhes de cada evento.

## ADDED Requirements

### Requirement: Acesso público pela navegação superior
O sistema SHALL disponibilizar acesso público à listagem de eventos a partir do item "Eventos" na barra de navegação superior pública, sem exigir autenticação prévia, destacando o item como ativo tanto na listagem quanto na página de detalhes de um evento.

#### Scenario: Navegação pelo item Eventos na TopBar
- **WHEN** um visitante ou cidadão autenticado clica no item "Eventos" na barra de navegação superior (desktop ou menu mobile)
- **THEN** o sistema navega para a rota pública `/eventos`
- **AND** destaca o item "Eventos" como seção ativa na barra de navegação

#### Scenario: Acesso direto sem autenticação
- **WHEN** um visitante não autenticado acessa diretamente `/eventos` ou `/eventos/:id`
- **THEN** o sistema exibe a página pública normalmente sem redirecionar para login nem abrir modal de autenticação

### Requirement: Visibilidade e ordenação de eventos na vitrine pública
O sistema SHALL exibir na listagem pública apenas eventos com status `CONFIRMADO` e `ENCERRADO`, ocultando eventos com status `PLANEJADO` ou `CANCELADO`, e apresentando os eventos `CONFIRMADO` antes dos eventos `ENCERRADO`.

#### Scenario: Filtragem automática de visibilidade pública
- **WHEN** a consulta de eventos retorna registros com status `CONFIRMADO`, `ENCERRADO`, `PLANEJADO` e `CANCELADO`
- **THEN** a listagem pública exibe somente os eventos `CONFIRMADO` e `ENCERRADO`
- **AND** não renderiza nenhum evento `PLANEJADO` ou `CANCELADO`

#### Scenario: Priorização de eventos confirmados na aba Todos
- **WHEN** o filtro de status está em `Todos` e existem eventos `CONFIRMADO` e `ENCERRADO`
- **THEN** os eventos `CONFIRMADO` são listados antes dos eventos `ENCERRADO`

### Requirement: Busca textual e filtro por status na listagem pública
O sistema SHALL permitir ao usuário filtrar a listagem pública por status (`Todos`, `Confirmados`, `Encerrados`) e buscar por texto livre sobre o nome do evento, o local de concentração e o destino final, de forma insensível a maiúsculas e minúsculas.

#### Scenario: Filtro por status Confirmados
- **WHEN** o usuário seleciona o filtro `Confirmados`
- **THEN** a listagem exibe apenas os eventos com status `CONFIRMADO`

#### Scenario: Filtro por status Encerrados
- **WHEN** o usuário seleciona o filtro `Encerrados`
- **THEN** a listagem exibe apenas os eventos com status `ENCERRADO` como histórico

#### Scenario: Busca por nome ou local
- **WHEN** o usuário digita um termo no campo de busca
- **THEN** a listagem exibe apenas os eventos visíveis cujo nome, local de concentração ou destino final contenha o termo informado (ignorando maiúsculas/minúsculas)

#### Scenario: Combinação de busca e filtro de status
- **WHEN** o usuário aplica simultaneamente um termo de busca e um filtro de status
- **THEN** a listagem exibe apenas os eventos que atendem aos dois critérios

### Requirement: Apresentação dos cartões de evento
O sistema SHALL apresentar cada evento da listagem pública em um cartão responsivo e acessível contendo imagem de banner (ou fallback visual quando ausente ou indisponível), badge de status com texto explícito, nome do evento, resumo da descrição, período/horário e local de concentração.

#### Scenario: Renderização de cartão com banner
- **WHEN** um evento possui imagem de banner carregada com sucesso
- **THEN** o cartão exibe o banner, o badge de status com rótulo textual ("Confirmado" ou "Encerrado"), o nome do evento, resumo da descrição, período/horário e local de concentração

#### Scenario: Fallback visual quando banner não existe ou falha ao carregar
- **WHEN** um evento não possui banner ou ocorre erro ao carregar a imagem do banner
- **THEN** o cartão exibe um bloco visual de fallback com ícone temático de evento sem quebrar o layout do cartão

#### Scenario: Navegação para detalhes ao acionar um cartão
- **WHEN** o usuário clica ou aciona via teclado um cartão de evento na listagem
- **THEN** o sistema navega para a página pública de detalhes `/eventos/:id` do evento selecionado

### Requirement: Estados de carregamento, vazio e erro na listagem pública
O sistema SHALL comunicar explicitamente os estados de carregamento, ausência de eventos, busca sem resultados e erro de requisição na listagem pública, sem mascarar falhas de API como lista vazia.

#### Scenario: Estado de carregamento inicial
- **WHEN** a listagem pública está buscando eventos no servidor
- **THEN** o sistema exibe indicador de carregamento (skeleton ou spinner) com texto acessível

#### Scenario: Estado vazio sem eventos publicados
- **WHEN** o servidor responde com sucesso, mas não há nenhum evento `CONFIRMADO` ou `ENCERRADO`
- **THEN** o sistema exibe estado vazio informativo indicando que não há eventos públicos disponíveis no momento

#### Scenario: Estado vazio por filtro ou busca sem correspondência
- **WHEN** existem eventos públicos, mas a combinação de busca e filtro não retorna resultados
- **THEN** o sistema exibe mensagem informando que nenhum evento corresponde aos filtros e oferece ação para limpar filtros

#### Scenario: Erro ao carregar listagem
- **WHEN** a requisição `GET /eventos` falha por erro de rede ou do servidor
- **THEN** o sistema exibe estado de erro amigável com ação "Tentar novamente" e não exibe o estado de lista vazia

### Requirement: Visualização pública de detalhes do evento
O sistema SHALL disponibilizar uma página pública de detalhes em `/eventos/:id` contendo breadcrumb de retorno para `/eventos`, banner em destaque (com fallback), badge de status explícito, nome, descrição completa, período (data de início e término), horário (início e término), local de concentração, destino final (quando informado), descrição do ponto de partida (quando informada) e descrição de encerramento (quando informada).

#### Scenario: Carregamento bem-sucedido de detalhes de evento confirmado ou encerrado
- **WHEN** o usuário acessa `/eventos/:id` de um evento `CONFIRMADO` ou `ENCERRADO`
- **THEN** o sistema consulta `GET /eventos/:id` e exibe todas as informações do evento com rótulos claros e breadcrumb para voltar à listagem `/eventos`

#### Scenario: Acesso direto a evento cancelado
- **WHEN** o usuário acessa diretamente `/eventos/:id` de um evento com status `CANCELADO`
- **THEN** o sistema exibe um aviso explícito de que o evento foi cancelado e oferece botão para retornar à listagem de eventos

#### Scenario: Acesso direto a evento planejado ou inexistente
- **WHEN** o usuário acessa `/eventos/:id` de um evento inexistente (`404`) ou ainda em status `PLANEJADO`
- **THEN** o sistema informa que o evento não foi encontrado ou não está disponível publicamente e oferece botão para retornar à listagem `/eventos`

#### Scenario: Erro de requisição ao carregar detalhes
- **WHEN** a requisição `GET /eventos/:id` falha por erro de rede ou servidor
- **THEN** o sistema exibe mensagem de erro amigável com opções de "Tentar novamente" e "Voltar para Eventos"
