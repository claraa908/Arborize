# Design

## Context

Veja `proposal.md` para motivação e escopo.

Estado atual relevante do repositório:
- **Backend pronto e público:** `EventosController` já expõe `GET /eventos`, `GET /eventos/{id}` e `GET /eventos/{id}/banner` (`EventosResponseDTO` contém `id`, `nome`, `descricao`, `dataInicio`, `dataTermino`, `localConcentracao`, `destinoFinal`, `horarioInicio`, `horarioTermino`, `descricaoPontoPartida`, `descricaoEncerramento`, `status` e `documentos`). `SecurityConfig` já libera esses endpoints `GET` com `permitAll()`.
- **Camada de dados frontend existente:** `frontend/src/types/eventos.ts`, `frontend/src/services/eventoService.ts` e `frontend/src/stores/eventoStore.ts` já existem e são utilizados no painel administrativo (`/sistema/eventos`).
- **Referência visual pública:** A Biblioteca Digital pública (`CatalogoView.vue`, `DocumentoCard.vue` e `DocumentoView.vue`) estabelece a linguagem visual pública do Arborize sob `PublicLayout.vue`: cabeçalho sticky com banner, busca + filtros em pílulas, grid responsivo de cartões (`rounded-box`, borda `amma-green-light`, `efeito-zoom`) e página de detalhes com breadcrumb e layout em duas colunas.
- **Lacunas observadas em `eventoStore.ts` e `eventos.ts`:** `eventoStore.ts` hoje não expõe estado de erro explícito (`error`) nem carregamento de evento individual (`eventoAtual`), tratando falhas apenas via toast — o que impede a UI pública de diferenciar "erro de rede/servidor" de "lista vazia" conforme exige `docs/padroes-de-projeto.md`. Além disso, `EventosResponseDTO` retorna `documentos` (lista de anexos do banner), o que permite saber se o evento possui banner antes de disparar `GET /eventos/{id}/banner`.

## Goals / Non-Goals

**Goals:**
- Entregar `/eventos` e `/eventos/:id` sob `PublicLayout.vue` seguindo fielmente a estética da Biblioteca Digital e os tokens de `docs/padroes-de-projeto.md`.
- Reaproveitar `eventoService` e `useEventoStore` sem quebrar nenhuma funcionalidade do painel administrativo (`/sistema/eventos`).
- Garantir diferenciação clara entre carregamento, sucesso, vazio, busca sem resultados, erro de requisição, evento cancelado e evento indisponível/não encontrado.
- Seguir a abordagem **Design First** (validar estrutura visual, responsividade mobile-first e acessibilidade antes/durante a integração com a API).

**Non-Goals:**
- Alterar qualquer arquivo do backend Java/Spring Boot.
- Implementar inscrição/participação de cidadãos em eventos ou alterar `HomeView.vue`.
- Refatorar as telas administrativas existentes em `frontend/src/views/auth/eventos/`.

## Decisions

### 1. Rotas e nomes no Vue Router
- **Decisão:** Adicionar duas rotas filhas em `PublicLayout` no `frontend/src/router/index.ts`:
  - `path: 'eventos'`, `name: 'eventos-publico'`, lazy-loaded para `frontend/src/views/public/eventos/EventosPublicoView.vue`.
  - `path: 'eventos/:id'`, `name: 'eventos-publico-detalhes'`, lazy-loaded para `frontend/src/views/public/eventos/EventoPublicoDetalhesView.vue`.
- **Justificativa:** Os nomes `eventos` e `eventos-detalhes` já existem nas rotas internas de `/sistema/eventos`. Usar `eventos-publico` e `eventos-publico-detalhes` evita colisão de nomes no Vue Router e espelha o padrão de `biblioteca-catalogo` / `biblioteca-documento`.
- **Alternativa descartada:** Renomear as rotas administrativas — descartado para não tocar no painel interno fora do escopo.

### 2. Ativação da `TopBar.vue`
- **Decisão:** Atualizar o item `{ label: 'Eventos' }` em `navLinks` para `{ label: 'Eventos', routeName: 'eventos-publico' }` e estender `isLinkActive` para manter o item ativo em qualquer sub-rota que comece com `/eventos` (assim como `arborizacao` faz com `/arborizacao`).
- **Justificativa:** Garante feedback visual de localização tanto na listagem quanto ao abrir os detalhes de um evento.

### 3. Evolução compatível da `eventoStore` e tipagem de `EventoDTO`
- **Decisão:**
  - Adicionar `documentos?: Array<{ id?: number; contentType?: string; originalFilename?: string }>` como campo opcional em `EventoDTO` (`frontend/src/types/eventos.ts`), refletindo o `EventosResponseDTO` real do backend.
  - Adicionar em `eventoStore.ts` o estado `error: Ref<string | null>` (limpo antes de cada requisição e preenchido no `catch`, preservando o toast existente) e uma ação `fetchEventoById(id)` com `eventoAtual`, `isLoadingDetail`, `detailError` e `detailStatus` (404 vs erro genérico), além de invalidação ao trocar de ID para evitar condição de corrida.
