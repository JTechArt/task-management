# AiTask Standalone — Product Requirements Document (PRD)

## 1. Summary
AiTask is a Kotlin/Compose Multiplatform for Desktop (org.jetbrains.compose) application for task and project management with deep Git and IDE integrations. This PRD is derived from `tech-appoach` specifications to guide the Maven/Kotlin/PostgreSQL implementation, replacing the prior Electron/TypeScript stack while enabling future IDE add-ons (Cursor, JetBrains, Augment).

## 2. Goals
- Provide reliable task and project management with Git-aware workflows.
- Deliver a native desktop UX (Compose Multiplatform for Desktop via org.jetbrains.compose) with fast load and responsive interactions.
- Support integrations: Git (GitHub/GitLab/Bitbucket via JGit), Slack notifications, Cursor IDE launch (future IDEs).
- Enforce rule management and future-proof for additional integration packs.

## 3. Non-Goals
- Mobile clients.
- Multi-tenant SaaS; scope is single user or small team deployments.
- Cloud-hosted DB; PostgreSQL is containerized locally.

## 4. Target Users and Personas
- Primary: Software engineers using Git-based workflows and AI-assisted IDEs.
- Secondary: Tech leads managing small backlogs, wanting quick task setup and rule enforcement.

## 5. Assumptions and Constraints
- Kotlin 2.0+, JVM 21, Maven 3.9+, Compose Multiplatform for Desktop (org.jetbrains.compose) 1.6+.
- PostgreSQL 16+, Exposed ORM, HikariCP, Flyway for migrations.
- Docker required for DB and optional app container; jpackage for installers.
- Performance targets: app start ≤ 3s, task list load ≤ 500ms with 1k tasks, DB queries ≤ 100ms typical.
- Accessibility: keyboard navigation, focus visibility, WCAG 2.1 AA contrast in UI components.

## 6. Functional Requirements
### 6.1 Task Management (Core Slice)
- CRUD tasks with fields: id (UUID), title (required), description, taskType ∈ {FEATURE, BUG_FIX, REFACTOR, DOCUMENTATION, TESTING, RESEARCH}, status ∈ {PENDING, IN_PROGRESS, COMPLETED, CANCELLED}, projectId (required), workspacePath, branchName, createdAt/updatedAt, completedAt auto on completion.
- Workspace bootstrap: generate workspacePath `{base}/{projectName}/{taskId}-{short}` and branchName from project.branchTemplate (default `task-{taskId}`); record in DB.
- Status transitions update timestamps; completedAt set when status becomes COMPLETED.
- Filters: status, type, project, search by title/description.
- Task statistics: counts by status/type; last updated timestamps.

### 6.2 Project Management
- CRUD projects: name (unique), repositoryUrl (validated), description, workspacePath, branchTemplate, optional wikiPage/team/hubUrl.
- Enforce referential integrity to tasks; cascade delete tasks on project removal.

### 6.3 Git Integration (via JGit)
- Store repositoryUrl per project.
- Prepare branch names per task; no automatic clone in first slice—record intent only.
- Future: clone/pull/commit/push, branch creation, status.

### 6.4 IDE Integration
- Cursor launch hook: stub service callable from UI to open workspacePath with context; log intent.
- Future: JetBrains/Augment launchers with the same contract.

### 6.5 Rule Management
- CRUD rules (global and project-scoped), link via project_rules.
- Apply rules to task context (metadata available for IDE integration).

### 6.6 Slack Integration
- Store Slack channels linked to projects; future notifications on task status changes.

### 6.7 Dashboard
- Provide aggregate metrics: tasks by status/type, recent activity, project coverage.
- Respect performance targets (<500ms with indexed queries).

### 6.8 Import/Export
- JSON import/export for tasks/projects/rules with validation; future CSV if needed.

## 7. Non-Functional Requirements
- Performance: list <500ms @1k tasks; task create <3s excluding network IO.
- Reliability: ACID via PostgreSQL; Flyway migrations; HikariCP pooling.
- Security: OAuth2 for external services (future); secrets not logged; DB creds via env.
- Logging: info-level for lifecycle events (create/update/delete), errors with context (no secrets).
- Observability: basic health/connection checks; future connection monitoring per `tech-appoach`.
- Packaging: Maven build; jpackage installers; Docker image for full stack.

## 8. Data Model (per `03-database-design.md`)
- Tables: projects, tasks, rules, project_rules, slack_channels (+ Flyway history).
- Constraints: repositoryUrl regex; taskType/status enums; triggers for updated_at; completed_at trigger on status change; FKs cascade/SET NULL as defined.
- Indexes: name, status, type, created_at/updated_at, GIN search for text fields.

## 9. UX Requirements
- Compose Multiplatform for Desktop (org.jetbrains.compose) with Material 3 styling.
- Screens: Task List, Task Detail/Edit dialog, Filters panel, Dashboard overview.
- States: loading, empty, populated, error; toasts for success/error; inline validation.
- Keyboard: tab order, Enter submit, Esc cancel dialogs.
- Mockups required before implementation (see `doc/tasks/mockups/mockup-task-crud.md`).

## 10. Integrations and Future Expansion
- Git providers: GitHub/GitLab/Bitbucket via JGit; abstract provider config.
- IDE: Cursor now; JetBrains/Augment planned via shared launcher interface.
- Slack: Ktor client with signing secret handling; retry/backoff.

## 11. Release Plan (Incremental)
- R1 (MVP): Task CRUD + Project CRUD + DB schema/migrations + Compose UI shell + stub IDE hook + basic dashboard metrics.
- R2: Git operations (clone/branch/push), Slack notifications, rule management UI.
- R3: IDE expansion (JetBrains/Augment), advanced dashboard, import/export polish.
- R4: Connection monitoring, performance tuning, installer hardening.

## 12. Acceptance Criteria (for R1)
- Migrations apply cleanly; Exposed models match schema.
- Task list and detail CRUD functional with filters; timestamps correct; completedAt logic correct.
- WorkspacePath/branchName generated and stored on create.
- Performance targets met on local env with 1k tasks.
- UI states and accessibility behaviors implemented; mockups produced and followed.
- Tests: unit for repositories/use cases; integration for migrations/CRUD; ViewModel state tests.

## 13. Risks and Mitigations
- Git auth complexity → start with read-only intents; add provider-specific auth later.
- Desktop packaging variance (macOS/Windows/Linux) → use jpackage and document prereqs.
- Performance regressions with large lists → ensure indexes; paginate if needed in later releases.

## 14. Open Questions
- Do we need offline mode/caching? (Not in R1.)
- Preferred auth provider order (GitHub vs GitLab) for future OAuth setup?

