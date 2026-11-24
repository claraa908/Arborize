# Spec Delta

## Purpose

Provides AMMA staff with a visual workspace to review and prepare technical suppression reports linked to a substitution protocol and its corresponding tree in the arboreal inventory.

## ADDED Requirements

### Requirement: AMMA report list and review screen
The system SHALL provide an AMMA-facing screen at `/sistema/laudos` to review technical suppression reports and their protocol, inventory tree, opinion, justification, date, and status.

#### Scenario: Open the report workspace
- **WHEN** an AMMA staff member opens `/sistema/laudos`
- **THEN** the screen displays the report list, summary information, and available search/filter controls using clearly identified fictitious sample data

#### Scenario: Find a report
- **WHEN** the staff member searches by protocol, tree, or justification
- **THEN** the list shows matching sample reports or a clear empty result state

#### Scenario: Review report details
- **WHEN** the staff member opens a report from the list
- **THEN** the screen shows its protocol, corresponding inventory tree, opinion, biological or structural-risk justification, PDF name, and status

#### Scenario: Initial loading and load failure presentation
- **WHEN** the workspace is first presented or a list operation is shown as loading or failed
- **THEN** the interface has distinct loading and friendly error presentations and does not present an error as an empty result

### Requirement: Technical report form presentation
The system SHALL provide an AMMA form for preparing a technical suppression report with a substitution protocol, corresponding inventory tree, technical opinion, biological or structural-risk justification, and official signed PDF selection.

#### Scenario: Open the form
- **WHEN** the staff member selects the action to prepare a report
- **THEN** the interface presents the required fields and identifies that the displayed protocols and trees are fictitious examples

#### Scenario: Invalid form
- **WHEN** the staff member submits the form with required fields missing or a file that is not a PDF or exceeds the configured UI limit
- **THEN** the interface associates validation messages with the invalid fields and keeps the entered values available for correction

#### Scenario: Demonstration submission
- **WHEN** the staff member submits a valid demonstration form
- **THEN** the interface displays a success toast and updates only its local demonstration state, clearly indicating that no report or document was persisted

### Requirement: Preserve existing request and tracking flows
The system SHALL leave the public Nim request flow, quiz, citizen muda tracking screen, `SolicitacoesView.vue`, existing APIs, and database unchanged in this increment.

#### Scenario: Existing muda flow remains available
- **WHEN** a citizen or AMMA staff member uses an existing muda request or tracking screen
- **THEN** its route and behavior are unchanged by the laudo interface