- **Justificativa:** Mantém 100% de retrocompatibilidade com `EventosView.vue` / `EventosCadastroView.vue` / `EventosDetalhesView.vue` e atende à diretriz de `docs/padroes-de-projeto.md` de nunca converter erro de API em lista vazia.
- **Alternativa descartada:** Criar uma segunda store só para eventos públicos — descartado para evitar duplicação de estado e serviço do mesmo recurso.

### 4. Filtragem, ordenação e visibilidade pública no frontend
- **Decisão:** Aplicar a regra de visibilidade (`CONFIRMADO` e `ENCERRADO`), a ordenação (`CONFIRMADO` antes de `ENCERRADO`), o filtro por status (`TODOS`, `CONFIRMADO`, `ENCERRADO`) e a busca textual (`nome`, `localConcentracao`, `destinoFinal`) via `computed` na `EventosPublicoView.vue`.
- **Justificativa:** O endpoint `GET /eventos` atual retorna a lista completa sem parâmetros de query; manter a filtragem no frontend evita modificar o backend (fora de escopo) e oferece resposta instantânea ao digitar.
- **Trade-off:** Caso o volume de eventos cresça muito no futuro, a filtragem/paginação poderá ser migrada para o backend sem alterar o contrato visual.

### 5. Estética baseada na Biblioteca Digital + Design First
- **Decisão:**
  - `EventosPublicoView.vue` reutiliza a estrutura do cabeçalho sticky da `CatalogoView.vue` (ícone `event` / `calendar_month`, título "Eventos Ambientais", subtítulo explicativo, campo de busca e botões de filtro `Todos`, `Confirmados` e `Encerrados` com contadores) + grade responsiva (`grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6`).
  - `EventoCard.vue` (`frontend/src/components/eventos/EventoCard.vue`) espelha `DocumentoCard.vue`: área superior de imagem (`h-44`) com banner via `eventoService.getBannerUrl(evento.id)` (somente quando há documento/banner e com handler `@error` para cair no fallback visual limpo), badge de status com texto explícito + ícone/cor semântica (`Confirmado` em `amma-green` e `Encerrado` em tom neutro), overlay de hover ("Ver detalhes"), título, resumo da descrição (`line-clamp-2`), data/horário e local de concentração. O cartão será acessível por teclado (`role="button"`, `tabindex="0"`, `@keydown.enter`/`@keydown.space` ou `<button>`/`<article>` clicável com foco visível).
  - `EventoPublicoDetalhesView.vue` espelha `DocumentoView.vue`: breadcrumb (`Eventos Ambientais / <Nome do Evento>`), layout responsivo em duas colunas (coluna visual com banner em destaque + programação de datas/horários e coluna de informações completas: status, descrição integral, local de concentração, ponto de partida, destino final e observações de encerramento).
  - Na tela de detalhes, eventos com status `CANCELADO` exibem alerta visual explícito (`feedback-warning` / `feedback-danger`) informando o cancelamento; eventos `PLANEJADO` ou resposta `404` exibem estado de "Evento não encontrado ou indisponível" com botão de retorno para `/eventos`.

## Risks / Trade-offs

- **[Banner sem arquivo ou falha no MinIO]** `GET /eventos/{id}/banner` pode retornar `404` se o evento não tiver imagem cadastrada ou se o MinIO estiver indisponível → **Mitigação:** só montar a URL do banner quando `evento.documentos?.length > 0` (ou `evento.bannerUrl`) e tratar o evento `@error` na tag `<img>` para alternar automaticamente para o fallback visual sem exibir imagem quebrada.
- **[Exposição de eventos `PLANEJADO` via API pública]** Como `GET /eventos` retorna todos os status, a filtragem é feita no frontend → **Mitigação:** o frontend oculta `PLANEJADO` e `CANCELADO` na listagem e bloqueia exibição normal de `PLANEJADO` em `/eventos/:id`; uma futura evolução de backend poderá filtrar por perfil no servidor.
- **[Formatação de datas/horários]** `dataInicio`, `dataTermino`, `horarioInicio` e `horarioTermino` vêm como `String` do backend (podendo estar em `YYYY-MM-DD` ou `DD/MM/YYYY`) → **Mitigação:** criar função utilitária pura de formatação defensiva na camada de apresentação que formata `YYYY-MM-DD` para `DD/MM/YYYY` e preserva o valor original caso já venha formatado ou exibe `"Não informado"` se vazio.
