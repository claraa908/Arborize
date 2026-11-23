# Spec Delta

## Purpose

Allows citizens to request removal of an invasive Nim tree and track the AMMA decision and work, while AMMA staff manage the request and its signed suppression report. Keeps this process separate from ordinary seedling recommendations and requests.

## ADDED Requirements

### Requirement: Citizen can request Nim removal
The system SHALL let a citizen submit a Nim removal request with identifying contact details, the tree location, and a description, and SHALL return a persistent protocol for follow-up.

#### Scenario: Submit a complete request
- **WHEN** a citizen submits valid required fields and an optional tree photograph
- **THEN** the system stores the request with an initial status and returns its protocol and submission date

#### Scenario: Reject invalid request input
- **WHEN** required fields are missing or the photograph violates the allowed file type or size
- **THEN** the system returns field-specific validation feedback and does not create a request

#### Scenario: Citizen views request confirmation
- **WHEN** the request is stored successfully
- **THEN** the confirmation presents the protocol, initial status, and a route to follow the request

### Requirement: Citizen can privately track a Nim removal request
The system SHALL provide a limited request timeline to a citizen who supplies the matching protocol and requester CPF, without exposing the CPF or internal AMMA data in the response.

#### Scenario: Retrieve a matching request
- **WHEN** a citizen submits a valid protocol and the CPF associated with that request
- **THEN** the system returns the current status, dated public timeline events, and a summary of the requested tree and replacement species

#### Scenario: Reject a mismatched lookup
- **WHEN** the protocol does not exist or the CPF does not match
- **THEN** the system returns the same not-found response without disclosing which value failed

#### Scenario: Display the request timeline
- **WHEN** the citizen opens the tracking view for a matching request
- **THEN** the view distinguishes received, under review, approved or rejected, report issued, removal/replacement in progress, and completed states using text and accessible indicators

### Requirement: AMMA manages Nim removal requests
The system SHALL let authorized AMMA staff review persisted requests, approve or reject them, and record public progress updates for removal and native replacement.

#### Scenario: Review requests
- **WHEN** an authorized AMMA staff member opens request management
- **THEN** the system displays persisted requests with protocol, requester summary, location, status, and submission date, with loading, empty, and friendly error states

#### Scenario: Decide a request
- **WHEN** an authorized AMMA staff member approves or rejects a request
- **THEN** the system persists the decision, records its date and responsible staff member, and updates the citizen-visible timeline

#### Scenario: Deny management to other roles
- **WHEN** a caller without AMMA management permission attempts a decision, progress update, or report operation
- **THEN** the API denies the operation and leaves the request unchanged

#### Scenario: Record removal and replacement progress
- **WHEN** AMMA staff record removal, native replacement, or completion progress
- **THEN** the system appends a dated timeline event and exposes the corresponding public status

### Requirement: AMMA prepares and stores the signed suppression report
The system SHALL let authorized AMMA staff associate a technical report and its signed PDF with an approved Nim removal request.

#### Scenario: Issue a report
- **WHEN** authorized AMMA staff provide the technical opinion, justification, inspection date, native replacement species, and valid signed PDF for an approved request
- **THEN** the system stores report metadata with the request, stores the binary document in object storage, and records the report-issued timeline event

#### Scenario: Reject an invalid report document
- **WHEN** the uploaded document is not a PDF, exceeds the configured size limit, or storage fails
- **THEN** the API returns a friendly structured error and does not report a successful issuance

#### Scenario: Review reports in the AMMA workspace
- **WHEN** authorized AMMA staff open the laudos workspace
- **THEN** the list, filters, summaries, details, and form use persisted requests, trees/locations, species, report metadata, and document availability instead of fictitious local records

### Requirement: Laudos workspace fits supported viewport widths
The system SHALL present the laudos list, details, and form without page-level horizontal overflow on mobile and desktop viewports.

#### Scenario: Use the workspace on a narrow screen
- **WHEN** an AMMA staff member uses the laudos workspace on a mobile-width viewport
- **THEN** content fits the viewport, form fields stack, and any wide data table scrolls within its own region

#### Scenario: Use the workspace on a wide screen
- **WHEN** an AMMA staff member uses the laudos workspace on a desktop viewport
- **THEN** the layout uses available width with readable hierarchy and the project's existing visual tokens

### Requirement: Nim removal stays separate from the planting quiz
The system SHALL keep Nim replacement requests in their dedicated Arborização form and omit the Nim replacement question from the ordinary seedling recommendation quiz and its API contract.

#### Scenario: Start the Arborização citizen flow
- **WHEN** a citizen opens the Arborização area
- **THEN** the citizen can choose the Nim removal request before starting the existing planting quiz

#### Scenario: Request ordinary seedling recommendations
- **WHEN** a citizen completes the planting quiz
- **THEN** the quiz asks only about the planting site and seedling request, and the recommendation API accepts those answers without a Nim replacement answer

#### Scenario: Preserve the ordinary seedling flow
- **WHEN** a citizen requests a seedling through the existing quiz flow
- **THEN** its existing recommendations, request, recycling conversion, and tracking behavior remain available

### Requirement: Citizen can track a seedling request from Arborização
The system SHALL let a citizen open seedling request tracking from the Arborização entry and planting quiz, then retrieve a persisted request using its protocol and the requester's CPF.

#### Scenario: Retrieve a seedling request after navigating away
- **WHEN** a citizen provides a matching seedling request protocol and valid CPF
- **THEN** the system returns the persisted protocol, current status, species, quantity, recyclable amount, requester display name, and submission date without returning CPF

#### Scenario: Reject a mismatched seedling lookup
- **WHEN** the protocol or CPF does not match an existing request
- **THEN** the system returns the same not-found response without disclosing which value failed

#### Scenario: Copy a seedling request protocol
- **WHEN** the citizen views a matching request
- **THEN** the tracking view offers an accessible action to copy its protocol
