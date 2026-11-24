# Proposal

## Why

O item **Sobre Nós** já existe na barra de navegação pública (`TopBar.vue`, apontando para a rota nomeada `sobre`), mas ainda não possui rota nem página implementada. A população de Quixadá precisa de uma página institucional clara e transparente que apresente a missão, as ações e as parcerias da AMMA (incluindo o laboratório do IFCE e associações de catadores), acompanhada de indicadores reais de impacto ambiental consultados diretamente do banco de dados.

## What Changes

- Criação da página pública institucional **Sobre Nós** na rota `/sobre` (`name: 'sobre'`, sob `PublicLayout`), acessível sem autenticação.
- Estrutura visual refinada baseada na identidade botânica do Arborize (`docs/padroes-de-projeto.md`):
  - **Hero institucional** com gradiente botânico suave, rótulo `SOBRE NÓS`, título *Autarquia Municipal do Meio Ambiente de Quixadá* e subtítulo explicativo.
  - **Grade de 3 pilares institucionais:** *Nossa missão*, *O que fazemos* e *Parceiros* (IFCE e associações de catadores).
  - **Grade de 4 indicadores de impacto em tempo real:** *Mudas doadas*, *Estoque do viveiro*, *Garrafas PET* e *Nins substituídos*, carregados do backend com estados explícitos de carregamento (skeleton), sucesso e erro com opção de tentar novamente (sem bloquear o restante da página institucional).
  - **Bloco "Fale com a AMMA"** com informações de atendimento presencial em Quixadá/CE e atalhos clicáveis para os serviços públicos do portal (*Arborização*, *Eventos* e *Biblioteca Digital*).
- Criação de endpoint público somente leitura no backend Spring Boot: `GET /api/sobre/indicadores`, retornando os 4 indicadores agregados diretamente das tabelas já existentes no PostgreSQL (sem novas tabelas nem migrations Flyway).
- **Fora de escopo:** formulário de envio de mensagens/ouvidoria, painel administrativo de edição de textos institucionais, novas tabelas/colunas no banco de dados e alterações na página inicial (`HomeView.vue`).

## Capabilities

### New Capabilities

- `sobre-nos`: Página institucional pública da AMMA em `/sobre` com informações de missão, atuação, parcerias, atendimento e indicadores ambientais reais servidos via endpoint público de leitura.

### Modified Capabilities

<!-- Nenhuma especificação existente em openspec/specs/ sofre alteração de requisitos. -->

## Impact

- **Backend (Java/Spring Boot):**
  - Novo DTO `SobreIndicadoresResponseDTO` (`dtos/response/`).
  - Novo serviço `SobreIndicadoresService` (`services/`) e controller `SobreController` (`controllers/`, `GET /api/sobre/indicadores`).
  - Novos métodos de consulta agregada em `MovimentacaoMudaRepository`, `SolicitacaoMudaRepository` e `SolicitacaoSubstituicaoRepository` (reutilizando `EspecieRepository.sumMudasDisponiveis()` já existente).
  - Testes unitários no backend para `SobreIndicadoresService` e `SobreController`.
- **Frontend (Vue 3 / TypeScript):**
  - Nova rota pública `/sobre` (`name: 'sobre'`) em `frontend/src/router/index.ts`.
  - Novo serviço `frontend/src/services/sobreService.ts` e tipo `frontend/src/types/sobre.ts`.
  - Nova view `frontend/src/views/public/SobreNosView.vue` (e componente auxiliar de card de indicador, se necessário).
  - Testes em Vitest cobrindo renderização institucional, carregamento, sucesso, erro com retry e navegação pela `TopBar`.
