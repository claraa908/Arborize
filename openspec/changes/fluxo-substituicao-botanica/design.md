# Design

## Context

See `proposal.md` for motivation and `specs/substituicao-botanica/spec.md` for observable behavior. The public `/arborizacao` route currently redirects directly into the planting quiz. `LaudosView.vue` contains component-local sample data, while `/api/solicitacoes-muda` already persists ordinary seedling requests. The existing `Supressao` entity is an aggregate used by dashboard queries, not a request workflow. Backend profiles currently use `ddl-auto=update`, no Flyway dependency or versioned migrations were found, and `SecurityConfig` has a broad permit rule for `/api/**`; new AMMA endpoints need explicit authorization before that rule.

## Goals / Non-Goals

**Goals:**
- Reuse Arborize's public form, feedback, input, authentication, species, and storage conventions where their contracts fit.
- Keep Nim removal requests and records tied to their own persistent domain while letting AMMA manage them from the existing laudos workspace.
- Preserve the existing ordinary seedling request flow and the current `Supressao` aggregate queries.
- Keep the laudos workspace responsive and aligned with the existing Tailwind tokens.

**Non-Goals:**
- Changing the AMMA seedling `SolicitacoesView.vue` or the approval/withdrawal lifecycle for seedling requests.
- Making the citizen issue or upload an official suppression report.
- Adding automatic inspection scheduling, inventory registration for private trees, or automatic supply of the replacement seedling.
- Replacing existing login, role, or public registration flows.

## Decisions

1. **Use a dedicated substitution request aggregate.** Store requester/contact snapshots, the reported tree location, description, protocol, and current state in a new request table. The request must not require an `Arvore` foreign key because a citizen may report a private tree that is absent from the public inventory. Store uploaded tree photos in MinIO and only opaque object references and media metadata in PostgreSQL. Keep personal details on the request record and out of status lookup responses.

   *Alternative considered:* reuse `Supressao`. Rejected because its existing fields (`zona`, `bairro`, species names, quantity, authorization, and occurrence date) represent completed aggregate data and do not capture applicant details, review history, or a pending report.

2. **Persist public progress as append-only request events.** Keep an event table related to the request for dated public status changes and AMMA notes, and derive the current status from the workflow state on the request. Use `RECEBIDA`, `EM_ANALISE`, `APROVADA`, `REJEITADA`, `LAUDO_EMITIDO`, `RETIRADA_EM_ANDAMENTO`, `REPLANTIO_EM_ANDAMENTO`, and `CONCLUIDA`. Only AMMA staff may advance review and execution states; a rejection includes an optional public explanation. Do not allow a report to be issued before approval.

   *Alternative considered:* overwrite only a status column. Rejected because the citizen needs the history and date of AMMA's acceptance and later removal/replacement progress.

3. **Store report metadata separately from its PDF.** Link one laudo record to its substitution request; persist opinion, justification type/text, inspection date, and selected replacement species as a foreign key to `Especie`. Store the signed PDF in MinIO through a laudo-specific storage adapter and persist only an opaque object key and validated media metadata in PostgreSQL. Accept only PDF content up to 10 MiB, consistent with the existing multipart ceiling. Issue report and status event in one database transaction; remove an uploaded object if the database operation fails.

   *Alternative considered:* keep the PDF in PostgreSQL or put the PDF bytes in the report DTO. Rejected because project standards reserve PostgreSQL for structured data and MinIO for binary documents.

4. **Separate public access from AMMA management.** Create public endpoints for request creation and tracking lookup. Submit protocol and CPF in a POST body for lookup; normalize CPF and return the same not-found response for unknown protocol or mismatched CPF. Return only public event descriptions and a limited request summary. Restrict request listing, decisions, timeline updates, report creation, and document download to AMMA staff. Add specific request matchers before the existing broad `/api/**` permit rule.

   *Alternative considered:* put CPF in a query parameter or return the whole request to anyone with a protocol. Rejected to reduce PII exposure through URLs/logs and prevent protocol-only enumeration.

