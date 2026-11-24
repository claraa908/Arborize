# Spec Delta

## Purpose

Unificar a área autenticada do cidadão no portal Arborize em `/perfil`, removendo a dashboard intermediária `/dashboard` e oferecendo um perfil reestruturado em duas colunas com dados pessoais, segurança e acompanhamento real de solicitações de mudas e substituições botânicas vinculadas ao CPF do usuário autenticado.

## ADDED Requirements

### Requirement: Remoção da dashboard do cidadão e redirecionamento pós-login para o perfil
O sistema SHALL remover a rota `/dashboard` (`cidadao-dashboard`) e o botão "Dashboard" da navegação superior do cidadão, redirecionando o cidadão autenticado diretamente para `/perfil` após o login ou ao tentar acessar rotas restritas a outros perfis.

#### Scenario: Login de cidadão redireciona para Meu Perfil
- **WHEN** um usuário com perfil `ROLE_CIDADAO` realiza login sem um destino `redirect` válido
- **THEN** o sistema navega diretamente para `/perfil` (`name: 'perfil'`)

#### Scenario: Navegação superior do cidadão sem botão Dashboard
- **WHEN** um cidadão autenticado visualiza a barra de navegação pública (desktop ou menu mobile)
- **THEN** o botão "Dashboard" não é exibido
- **AND** os botões "Meu perfil" e "Sair" permanecem visíveis e funcionais

#### Scenario: Tentativa de acesso do cidadão a rota restrita de gestão
- **WHEN** um cidadão autenticado tenta acessar uma rota restrita de gestão (ex.: `/sistema/inventario`)
- **THEN** o sistema redireciona o cidadão para `/perfil`

### Requirement: Endpoint autenticado de acompanhamentos do cidadão
O sistema SHALL expor o endpoint autenticado somente leitura `GET /usuarios/me/acompanhamentos` que identifica o usuário pelo token JWT, utiliza o CPF cadastrado do próprio usuário e retorna suas solicitações de mudas (`solicitacoesMuda`) e suas solicitações de substituição botânica (`substituicoesBotanicas`) ordenadas da mais recente para a mais antiga.

#### Scenario: Consulta autenticada com acompanhamentos existentes
- **WHEN** um usuário autenticado realiza `GET /usuarios/me/acompanhamentos`
- **THEN** o servidor responde com status HTTP `200 OK` contendo as listas `solicitacoesMuda` e `substituicoesBotanicas` vinculadas ao CPF do usuário autenticado

#### Scenario: Consulta autenticada sem solicitações registradas
- **WHEN** o usuário autenticado não possui solicitações de mudas nem substituições botânicas vinculadas ao seu CPF
- **THEN** o servidor responde com `200 OK` e listas vazias (`solicitacoesMuda: []`, `substituicoesBotanicas: []`)

#### Scenario: Acesso não autenticado rejeitado
- **WHEN** uma requisição sem token JWT válido acessa `GET /usuarios/me/acompanhamentos`
- **THEN** o servidor rejeita a requisição com status HTTP `401 Unauthorized`

### Requirement: Layout simétrico em duas colunas do Meu Perfil com altura fixa e rolagem interna
O sistema SHALL apresentar `/perfil` em duas colunas simétricas com altura fixa ocupando a área visível em desktop: coluna esquerda fixa com avatar no padrão institucional, identidade, contadores e menu entre **Meus Acompanhamentos**, **Dados Pessoais** e **Segurança e Senha** (sem atalhos externos), e coluna direita interativa com rolagem interna no conteúdo.

#### Scenario: Abertura inicial na seção Meus Acompanhamentos
- **WHEN** o cidadão acessa `/perfil`
- **THEN** a coluna esquerda exibe o resumo do perfil e o menu de navegação interna
- **AND** a coluna direita abre por padrão na seção **Meus Acompanhamentos**

