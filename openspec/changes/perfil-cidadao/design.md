# Design

## Context

Veja `proposal.md` para motivação e escopo.

Estado atual relevante do repositório:
- **Rota e view da dashboard do cidadão:** `frontend/src/views/public/CidadaoDashboardView.vue`, rota `cidadao-dashboard` em `frontend/src/router/index.ts`, botões em `TopBar.vue` e `dashboardRouteName` em `authStore.ts`.
- **Perfil atual do cidadão:** `frontend/src/views/public/PerfilView.vue` delega para `frontend/src/components/profile/MeuPerfil.vue`, que já consome `buscarMeuPerfil()` (`GET /usuarios/me`), `atualizarMeuPerfil()` (`PUT /usuarios/me`) e `ModalAlterarSenha.vue` (`PUT /usuarios/me/senha`), porém usa cores/arredondamentos legados e não exibe acompanhamentos.
- **Segurança no backend:** `SecurityConfig.java` já protege `/usuarios/me` e `/usuarios/me/**` com `.authenticated()`. Portanto, `GET /usuarios/me/acompanhamentos` herda automaticamente a exigência de JWT válido.
- **Entidades existentes no PostgreSQL (sem novas tabelas nem migrations):**
  - `SolicitacaoMuda` possui `cpf` (11 dígitos), `especie` (`@ManyToOne` lazy), `quantidadeMudas`, `totalRecicaveis`, `quantidadeRecebida`, `status` (`SolicitacaoMudaStatus`), `motivoRejeicao`, `criadaEm`, `bairro`, `logradouro`, `numeroEndereco`. O protocolo é formatado por `SolicitacaoMudaMapper.protocolo(item)`.
  - `SolicitacaoSubstituicao` possui `cpf` (11 dígitos), `protocolo`, `status` (`SubstituicaoBotanicaStatus`), `criadaEm`, `bairro`, `logradouro`, `numeroEndereco`, `descricaoArvore`.

## Goals / Non-Goals

**Goals:**
- Eliminar a dashboard intermediária do cidadão e redirecioná-lo direto para `/perfil` após login.
- Entregar um `MeuPerfil.vue` visualmente moderno, responsivo (empilhado no mobile, 2 colunas com sidebar fixa no desktop) e 100% aderente a `docs/padroes-de-projeto.md`.
- Consultar acompanhamentos reais do cidadão autenticado direto do banco via `GET /usuarios/me/acompanhamentos`, usando o CPF do usuário autenticado (sem aceitar CPF por parâmetro de URL para evitar vazamento de dados de terceiros).

**Non-Goals:**
- Alterar `PerfilInstitucionalView.vue` (`/sistema/perfil`) ou qualquer fluxo dos perfis AMMA/IFCE.
- Criar novas tabelas/colunas ou migrations Flyway.
- Permitir download de comprovante de retirada pelo cidadão (restrição explícita do `AGENTS.md`).

## Decisions

### 1. Endpoint `GET /usuarios/me/acompanhamentos` (seguro por JWT)
- **Decisão:** Adicionar `@GetMapping("/me/acompanhamentos")` em `UsuarioController` delegando para `CidadaoAcompanhamentoService.listarDoUsuario(Long usuarioId)`.
  - O serviço busca o `Usuario` pelo `usuarioId` do `@AuthenticationPrincipal`, normaliza/usa seu `cpf` (11 dígitos) e consulta:
    - `SolicitacaoMudaRepository.findByCpfOrderByCriadaEmDesc(cpf)` (anotado com `@EntityGraph(attributePaths = "especie")` para evitar `LazyInitializationException` / N+1).
    - `SolicitacaoSubstituicaoRepository.findByCpfOrderByCriadaEmDesc(cpf)`.
  - Retorna `CidadaoAcompanhamentosResponseDTO` (Java `record`) contendo:
    - `List<SolicitacaoMudaResumo>` (`id`, `protocolo`, `especieNome`, `quantidadeMudas`, `totalRecicaveis`, `quantidadeRecebida`, `status`, `motivoRejeicao`, `bairro`, `criadaEm`)
    - `List<SubstituicaoResumo>` (`id`, `protocolo`, `status`, `bairro`, `logradouro`, `numeroEndereco`, `descricaoArvore`, `criadaEm`)