5. **Keep the existing public Arborização route as the entry point.** Replace its redirect with a small choice screen for “Solicitar retirada de Nim” and “Assistente de plantio”; use focused routes for the request form and protocol tracking. In the quiz request payload, remove `substituicao_nim` from both frontend question data and backend validation, and change its required answer count from ten to nine without changing the remaining recommendation rules.

   *Alternative considered:* add the Nim question as a branching step in the quiz. Rejected because it conflates a tree removal authorization workflow with ordinary seedling recommendations.

6. **Use the existing laudos route for AMMA substitution management.** Replace local arrays with service/store-backed loading, decision and progress actions, report creation, and details linked to persisted request IDs. Do not add these records to the seedling requests tab. Constrain flexible parents with `min-w-0`, use responsive layouts, and place wide tables in their own `overflow-x-auto` region so the page itself stays within the viewport.

   *Alternative considered:* add substitution requests to `SolicitacoesView.vue`. Rejected by the requested scope and because that view represents ordinary seedling requests.

7. **Establish Flyway-managed schema before enabling validation.** The repository currently has no Flyway dependency or versioned schema migrations and both profiles use `ddl-auto=update`, despite the project rule requiring Flyway and `ddl-auto=validate`. Add a baseline migration that represents the existing PostgreSQL schema without dropping data, then a versioned migration for substitution request, event, laudo, and object metadata tables and constraints. Switch JPA schema handling to `validate` only after the existing schema is represented and migration startup has a documented baseline path for databases already initialized by `update`.

   *Alternative considered:* leave `ddl-auto=update` and add only entity classes. Rejected because it would bypass the required migration process and would not establish a repeatable schema for new environments.

8. **Make seedling tracking resumable with protocol and CPF.** The existing seedling confirmation view depends on Pinia memory and cannot be opened from Arborização after a reload. Add a public POST lookup against the existing request record, compare the generated protocol and normalized CPF, and return the existing privacy-limited response. Keep CPF out of URLs and lookup responses. Reuse the same client CPF mask/checksum validation for seedling and Nim forms and add a copy-protocol action to both tracking results.

   *Alternative considered:* persist the full tracking result or CPF in browser storage. Rejected because the seedling request is already persisted in PostgreSQL and a protocol-plus-CPF lookup avoids creating a second client-side source of truth.

## Risks / Trade-offs

- [Baseline migration may diverge from databases previously created by Hibernate update] → Inventory the mapped schema before writing the baseline, preserve current data, and validate migration behavior against both an existing database and an empty database.
- [Protocol plus CPF remains a low-assurance public lookup] → Normalize and compare CPF on the server, avoid logging it or exposing it in URLs/responses, rate-limit if an existing facility is available, and return indistinguishable not-found errors.
- [MinIO and PostgreSQL cannot share a transaction] → Upload first, commit metadata/event transaction second, and remove the object on database failure; surface cleanup failure only in safe server logs.
- [Current authorization rules permit `/api/**` broadly] → Place narrow AMMA role matchers first and verify anonymous/citizen calls cannot reach management operations.
- [Removing the tenth quiz answer invalidates old clients sending `substituicao_nim`] → The current application is a single web client; update frontend and backend contract together and keep the ordinary seedling request API unchanged.

## Migration Plan

1. Inventory current entity/table mappings and local schema state; create and verify a non-destructive Flyway baseline for the schema already managed by Hibernate.
2. Add the substitution tables and indexes in a later Flyway migration, then set `ddl-auto=validate` for supported profiles.
3. Deploy the API and frontend together so the new nine-answer quiz contract and the updated backend validator remain in sync.
4. Roll back code without dropping request or document data. Any schema rollback must be an explicit forward migration or restore operation; do not delete citizen-submitted requests or MinIO objects automatically.

## Open Questions

- None that changes the specified flow. The public form will use the requester's CPF as the second factor for protocol lookup, matching the current public citizen flow and avoiding a new account requirement.