#### Scenario: Alternância entre seções no painel interativo
- **WHEN** o cidadão seleciona **Dados Pessoais** ou **Segurança e Senha** no menu lateral do perfil
- **THEN** a coluna direita atualiza para exibir a seção escolhida
- **AND** a coluna esquerda permanece visível com o item ativo destacado

### Requirement: Acompanhamento real expansível em linha do tempo no perfil do cidadão
O sistema SHALL exibir na seção **Meus Acompanhamentos** as solicitações de mudas e substituições botânicas do cidadão com filtros por tipo e cartões em acordeão de abertura única que exibem a etapa atual quando recolhidos e a linha do tempo completa do processo (etapas concluídas, atual e próximas bloqueadas) quando expandidos.

#### Scenario: Filtragem entre mudas e substituições botânicas
- **WHEN** o cidadão possui solicitações de mudas e de substituição botânica e seleciona o filtro *Solicitações de Mudas* ou *Substituição de Nim*
- **THEN** a lista exibe apenas os itens do tipo selecionado

#### Scenario: Expansão em acordeão único com etapas do processo
- **WHEN** o cidadão clica em um cartão de solicitação na lista
- **THEN** o cartão expande para exibir as etapas do processo (concluídas, em andamento e próximas etapas bloqueadas)
- **AND** ao clicar em outro cartão, o anterior se retrai automaticamente mantendo apenas o último aberto

#### Scenario: Estado vazio limpo sem solicitações
- **WHEN** o cidadão não possui nenhum acompanhamento cadastrado
- **THEN** a seção exibe estado vazio informativo sem botões de atalho para outras abas

#### Scenario: Erro ao carregar acompanhamentos não bloqueia edição do perfil
- **WHEN** `GET /usuarios/me/acompanhamentos` falha por erro de rede ou servidor
- **THEN** a seção de acompanhamentos exibe mensagem de erro amigável com botão "Tentar novamente"
- **AND** o cidadão continua podendo navegar para **Dados Pessoais** e **Segurança e Senha**

### Requirement: Redirecionamento dos acompanhamentos de Arborização para o perfil filtrado
O sistema SHALL direcionar os botões *Acompanhar requerimento* e *Acompanhar pedido de muda* na área de Arborização (assim como o redirecionamento pós-envio de solicitações do cidadão) para `/perfil` com o filtro correspondente (`?filtro=SUBSTITUICOES` ou `?filtro=MUDAS`) e, quando houver protocolo na query, expandir automaticamente o cartão correspondente.

#### Scenario: Acesso pelo botão Acompanhar requerimento em Arborização
- **WHEN** um cidadão clica em *Acompanhar requerimento* na tela de Arborização
- **THEN** o sistema navega para `/perfil?filtro=SUBSTITUICOES` exibindo os acompanhamentos já filtrados em *Substituição de Nim*

#### Scenario: Acesso com protocolo na URL expande o cartão automaticamente
- **WHEN** o cidadão abre `/perfil?filtro=SUBSTITUICOES&protocolo=<PROTOCOLO>`
- **THEN** o filtro *Substituição de Nim* é pré-selecionado e o cartão correspondente ao protocolo é aberto automaticamente

### Requirement: Edição de dados pessoais e alteração de senha no padrão visual do Arborize
O sistema SHALL permitir ao cidadão visualizar CPF e e-mail (somente leitura), editar e salvar nome completo, WhatsApp (com máscara brasileira) e endereço completo na seção **Dados Pessoais**, e abrir o modal de alteração de senha na seção **Segurança**, utilizando os tokens visuais oficiais do Arborize e feedback via `AppToastContainer`.

#### Scenario: Atualização bem-sucedida dos dados pessoais
- **WHEN** o cidadão altera nome, WhatsApp ou endereço e aciona "Salvar alterações"
- **THEN** o sistema envia `PUT /usuarios/me`, atualiza o nome na sessão e exibe toast de sucesso

#### Scenario: Abertura do modal de alteração de senha
- **WHEN** o cidadão acessa a seção **Segurança** e aciona "Alterar senha"
- **THEN** o sistema abre o modal `ModalAlterarSenha` para atualização da senha
