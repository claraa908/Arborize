# Proposal

## Why

`/sistema/laudos` currently renders only a placeholder, leaving AMMA staff without the planned visual workspace to review and prepare technical suppression reports. This increment delivers the laudo interface first so its workflow can be reviewed without changing the existing muda request and protocol tracking flows.

## What Changes

- Replace the `LaudosView.vue` placeholder at `/sistema/laudos` with a responsive AMMA screen for listing, filtering, and reviewing technical suppression reports.
- Provide the report form shown in the supplied reference, with protocol and inventory-tree links, technical opinion, biological or structural-risk justification, and selection of the signed official PDF.
- Use clearly fictitious local sample data and demonstrate form validation and success/empty/loading states.
- Keep the existing route and visual identity. Do not change the public Nim request form, quiz, muda tracking screen, `SolicitacoesView.vue`, APIs, or database in this increment.

## Capabilities

### New Capabilities
- `substituicao-botanica`: AMMA-facing visual workspace for reviewing and preparing technical suppression reports associated with a substitution protocol and inventory tree.

### Modified Capabilities
- Nenhuma.

## Impact

- Frontend: `frontend/src/views/auth/LaudosView.vue` and, only if needed, focused laudo-specific presentational components/types. Preserve the registered `/sistema/laudos` route.
- Reference: `openspec/docs/interfaces/substituicao-botanica/laudos.png`.
- Business traceability: RF021, as supplied by the user; RF019/RU002/RN002 motivate the wider substitution flow but their public request and tracking behaviors are outside this UI-only increment.
- Backend, APIs, Flyway, PostgreSQL, MinIO persistence, public Nim request, and citizen protocol tracking are deferred to a later change. The UI must label demo data as demonstrative and must not imply that a PDF or report was persisted.
