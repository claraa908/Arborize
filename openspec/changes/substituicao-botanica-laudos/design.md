# Design

## Context

See `proposal.md` for the revised UI-only scope and `specs/substituicao-botanica/spec.md` for the interface contract. The route `/sistema/laudos` already exists and lazy-loads `frontend/src/views/auth/LaudosView.vue`, which currently contains a placeholder. The project provides an authenticated layout, global toast container, Tailwind theme, and reusable table/feedback components. The supplied reference shows a report list with summary cards and a report form.

## Goals / Non-Goals

**Goals:**
- Replace the placeholder with an AMMA report workspace matching the supplied Arborize visual reference.
- Demonstrate the list, report details, form validation, and local success behavior with unmistakably fictitious data.
- Keep all controls within the existing laudo route and preserve the surrounding authenticated layout.

**Non-Goals:**
- Adding or changing the public Nim request form, public tracking, quiz, muda screens, or `SolicitacoesView.vue`.
- Connecting the form to an API, storing reports in PostgreSQL/MinIO, or adding migrations.
- Claiming that a locally selected PDF has been signed, uploaded, or persisted on the server.
- Adding new dependencies, routes, roles, or business status transitions.

## Decisions

1. **Keep the view under the existing route.** Implement list/form states inside `LaudosView.vue`; do not add a route or change the router. This preserves existing navigation and avoids widening the work to citizen flows.

   *Alternative considered:* add public and administrative routes for the complete substitution workflow. Deferred because the current request limits this increment to laudo screens.

2. **Use local fictitious data for the visual prototype.** Provide sample report rows with fake protocols, tree labels, dates, opinions, justifications, and statuses. Form submission may update only component-local demonstration state and must state that persistence is not connected.

   *Alternative considered:* call a new API or mutate existing inventory/muda services. Rejected because no backend contract or persistence is in scope.

3. **Use existing project UI conventions.** Use the design reference, theme tokens, semantic labels, keyboard-visible focus, responsive cards/table, inline validation, loading/empty/error examples, and the global toast store. Keep all component state and types local to the laudo UI unless a small laudo-specific component materially simplifies the view.

   *Alternative considered:* install a table/form dependency. Rejected because existing Vue/Tailwind components are sufficient and new dependencies are outside the request.

4. **Validate the selected PDF in the browser as a prototype aid.** Accept a non-empty `.pdf`/`application/pdf` file up to the repository's existing 10 MiB multipart ceiling; show the file name and never transmit it. This client check is for interface feedback only and is not a substitute for server validation in a future integration.

   *Alternative considered:* imply upload success immediately after file selection. Rejected because no storage API is in scope.

## Risks / Trade-offs

- [Demonstration data can be mistaken for real laudos] → Display a persistent “Dados demonstrativos” label and use visibly fictitious protocols/names.
- [Local success may look like persistence] → State in the feedback and form that the change exists only in the current view and disappears on reload.
- [The eventual API may use different statuses or file limits] → Keep labels/data isolated in local constants so a later integration can replace them without changing this prototype's layout.
