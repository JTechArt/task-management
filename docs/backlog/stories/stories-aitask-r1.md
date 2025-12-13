# Stories: AiTask R1 (BMAD)
Epic: EP-AITASK-R1 (`docs/backlog/epics/epic-aitask-r1.md`)

## DoD (applies to all stories)
- Lint/static analysis clean; coverage ≥80% for touched modules.
- Tests written (unit for use cases/repos; integration where DB/flows involved).
- UI pieces meet WCAG 2.1 AA; keyboard nav and focus visible.
- Performance smoke relevant to scope (startup ≤3s, list <500ms @1k tasks).
- Docs updated (runbook/README notes where applicable).

## AITASK-R1-S1: Initialize Platform Scaffold
- Outcome: Maven/Kotlin 2/JVM21/Compose Desktop skeleton with logging, HikariCP, Exposed, Flyway wired; jpackage + Docker skeleton present.
- AC:
  - App builds and runs a sample Compose window.
  - Config driven by env vars; defaults safe for local dev.
  - Logging configured at info/error; no secrets logged.
  - jpackage and Docker skeleton scripts present.
- Tasks:
  - Create parent POM/ modules as needed; set JVM toolchain 21.
  - Add dependencies: Compose Desktop, HikariCP, Exposed, Flyway, logging.
  - Wire config loader (env + defaults); document env vars.
  - Add basic Compose entry point; verify window renders.
  - Add Dockerfile and docker-compose service stub for DB/app; add jpackage skeleton.
- Tests:
  - Smoke test for application startup hook (headless).

## AITASK-R1-S2: Flyway Baseline Schema
- Outcome: Migrations for projects, tasks, rules, project_rules, slack_channels with enums, constraints, cascades per PRD.
- AC:
  - Flyway applies cleanly on empty DB.
  - Enums, FKs, cascades, uniqueness, indexes match PRD.
  - Exposed mappings align with schema.
- Tasks:
  - Author V1 migrations (tables, enums, indexes, triggers for updated_at/completed_at if needed).
  - Add repositoryUrl regex constraint.
  - Set cascade delete tasks on project removal; define project_rules links.
  - Add indexes for status/type/search fields.
- Tests:
  - Migration integration test: apply on clean DB; verify constraints and indexes.

## AITASK-R1-S3: Project Domain and CRUD
- Outcome: Project entity + repository + use cases with validation.
- AC:
  - Unique name enforced; repositoryUrl regex validated.
  - Create/read/update/delete operations persist correctly.
  - Deleting project cascades tasks.
- Tasks:
  - Define Project domain model and DTOs.
  - Implement Exposed DAO/table mapping with validations.
  - Implement use cases (create/update/delete/get/list).
  - Handle error cases (duplicate name, bad URL).
- Tests:
  - Unit tests for use cases (success/failure).
  - Integration tests for persistence and cascade delete.

## AITASK-R1-S4: Task Domain, Status, Timestamps
- Outcome: Task entity + repository + use cases; status transitions; timestamps.
- AC:
  - createdAt/updatedAt managed automatically.
  - completedAt set only on transition to COMPLETED.
  - Invalid status transitions rejected with clear errors.
  - Enums match PRD taskType/status definitions.
- Tasks:
  - Define Task domain model and status transition guard.
  - Implement Exposed mapping and repository.
  - Implement use cases for create/update/status change/delete/get/list.
- Tests:
  - Unit tests for transition guard logic.
  - Integration tests for timestamps and transitions.

## AITASK-R1-S5: Workspace/Branch Generation
- Outcome: On task create, generate workspacePath `{base}/{projectName}/{taskId-short}` and branchName from project.branchTemplate; persist.
- AC:
  - Deterministic generation; persisted with task.
  - Branch template substitution works; sensible default applied.
- Tasks:
  - Implement generator utility (base path + sanitized project name + short taskId).
  - Apply generator in Task create use case.
  - Support project.branchTemplate with placeholders; default to `task-{taskId}`.
- Tests:
  - Unit tests for generator (edge chars, lengths).
  - Integration test: persisted values saved.

## AITASK-R1-S6: Filters, Search, Stats
- Outcome: Filter tasks by status/type/project/text; stats by status/type; indexed queries.
- AC:
  - Filters produce correct subsets for combinations.
  - Stats accurate for status/type counts; recent activity list correct.
  - List query <500ms @1k tasks with indexes.
