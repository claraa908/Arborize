# Proposal

## Why

O portal público do Arborize já exibe o item **Eventos** na barra de navegação superior (`TopBar.vue`), mas ele ainda não possui rota nem interface pública — embora o backend já exponha os endpoints públicos de consulta (`GET /eventos`, `GET /eventos/{id}` e `GET /eventos/{id}/banner`). Os cidadãos de Quixadá precisam de uma vitrine pública clara e acessível para descobrir eventos socioambientais confirmados e consultar o histórico de eventos encerrados da AMMA.

## What Changes

- Ativação do item **Eventos** na navegação pública (`TopBar.vue`) apontando para a nova rota pública `/eventos`.
- Criação da tela pública de listagem de eventos (`/eventos`) inspirada na estética da Biblioteca Digital (`CatalogoView.vue`), com cabeçalho fixo responsivo, busca por texto (nome, local de concentração e destino final) e filtros por status (`Todos`, `Confirmados`, `Encerrados`).
- Regra de visibilidade pública: exibição apenas de eventos com status `CONFIRMADO` (destaque principal, exibidos primeiro) e `ENCERRADO` (histórico); eventos `PLANEJADO` e `CANCELADO` ficam ocultos na listagem pública.
- Criação do componente de cartão público de evento (`EventoCard.vue`), seguindo o padrão visual do `DocumentoCard.vue`, exibindo banner (com fallback visual), badge de status com texto explícito, nome, resumo, data/horário e local de concentração.
- Criação da tela pública de detalhes do evento (`/eventos/:id`), inspirada em `DocumentoView.vue`, exibindo breadcrumb de retorno, banner em destaque, badge de status, descrição completa, datas, horários, local de concentração, destino final, ponto de partida e observações de encerramento — além de aviso explícito caso o link direto aponte para um evento `CANCELADO` ou indisponível.
- Tratamento completo de estados de interface: carregamento (skeleton/spinner), lista vazia, busca sem resultados, erro de requisição (com opção de tentar novamente) e evento não encontrado (`404`).
- **Fora de escopo:** inscrição/confirmação de presença de cidadãos em eventos, alterações no painel administrativo (`/sistema/eventos`), alterações na página inicial (`HomeView.vue`) e criação/modificação de endpoints ou tabelas no backend.

## Capabilities

### New Capabilities

- `eventos-publicos`: Consulta pública de eventos socioambientais da AMMA no portal do cidadão, incluindo listagem filtrável de eventos confirmados e encerrados e visualização detalhada de cada evento.

### Modified Capabilities

<!-- Nenhuma especificação existente em openspec/specs/ sofre alteração de requisitos. -->

## Impact

- **Frontend (rotas e navegação):** `frontend/src/router/index.ts` (rotas `/eventos` e `/eventos/:id` sob `PublicLayout`), `frontend/src/components/layout/TopBar.vue` (vínculo da rota `eventos-publico` e estado ativo).
- **Frontend (views e componentes novos):** `frontend/src/views/public/eventos/EventosPublicoView.vue`, `frontend/src/views/public/eventos/EventoPublicoDetalhesView.vue` e `frontend/src/components/eventos/EventoCard.vue`.
- **Frontend (estado e serviço reutilizados):** `frontend/src/stores/eventoStore.ts`, `frontend/src/services/eventoService.ts` e `frontend/src/types/eventos.ts` (reaproveitamento e eventual ajuste mínimo de estado de erro/carregamento individual sem quebrar o painel administrativo).
- **Backend:** Sem alterações — consome os endpoints públicos existentes `GET /eventos`, `GET /eventos/{id}` e `GET /eventos/{id}/banner`, já liberados com `permitAll()` em `SecurityConfig.java`.
- **Testes:** Cobertura unitária/componentes em Vitest para listagem pública, filtragem de status/busca, estados de interface e visualização de detalhes.
