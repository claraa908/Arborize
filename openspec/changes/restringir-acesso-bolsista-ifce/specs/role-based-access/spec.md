# Spec Delta

## Purpose

Define como o sistema limita páginas e APIs conforme o perfil autenticado. Para bolsistas do IFCE, o fluxo interno disponível é o Viveiro e o gerenciamento do próprio perfil; também podem navegar pelas áreas públicas de Arborização e Biblioteca Digital.

## ADDED Requirements

### Requirement: Login de bolsista direciona ao Viveiro
O sistema MUST direcionar usuários autenticados com papel `ROLE_BOLSISTA_IFCE` ou `ROLE_IFCE_BOLSISTA` ao Viveiro após o login, independentemente do destino de retorno informado.

#### Scenario: Login de bolsista sem destino anterior
- **WHEN** um usuário com um dos papéis de bolsista conclui o login
- **THEN** o sistema abre a tela principal do Viveiro

#### Scenario: URL de retorno aponta para outra funcionalidade
- **WHEN** o login de um bolsista recebe um destino de retorno fora do Viveiro
- **THEN** o destino é ignorado e o usuário é direcionado ao Viveiro

### Requirement: Bolsista tem rotas explicitamente permitidas
Durante a sessão, o bolsista MUST acessar as páginas do Viveiro, seu perfil próprio e as áreas públicas de início, Arborização e Biblioteca Digital. Tentativas de abrir qualquer outra rota MUST redirecionar ao Viveiro.

#### Scenario: Bolsista tenta abrir uma rota não permitida
- **WHEN** um usuário autenticado como bolsista navega para uma rota fora do Viveiro, do perfil próprio, de Arborização, Biblioteca Digital e da página pública inicial
- **THEN** o sistema redireciona para a tela principal do Viveiro

#### Scenario: Bolsista abre o próprio perfil
- **WHEN** um usuário autenticado como bolsista seleciona o atalho Meu perfil ou abre a rota institucional do próprio perfil
- **THEN** o sistema exibe a tela de perfil com os dados da própria conta e permite editar os dados e a senha, sem permitir acesso ao perfil de outra pessoa

#### Scenario: Bolsista navega nas áreas públicas permitidas
- **WHEN** um usuário autenticado como bolsista seleciona Arborização ou Biblioteca Digital no menu público
- **THEN** o sistema exibe a página selecionada e seus fluxos públicos associados, mantendo as demais rotas bloqueadas

### Requirement: Bolsista vê somente a navegação do Viveiro
Para ambos os papéis de bolsista do IFCE, a navegação autenticada MUST exibir somente a entrada Viveiro no menu lateral. O cabeçalho MUST exibir o atalho para a página pública inicial, o atalho Meu perfil e o controle de encerramento de sessão; o atalho Meu perfil MUST abrir somente os dados da própria conta. Os demais atalhos de navegação administrativa MUST permanecer ocultos. Na página pública, as opções Arborização e Biblioteca Digital MUST funcionar para o bolsista.

#### Scenario: Menu do bolsista
- **WHEN** um usuário autenticado como bolsista visualiza o layout do sistema
- **THEN** o menu lateral contém somente a entrada Viveiro, o cabeçalho exibe os atalhos para a página pública e o perfil próprio além da saída da sessão, e as opções Arborização e Biblioteca Digital funcionam na navegação pública

### Requirement: Bolsista pode acessar as páginas internas do Viveiro
O sistema MUST permitir ao bolsista as páginas existentes que compõem o fluxo do Viveiro, incluindo lista, cadastro, detalhes e edição de espécies, além da tela institucional de perfil próprio.

#### Scenario: Navegação interna do Viveiro
- **WHEN** um usuário autenticado como bolsista abre uma rota de lista, cadastro, detalhes ou edição do Viveiro
- **THEN** a página solicitada é exibida

#### Scenario: Bolsista acessa a tela de perfil institucional
- **WHEN** um usuário autenticado como bolsista abre a rota `/sistema/perfil`
- **THEN** a tela institucional carrega os dados da própria conta e oferece as ações existentes de edição do perfil e da senha

### Requirement: APIs fora do Viveiro são proibidas para bolsistas
O backend MUST negar com HTTP 403 requisições de bolsistas fora da lista de APIs do Viveiro, do perfil próprio, de Arborização pública, Biblioteca Digital pública, login e preflight CORS. As APIs de perfil permitidas MUST limitar operações à identidade autenticada (`/usuarios/me`); endpoints de outras contas e todas as demais APIs, inclusive operações administrativas da Biblioteca, MUST permanecer bloqueadas.

#### Scenario: Bolsista acessa APIs das áreas permitidas
- **WHEN** um usuário autenticado como bolsista solicita uma destas APIs públicas:
  - `/especies/**`
  - `GET /api/materials`, `GET /api/materials/{id}`, `GET /api/materials/{id}/cover` ou `GET /api/materials/{id}/download`
  - `POST /api/assistente-plantio/recomendar`
  - `GET /api/solicitacoes-muda/conversao` ou `POST /api/solicitacoes-muda`
  - `GET /usuarios/me`, `PUT /usuarios/me` ou `PUT /usuarios/me/senha`
- **THEN** o endpoint processa a requisição conforme seu contrato atual

#### Scenario: Bolsista não acessa dados de outra conta
- **WHEN** um usuário autenticado como bolsista solicita endpoint de perfil que não seja `GET /usuarios/me`, `PUT /usuarios/me` ou `PUT /usuarios/me/senha`
- **THEN** o backend responde HTTP 403 sem executar a operação

#### Scenario: Bolsista chama endpoint não autorizado
- **WHEN** um usuário autenticado como bolsista chama uma API não listada, inclusive operações administrativas de materiais
- **THEN** o backend responde HTTP 403 e não executa a operação solicitada

### Requirement: Restrições de bolsista não alteram os demais perfis
O sistema MUST manter para visitantes sem sessão e para os demais papéis as permissões de páginas e endpoints que possuíam antes desta mudança.

#### Scenario: Visitante preserva acesso público
- **WHEN** uma pessoa sem sessão acessa uma página ou endpoint que já era público
- **THEN** o acesso continua disponível conforme as regras atuais

#### Scenario: Outro perfil preserva seu fluxo
- **WHEN** um usuário autenticado com papel diferente dos dois papéis de bolsista acessa uma rota ou endpoint permitido pelas regras atuais
- **THEN** a autorização e a navegação continuam conforme essas regras
