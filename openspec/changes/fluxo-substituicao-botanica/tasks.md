# Tasks

## 1. Database foundation

- [ ] 1.1 Inventory all current JPA mappings and PostgreSQL schema assumptions; create a non-destructive Flyway baseline and verify it represents existing tables and relationships without deleting existing records.
- [ ] 1.2 Add the required Flyway runtime configuration and a versioned migration for substitution requests, public timeline events, laudo metadata, constraints, indexes, and object references; verify migration on an empty database and on a database initialized by the current Hibernate update workflow.
- [ ] 1.3 Switch supported profiles from `ddl-auto=update` to `ddl-auto=validate` after migrations are in place; verify application startup against the migrated schema.

## 2. Substitution API and storage

- [ ] 2.1 Add request, timeline, and laudo entities/DTOs/repositories with validation and explicit mapping; keep applicant data on the request, native replacement linked to `Especie`, and avoid requiring a public inventory `Arvore` for private trees; verify persisted relations and normalized protocol/CPF handling.
- [ ] 2.2 Implement public request creation, photo validation/storage, and protocol-plus-CPF lookup with privacy-limited responses; verify invalid fields/files do not create records and mismatched lookups return indistinguishable not-found responses.
- [ ] 2.3 Implement AMMA request listing, decision, public progress events, report issue, and PDF retrieval with server-side PDF validation and MinIO storage; verify allowed transitions, cleanup after failed persistence, and role restrictions.
- [ ] 2.4 Order specific substitution API authorization rules before broad permit matchers and ensure only request creation and tracking lookup are public; verify anonymous/citizen users cannot invoke management operations.

## 3. Citizen Arborização flow

- [ ] 3.1 Replace the `/arborizacao` quiz redirect with a responsive entry screen offering Nim removal and the existing planting assistant; add the Nim request form before the quiz using the supplied reference and global feedback patterns; verify keyboard access, validation, upload error, loading, and success states.
- [ ] 3.2 Add protocol and CPF tracking with a persisted, privacy-limited status timeline; verify navigation works after reload and every displayed status/date comes from the API.

## 4. AMMA laudos workspace

- [ ] 4.1 Replace fictitious LaudosView records and local submission with persisted request/report queries, details, search/filter, and empty/loading/error states; verify request and report fields are sourced from their respective API records.
- [ ] 4.2 Add AMMA-only approve/reject, progress, signed PDF issuance, and document access to the existing laudos route without changing `SolicitacoesView.vue`; verify success/error feedback and that citizen-submitted requests appear in the laudos workflow.
- [ ] 4.3 Correct responsive sizing with Tailwind project tokens, `min-w-0`, and contained table scrolling; verify no page-level horizontal overflow at mobile, tablet, and desktop widths against `openspec/docs/interfaces/substituicao-botanica/laudos.png`.

## 5. Keep planting quiz focused on ordinary seedling requests

- [ ] 5.1 Remove `substituicao_nim` from frontend question data and update quiz progress counts; verify the remaining planting and quantity questions still complete in order.
- [ ] 5.2 Remove `substituicao_nim` from backend answer validation and update the expected answer count to nine; verify current frontend quiz requests still receive recommendations and ordinary seedling requests remain unchanged.

## 6. Integration review

- [ ] 6.1 Build the frontend and compile the backend; review the diff and verify public request, persisted tracking, AMMA report management, quiz contract, database migration, access control, and unchanged seedling requests tab against the spec scenarios.
- [x] 6.2 Add protocol-plus-CPF lookup for persisted seedling requests, expose it from Arborização and the quiz, and add protocol-copy actions; verify frontend production build and backend compilation.
