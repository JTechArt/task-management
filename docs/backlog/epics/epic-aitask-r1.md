# Epic: AiTask R1 Core Task & Project Management

## Context
AiTask is a Kotlin/Compose Desktop app for Git-aware task and project management (see `docs/prd.md`). R1 targets the MVP slice with local PostgreSQL, Exposed ORM, and Flyway migrations, focusing on task/project CRUD, workspace/branch intent recording, basic dashboard metrics, and stubs for IDE/Git/Slack integrations.

## Goal
Deliver the R1 MVP that enables reliable project and task management with performance, accessibility, and quality gates met.

## In Scope
- Project CRUD with validation (unique name, repository URL regex, branch template, optional wiki/team/hub URL).
- Task CRUD with enums (taskType/status), timestamps, status transitions, completedAt on COMPLETED, workspacePath and branchName generation on create.
- Filters/search (status/type/project/title/description) and stats (counts by status/type, recent activity).
- Dashboard cards with aggregates and loading/empty/error states.
- Stubs: Git intent (JGit-ready), Cursor IDE launcher hook, Slack channel linkage persistence.
- Schema via Flyway; Exposed models; HikariCP configuration.
- Packaging skeleton: Maven build, Docker for DB/app, jpackage setup.

## Out of Scope (R1)
- Git clone/pull/push/branch network operations.
- Slack outbound notifications.
- JetBrains/Augment IDE launchers.
- Import/export beyond JSON (CSV, others).
- Mobile clients; multi-tenant SaaS.

## Constraints & Standards
- Kotlin 2.0+, JVM 21, Compose Desktop 1.6+, Maven 3.9+.
- PostgreSQL 16+, Exposed, HikariCP, Flyway.
- Performance: startup ≤3s; task list load ≤500ms @1k tasks; DB queries ≈100ms typical.
- Accessibility: WCAG 2.1 AA; full keyboard navigation; focus visibility.
- Quality gates: lint clean; tests ≥80% coverage; no critical vulnerabilities.

## Key Capabilities (Features)
1) Project management: create/read/update/delete with referential integrity; cascade task delete.  
2) Task management: CRUD; status transitions; completedAt; workspacePath/branchName generation from branch template.  
3) Filtering/search + stats: indexed queries for status/type/project/text; aggregates for dashboard.  
4) Dashboard UI: cards for counts/status/type, recent activity; loading/empty/error states.  
5) Integrations (stubs): Git intent logging; IDE launcher service for workspacePath; Slack channel linkage persisted.  
6) Packaging/ops: Flyway migrations; Docker compose for DB/app; jpackage skeleton.

## Linked Stories (AiTask R1)
- AITASK-R1-S1: Initialize platform scaffold.
- AITASK-R1-S2: Flyway baseline schema.
- AITASK-R1-S3: Project domain and CRUD.
- AITASK-R1-S4: Task domain, status, timestamps.
- AITASK-R1-S5: Workspace/branch generation.
- AITASK-R1-S6: Filters, search, stats.
- AITASK-R1-S7: Mockups for UI shell.
- AITASK-R1-S8: Task list UI + filters.
- AITASK-R1-S9: Task detail/edit dialog.
- AITASK-R1-S10: Dashboard aggregates UI.
- AITASK-R1-S11: Integration stubs (Git/IDE/Slack).
- AITASK-R1-S12: Import/export JSON.
- AITASK-R1-S13: Quality, packaging, performance.

## Risks / Unknowns
- Git auth and provider variance for future networked operations.
- Desktop packaging differences across macOS/Windows/Linux.
- Performance with larger datasets; indexing adequacy.

## Acceptance Criteria (R1-aligned)
- Flyway migrations apply cleanly on fresh DB; Exposed models match schema (projects, tasks, rules, project_rules, slack_channels; enums; constraints).
- Project CRUD enforces unique name and repository URL regex; cascade deletes tasks.
- Task CRUD sets timestamps; completedAt only on COMPLETED; invalid status transitions rejected.
- WorkspacePath/branchName auto-generated and persisted on create per branch template.
- Filters/search return correct subsets; stats return accurate counts; queries performant (@1k tasks).
- Dashboard renders loading/empty/error/success states; aggregates match data; keyboard accessible.
- Stubs: Git intent logged; IDE launcher callable with workspacePath; Slack channel linkage persisted.
- Tests: unit for use cases/repos; integration for migrations/CRUD/status transitions/workspace generation; coverage ≥80%.
- Lint/static analysis passes; no critical security findings.
- UI meets WCAG 2.1 AA for components delivered in R1.

## Definition of Done (DoD)
- Code merged with green CI: lint, tests, coverage gate ≥80%.
- Performance smoke: startup ≤3s; list load <500ms @1k tasks (local).
- Accessibility: keyboard nav, focus outlines, contrast checks on delivered views.
- Documentation: update PRD links if needed; record runbooks for migrations/startup; note stubs behavior.
- Packaging: Maven build succeeds; Docker compose up works for app+DB; jpackage skeleton verified.

## Dependencies
- Local Postgres instance via Docker.
- JGit library for future Git operations (stub phase references only).
- Compose Desktop Material 3 components.

## Milestones (suggested)
- M1: Schema + domain/use cases (projects/tasks) landed with tests.
- M2: UI shell with mockups approved and implemented (task list/detail, filters, dashboard states).
- M3: Dashboard aggregates + integration stubs (Git/IDE/Slack).
- M4: Hardening (perf, accessibility, packaging, coverage).

