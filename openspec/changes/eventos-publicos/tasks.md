# Tasks

## 1. Tipos, utilitários e evolução compatível da store de eventos

- [x] 1.1 Atualizar `EventoDTO` em `frontend/src/types/eventos.ts` para incluir o campo opcional `documentos` (espelhando `EventosResponseDTO`) e criar utilitário puro de apresentação de eventos (formatação defensiva de data/período, rótulos/classes de status e verificação de banner) com testes unitários passando via `npm run test:unit` em `frontend/`.
- [x] 1.2 Evoluir `frontend/src/stores/eventoStore.ts` para expor estado de erro da listagem (`error`) e busca de evento individual (`eventoAtual`, `isLoadingDetail`, `detailError`, `detailStatus`, `fetchEventoById`) com proteção contra requisições concorrentes, mantendo compatibilidade total com `frontend/src/__tests__/eventoStore.spec.ts` e cobrindo os novos estados com testes unitários passando.

## 2. Componente de cartão e tela pública de listagem (`/eventos`)

- [x] 2.1 Criar `frontend/src/components/eventos/EventoCard.vue` seguindo a estética de `DocumentoCard.vue` e os tokens de `docs/padroes-de-projeto.md` (banner com fallback no `@error`, badge de status textual `Confirmado`/`Encerrado`, nome, resumo, período/horário, local de concentração, acessibilidade via teclado) e validar a renderização e interação com testes de componente em Vitest.
- [x] 2.2 Criar `frontend/src/views/public/eventos/EventosPublicoView.vue` inspirada em `CatalogoView.vue` (cabeçalho sticky responsivo, busca por nome/local/destino, filtros `Todos`, `Confirmados` e `Encerrados` com contadores, regra de visibilidade apenas `CONFIRMADO` e `ENCERRADO` ordenados com `CONFIRMADO` primeiro, e estados de carregamento, vazio, sem resultados e erro com "Tentar novamente") e validar todos os cenários com testes em Vitest.

## 3. Tela pública de detalhes (`/eventos/:id`)

- [x] 3.1 Criar `frontend/src/views/public/eventos/EventoPublicoDetalhesView.vue` inspirada em `DocumentoView.vue` (breadcrumb para `/eventos`, banner em destaque com fallback, badge de status, descrição completa, datas, horários, local de concentração, ponto de partida, destino final e observações de encerramento; além de aviso explícito para `CANCELADO`, estado indisponível/não encontrado para `PLANEJADO` ou `404` e estado de erro com "Tentar novamente") e cobrir todos os estados com testes em Vitest.

## 4. Rotas públicas e navegação na TopBar

- [x] 4.1 Registrar as rotas públicas `/eventos` (`eventos-publico`) e `/eventos/:id` (`eventos-publico-detalhes`) sob `PublicLayout` em `frontend/src/router/index.ts` e conectar o item `Eventos` em `frontend/src/components/layout/TopBar.vue` com estado ativo para `/eventos` e sub-rotas; verificar navegação e destaque ativo em testes de roteamento/componente.

## 5. Verificação integrada

- [x] 5.1 Executar `npm run type-check`, `npm run build` e `npm run test:unit` em `frontend/`, confirmando ausência de erros de TypeScript/build e que as telas existentes (incluindo `/sistema/eventos` e `/biblioteca`) continuam passando nos testes.

## Workflow follow-up

- Revisar visualmente `/eventos` e `/eventos/:id` em desktop e mobile após a implementação.
- Arquivar a change no OpenSpec (`openspec archive`) após validação completa.
