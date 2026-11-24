# Spec Delta

## Purpose

Define o conjunto restrito de páginas e APIs disponíveis ao gestor IFCE, preservando a gestão do Viveiro, o cadastro da equipe de bolsistas e o acesso aos dados da própria conta.

## ADDED Requirements

### Requirement: Login do gestor IFCE direciona à Equipe do IFCE
O sistema MUST direcionar `ROLE_GESTOR_IFCE` à tela Equipe do IFCE após o login, sem permitir que um destino de retorno selecione outra página.

#### Scenario: Login do gestor sem destino anterior
- **WHEN** um usuário com papel `ROLE_GESTOR_IFCE` conclui o login
- **THEN** o sistema abre a tela Equipe do IFCE

#### Scenario: Login do gestor com destino de retorno
- **WHEN** o login do gestor recebe como destino uma rota diferente da Equipe do IFCE
- **THEN** o sistema ignora esse destino e abre a tela Equipe do IFCE

### Requirement: Gestor acessa somente as páginas autorizadas
Durante a sessão, `ROLE_GESTOR_IFCE` MUST acessar todo o site público disponível a visitantes, incluindo Eventos, o fluxo de requerimento e acompanhamento de substituição botânica, as páginas do Viveiro, Equipe do IFCE e o próprio perfil; qualquer outra rota MUST redirecionar à Equipe do IFCE. O escopo público inclui home, Arborização e Biblioteca Digital, mas não inclui dashboard nem perfil exclusivos do cidadão.

#### Scenario: Gestor abre uma página pública ou institucional permitida
- **WHEN** o gestor acessa as rotas públicas existentes de home, Eventos e seus detalhes, Arborização e seus fluxos de requerimento/acompanhamento, Biblioteca Digital, `/sistema/viveiro` e suas subpáginas, `/ifce/equipe` ou `/sistema/perfil`
- **THEN** o sistema exibe a página correspondente

#### Scenario: Gestor tenta abrir uma página privada de outro perfil ou administrativa
- **WHEN** o gestor acessa diretamente uma página fora do site público e das áreas autorizadas, incluindo dashboard geral, dashboard/perfil do cidadão ou páginas administrativas da AMMA
- **THEN** o sistema redireciona para Equipe do IFCE

### Requirement: Navegação do gestor mostra somente as áreas permitidas
O menu lateral de `ROLE_GESTOR_IFCE` MUST exibir somente Viveiro e Equipe do Laboratório IFCE. O cabeçalho MUST manter atalhos para a home pública, Equipe do IFCE, perfil próprio e saída, ocultando outros atalhos administrativos.

#### Scenario: Gestor visualiza os menus
- **WHEN** o gestor visualiza o layout autenticado em desktop ou dispositivo móvel
- **THEN** o menu apresenta somente Viveiro e Equipe, e o cabeçalho oferece home pública, Equipe, perfil e saída

#### Scenario: Gestor navega pela home pública
- **WHEN** o gestor acessa a home pública
- **THEN** a navegação pública mantém as opções disponíveis a visitantes, e a ação de voltar ao painel abre Equipe do IFCE

### Requirement: Gestor pode consultar e cadastrar bolsistas IFCE
A tela Equipe do IFCE MUST permitir ao gestor consultar a lista de bolsistas e cadastrar novos bolsistas pelos fluxos existentes.

#### Scenario: Gestor consulta e cadastra um bolsista
- **WHEN** o gestor abre Equipe do IFCE e consulta ou envia um cadastro válido de bolsista
- **THEN** o sistema lista os bolsistas ou cria o novo cadastro conforme o contrato atual

### Requirement: Gestor acessa somente o próprio perfil
O gestor MUST consultar e editar apenas os dados da própria conta e a própria senha pela tela institucional de perfil.

#### Scenario: Gestor atualiza os próprios dados ou senha
- **WHEN** o gestor consulta ou atualiza seus dados ou senha pela tela de perfil
- **THEN** o sistema processa a operação para a identidade autenticada

#### Scenario: Gestor tenta acessar perfil de outra conta
- **WHEN** o gestor solicita uma operação de perfil que identifica outra conta
- **THEN** o sistema nega a operação sem revelar ou alterar os dados dessa conta

### Requirement: APIs do gestor usam uma allowlist restrita
O backend MUST permitir ao gestor APIs do site público disponíveis a visitantes, APIs do Viveiro, consulta/cadastro de bolsistas, perfil próprio, login e preflight CORS; APIs privadas ou administrativas fora da allowlist MUST responder HTTP 403.

#### Scenario: Gestor usa APIs públicas e institucionais permitidas
- **WHEN** o gestor usa GET `/api/materials` e seus detalhes públicos, GET `/eventos/**`, POST `/api/substituicoes-botanicas` ou `/api/substituicoes-botanicas/acompanhar`, `/especies/**`, POST `/api/assistente-plantio/recomendar`, GET `/api/solicitacoes-muda/conversao`, POST `/api/solicitacoes-muda`, APIs de consulta/registro do Viveiro, GET/POST `/ifce/bolsistas`, GET/PUT `/usuarios/me` ou PUT `/usuarios/me/senha`
- **THEN** a API processa a requisição conforme seu contrato atual

#### Scenario: Gestor chama API fora da allowlist
- **WHEN** o gestor chama qualquer outra API, inclusive endpoints de dashboard, inventário, administração da Biblioteca ou endpoints administrativos da AMMA
- **THEN** o backend responde HTTP 403 sem executar a operação

### Requirement: Restrição do gestor não altera outros perfis
O sistema MUST preservar as permissões dos visitantes e dos papéis canônicos diferentes de `ROLE_GESTOR_IFCE`. Contas legadas convertidas para `ROLE_GESTOR_IFCE` MUST seguir a mesma restrição do gestor.

#### Scenario: Outro perfil mantém as permissões existentes
- **WHEN** visitante ou usuário autenticado com papel canônico diferente de `ROLE_GESTOR_IFCE` acessa uma página ou API já permitida
- **THEN** o sistema mantém o comportamento de autorização existente

#### Scenario: Gestor convertido segue a restrição canônica
- **WHEN** uma conta com o papel legado `ROLE_IFCE_GESTOR` é convertida e autentica como `ROLE_GESTOR_IFCE`
- **THEN** o sistema aplica a allowlist de páginas e APIs do gestor IFCE