- **Justificativa:** Nunca recebe CPF do cliente (elimina risco de IDOR — o cidadão só vê registros vinculados ao CPF da sua própria conta JWT). Reutiliza `SolicitacaoMudaMapper.protocolo(...)` para consistência de protocolo.

### 2. Remoção limpa da Dashboard do Cidadão
- **Decisão:**
  - Remover `frontend/src/views/public/CidadaoDashboardView.vue`.
  - Remover a rota `cidadao-dashboard` em `frontend/src/router/index.ts`.
  - Atualizar `authStore.ts`: `dashboardRouteName` retorna `'perfil'` quando `isCitizen` for verdadeiro.
  - Remover os botões `Dashboard` condicionais a `auth.isCitizen` em `TopBar.vue` (desktop e menu mobile).
  - Atualizar `frontend/src/__tests__/sessionNavigation.spec.ts` para esperar `'perfil'` em vez de `'cidadao-dashboard'`.
- **Justificativa:** Mantém toda a guarda de navegação (`sessionNavigation.ts`) consistente sem referências mortas.

### 3. Arquitetura visual em 2 colunas de `MeuPerfil.vue`
- **Decisão:** Reestruturar `frontend/src/components/profile/MeuPerfil.vue` (e extrair `frontend/src/components/profile/CidadaoAcompanhamentosPanel.vue` para manter o código limpo e modular):
  - **Container raiz:** `max-w-7xl mx-auto py-4 md:py-6` (sem fundo cinza duplicado dentro do `PublicLayout`).
  - **Grid principal:** `grid grid-cols-1 lg:grid-cols-12 gap-6 items-start`.
  - **Coluna esquerda (`lg:col-span-4 lg:sticky lg:top-4`):**
    - Card `rounded-box border border-amma-green-light bg-white p-6 shadow-xs` contendo:
      - Avatar circular com iniciais em `bg-amma-green text-white`, nome do cidadão, badge `Cidadão` (`bg-amma-green-lighter text-amma-green-dark`), e-mail e CPF mascarado.
      - Mini resumo numérico: total de solicitações de mudas e total de substituições de Nim.
      - Menu vertical de navegação (`aria-label="Seções do perfil"`) com 3 botões: **Meus Acompanhamentos**, **Dados Pessoais** e **Segurança**, com destaque visual ativo (`bg-amma-green text-white` / `rounded-btn`).
      - Seção discreta de atalhos rápidos (`RouterLink` para `arborizacao-assistente` e `arborizacao-substituicao-nim`).
  - **Coluna direita (`lg:col-span-8 min-w-0`):**
    - Renderiza dinamicamente:
      - `CidadaoAcompanhamentosPanel.vue` (aba padrão `acompanhamentos`) com pílulas de filtro (`Todos`, `Solicitações de Mudas`, `Substituição de Nim`), cards com badge de status (ícone + texto explícito, sem comunicar só por cor), botão para abrir o fluxo de acompanhamento correspondente (`/arborizacao/acompanhar` ou `/arborizacao/substituicao-nim/acompanhar`), skeleton de carregamento, empty state com CTAs e erro isolado com "Tentar novamente".
      - Formulário de **Dados Pessoais** (aba `dados`) seguindo exatamente os inputs de `docs/padroes-de-projeto.md` (`rounded-btn border-2 border-amma-green-light focus:border-amma-green focus:bg-amma-green-lighter`).
      - Card de **Segurança** (aba `seguranca`) com orientações breves e botão primário/secundário para abrir `ModalAlterarSenha.vue`.

## Risks / Trade-offs

- **[Cidadão que solicitou muda com CPF formatado ou diferente]** Se no banco houver solicitações antigas com CPF contendo pontuação, a busca exata por 11 dígitos não as encontraria → **Mitigação:** `SolicitacaoMuda` e `SolicitacaoSubstituicao` já normalizam CPF para 11 dígitos numéricos (`length = 11`); o `CidadaoAcompanhamentoService` normaliza `usuario.getCpf().replaceAll("\\D", "")` antes de consultar.
- **[Carregamento independente de perfil e acompanhamentos]** Uma falha em `/usuarios/me/acompanhamentos` não pode impedir o cidadão de editar seus dados pessoais → **Mitigação:** disparar `buscarMeuPerfil()` e `buscarMeusAcompanhamentos()` de forma independente, isolando erros de acompanhamentos no painel direito.
