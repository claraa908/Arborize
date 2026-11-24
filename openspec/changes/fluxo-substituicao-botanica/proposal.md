# Proposal

## Why

Cidadãos ainda não têm um fluxo próprio para requerer a retirada de Nim Indiano, e a tela de laudos usa dados locais sem vínculo persistido com esses pedidos. O fluxo precisa registrar o requerimento no banco, permitir que a AMMA avalie e anexe o laudo oficial, e deixar o cidadão acompanhar o andamento sem misturar a substituição de Nim com o pedido comum de mudas.

## What Changes

- Add a citizen-facing Nim removal request before the planting quiz in the Arborização area, following the supplied substitution reference and existing public form patterns.
- Persist requests with a protocol and a status history suitable for AMMA review; allow citizens to retrieve a limited status view using the protocol and the requester's CPF.
- Connect the AMMA laudos screen to persisted requests, tree/location and species data, and signed PDF storage; keep approval, rejection, report issuance, and removal/replacement updates in the AMMA workflow.
- Fix the laudos layout for narrow and wide screens using project Tailwind tokens and prevent horizontal page overflow.
- Remove the Nim replacement question from the planting quiz and its backend answer contract while preserving the existing seedling request, recommendations, and planting guidance flow.
- Establish Flyway-managed schema migrations for the existing database and this workflow, then use JPA schema validation as required by the project architecture.
- Keep the existing seedling requests tab and its behavior out of scope.

## Capabilities

### New Capabilities

- `substituicao-botanica`: Citizen requests, AMMA review and report handling, document storage, and citizen status tracking for invasive Nim removal and native replacement.

### Modified Capabilities

- None. The project has no main OpenSpec capability files yet; the quiz contract change is captured as a requirement in the new substitution capability because it keeps Nim replacement requests separate from ordinary seedling recommendations.

## Impact

- Frontend: public Arborização entry and request/tracking views, planting quiz question data/store, `LaudosView.vue`, and focused API service/store/types.
- Backend: request, status/history, and laudo DTOs/entities/repositories/services/controllers; validation and PDF endpoints; Flyway migration(s); MinIO object storage for signed PDFs.
- Existing domain: use species records for native replacements and retain `Supressao` for its current aggregate reporting use; do not modify `SolicitacoesView.vue` or the ordinary seedling request lifecycle.
- API and security: public request creation and restricted tracking lookup, plus AMMA-only management operations using existing project authentication/authorization conventions.
- Add only the Flyway dependency required by the project's mandated database migration architecture; no new framework or database.