- Tasks:
  - Implement repository queries with filters.
  - Add search over title/description (use suitable index).
  - Add stats service for aggregates and recent activity.
- Tests:
  - Unit tests for query builders.
  - Integration tests with seeded data; perf smoke (timed).

## AITASK-R1-S7: Mockups for UI Shell (Required Pre-Implementation)
- Outcome: Static + interactive mockups for task list, task detail/edit dialog, filters panel, dashboard; states loading/empty/error/success; responsive/adaptive.
- AC:
  - Mockups approved; include accessibility notes and interaction states.
  - Shows responsive/adaptive behavior and keyboard focus.
- Tasks:
  - Produce static mockups and an interactive HTML prototype.
  - Cover states: loading/empty/error/success for list and dashboard.
  - Annotate with WCAG notes, focus order, contrast.
- Tests:
  - Approval checkpoint; no automated test.

## AITASK-R1-S8: Task List UI + Filters
- Outcome: Compose view for task list with filters and states.
- AC:
  - Loading/empty/error/populated states rendered per mockups.
  - Keyboard navigation across list and filters.
  - Filter inputs bind to view-model and update results.
  - Error/success toasts displayed on operations.
- Tasks:
  - Implement view-model (state, intents, validation).
  - Compose screens per mockups; add state handling.
  - Wire to task queries with filters.
- Tests:
  - View-model state tests; UI state tests with fake repo.

## AITASK-R1-S9: Task Detail/Edit Dialog
- Outcome: Compose dialog for create/edit task with inline validation.
- AC:
  - Required fields validated client-side; inline messages.
  - Esc cancels, Enter submits; buttons keyboard accessible.
  - Success/error toasts shown; form resets correctly on close.
- Tasks:
  - Implement dialog UI per mockups; add validation rules.
  - Wire create/update flows; handle status transitions when editing.
  - Manage focus and accessibility labels.
- Tests:
  - View-model tests for validation and flows.
  - UI state tests for dialog states.

## AITASK-R1-S10: Dashboard Aggregates UI
- Outcome: Aggregates service and dashboard cards (status/type counts, recent activity).
- AC:
  - Loading/empty/error states visible per mockups.
  - Data matches stats service outputs.
  - Keyboard accessible; focus order defined.
- Tasks:
  - Implement aggregates service using repo stats.
  - Build dashboard Compose UI per mockups.
  - Add error handling and retry affordance.
- Tests:
  - Unit tests for aggregates.
  - UI state tests with fake data providers.

## AITASK-R1-S11: Integration Stubs (Git/IDE/Slack)
- Outcome: Git intent logger (no network), IDE launcher service for workspacePath, Slack channel linkage persistence.
- AC:
  - RepositoryUrl stored; branch names prepared (no network ops).
  - IDE launcher callable; logs intent with workspacePath.
  - Slack channel linkage persisted; no outbound calls.
- Tasks:
  - Implement stub Git intent service; log branch preparation intent.
  - Implement IDE launcher interface and Cursor stub.
  - Implement Slack channel persistence and simple service facade.
- Tests:
  - Unit tests for stubs and logging behavior.
  - Integration for persistence of Slack linkage.

## AITASK-R1-S12: Import/Export JSON
- Outcome: JSON import/export for projects/tasks/rules with validation.
- AC:
  - Invalid enums/URLs rejected with errors.
  - Round-trip preserves data; conflicts handled (documented).
  - Schema validated; malformed JSON handled gracefully.
- Tasks:
  - Define JSON schemas/contracts.
  - Implement import with validation and reporting; export with filters.
- Tests:
  - Unit tests for validation.
  - Integration tests for round-trip and error cases.

## AITASK-R1-S13: Quality, Packaging, Perf
- Outcome: Lint/format/static analysis; coverage gate; Docker compose for app+DB; jpackage skeleton validated; perf smoke scripts.
- AC:
  - CI passes lint/tests/coverage gate (≥80%).
  - docker-compose up works locally (app+DB).
  - jpackage build runs successfully (artifact produced).
  - Perf smoke recorded: startup ≤3s, list <500ms @1k tasks.
- Tasks:
  - Configure ktlint/detekt and coverage rules.
  - Finalize docker-compose services; docs for setup.
  - Validate jpackage flow on macOS (note Windows/Linux follow-ups).
  - Add perf smoke script and document usage.
- Tests:
  - CI pipeline verification.
  - Manual perf smoke with recorded timings.

