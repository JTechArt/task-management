# AiTask Product Requirements Document (PRD)

## Goals and Background Context

### Goals

- Reduce per-task development environment setup time by automating workspace creation, repository preparation, branch setup, and IDE launch.
- Centralize project, repository, task, rule, and IDE configuration in a single desktop application for developers and team leads.
- Support multi-repository development workflows without forcing teams into a single repository structure or Git provider.
- Make AI-assisted development repeatable by applying project and IDE-specific rules automatically when launching task workspaces.
- Improve visibility into active work through task lifecycle tracking, dashboard metrics, recent activity, and project-level analytics.
- Provide secure, cross-platform desktop operations for Git credentials, OAuth-based integrations, Slack notifications, and data portability.

### Background Context

AiTask is a cross-platform desktop application for developers who currently manage tasks, repositories, workspaces, IDE settings, and AI assistant rules across disconnected tools and manual steps. The product brief and feature list both point to the same core pain: starting work on a task is still operationally heavy, especially when a project spans multiple repositories, different IDEs, and different AI tooling preferences.

The product is intended to collapse that fragmented workflow into a single desktop experience that can create task workspaces, prepare Git state, launch the correct IDE, apply rules, and keep task progress visible. The MVP should therefore focus on turning "select task -> open ready-to-code workspace" into a reliable, repeatable, auditable flow rather than trying to solve every possible collaboration need up front.

### Change Log

| Date | Version | Description | Author |
| --- | --- | --- | --- |
| 2026-03-11 | v0.1 | Initial PRD draft created from product brief and features list | John (PM) |

## Requirements

### Functional

1. FR1: The system shall allow users to create, edit, archive, and delete projects with metadata including name, description, tags, teams, workspace path, and branch naming template.
2. FR2: The system shall allow each project to include one or more repositories with stored Git provider, clone URL, and repository-level development settings.
3. FR3: The system shall allow each repository to define one or more preferred IDEs and associated AI tooling or rule configurations.
4. FR4: The system shall allow users to import and export project definitions, including associated repositories and rule mappings.
5. FR5: The system shall allow users to create, view, update, archive, and delete development tasks assigned to a project.
6. FR6: The system shall support task types including Feature, Bug Fix, Research, Enhancement, Documentation, and Refactoring.
7. FR7: The system shall manage task status transitions at minimum across Pending, In Progress, Completed, and Archived states.
8. FR8: The system shall generate a task-specific local workspace when a user starts work on a task, using the configured project workspace path and task metadata.
9. FR9: The system shall allow users to choose which repositories in a multi-repository project are included when creating or opening a task workspace.
10. FR10: The system shall prepare selected repositories in the task workspace using repository retrieval settings optimized for fast startup and configurable by the product.
11. FR11: The system shall create task-specific Git branches using a configurable naming template such as `task-{taskId}`.
12. FR12: The system shall validate repository URLs and support Git operations across GitHub, GitLab, and Bitbucket repositories.
13. FR13: The system shall support both SSH and HTTPS authentication flows for Git operations.
14. FR14: The system shall use local SSH configuration when available so repository access can respect account-specific SSH key mappings.
15. FR15: The system shall securely store HTTPS credentials and other integration secrets in encrypted form.
16. FR16: The system shall allow users to launch a configured IDE for a task workspace, including Cursor, VS Code, and supported JetBrains IDEs.
17. FR17: The system shall present only the IDE options valid for the selected repository configuration when opening a task workspace.
18. FR18: The system shall apply global, project-specific, repository-specific, and IDE-specific rule files or configurations to the task workspace during launch preparation.
19. FR19: The system shall allow users to create, edit, attach, detach, and share reusable rule sets across projects.
20. FR20: The system shall allow users to import and export rule sets independently or together with project data.
21. FR21: The system shall provide a dashboard with task counts, project statistics, recent activity, completion metrics, and filtering or search over tasks and projects.
22. FR22: The system shall maintain an activity history for key actions including task creation, workspace generation, Git preparation, IDE launch, status changes, and task completion.
23. FR23: The system shall allow users to configure Slack channels per project and choose which task lifecycle events trigger Slack notifications.
24. FR24: The system shall support an OAuth2-based authentication framework for external provider integrations.
25. FR25: The system shall display connection and service health status for the database, Git repositories, and configured external APIs.
26. FR26: The system shall support JSON-based backup and restore of projects, tasks, repositories, and rules across installations.
27. FR27: The system shall support committing and pushing task changes from within the application or via its Git integration layer.
28. FR28: The system shall allow users to configure workspace retention, cleanup, or archival behavior when a task is completed or deleted.

### Non Functional

1. NFR1: The application shall run as a native desktop experience on Windows, macOS, and Linux from a shared Kotlin/Compose Multiplatform codebase.
2. NFR2: The application shall allow a user to open a ready-to-code task workspace within 60 seconds for a typical shallow-clone setup under normal network conditions.
3. NFR3: The application shall encrypt stored credentials and secrets at rest and avoid exposing them in logs, exports, or UI surfaces.
4. NFR4: The application shall preserve data integrity for projects, tasks, repositories, rules, and credentials through transactional persistence and versioned database migrations.
5. NFR5: The application shall remain responsive during long-running operations such as cloning, branch creation, health checks, and Slack notification delivery by using asynchronous execution and visible progress states.
6. NFR6: The application shall provide observable error handling with actionable messages for failed Git operations, authentication failures, and workspace setup issues.
7. NFR7: The application shall support offline access to locally stored project and task metadata, with graceful degradation for external integrations when connectivity is unavailable.
8. NFR8: The application shall be testable through automated unit and integration tests covering domain logic, persistence, Git workflows, and integration boundaries.
9. NFR9: The application shall package into distributable installers for Windows, macOS, and Linux using the project’s native packaging workflow.
10. NFR10: The application shall support secure import and export flows that preserve compatibility across installations without corrupting data or rule attachments.

## User Interface Design Goals

### Overall UX Vision

AiTask should feel like a developer control center rather than a generic task manager. The core experience should prioritize fast orientation, low-friction task startup, and clear visibility into repository, workspace, IDE, and AI-rule context so that users can move from planning to coding with minimal operational overhead.

The interface should balance information density with speed. Power users need rich configuration and multi-repository visibility, but the primary workflow should remain simple: select project, select task, confirm workspace context, and open the task in the right IDE with the right rules already applied.

### Key Interaction Paradigms

- Dashboard-first navigation with clear entry points into projects, active tasks, recent activity, and system health
- Master-detail workflows for projects, repositories, tasks, rules, and integrations
- Guided task-launch flow that confirms repository selection, branch naming, workspace destination, and IDE choice before opening development tools
- Inline status feedback for long-running actions such as clone, branch creation, health checks, credential validation, and IDE launch
- Persistent context panels showing project metadata, repository configuration, applied rules, and current task state
- Safe destructive-action patterns for workspace cleanup, archival, credential replacement, and task deletion

### Core Screens and Views

- Main Dashboard
- Project List and Project Detail View
- Repository Configuration View
- Task List and Task Detail View
- Task Launch / Workspace Preparation Flow
- Rule Management View
- Integrations and Credentials Settings
- Slack Channel Configuration View
- Health Monitoring / Connection Status View
- Import / Export / Backup Restore View

### Accessibility: WCAG AA

The product should target WCAG AA-equivalent desktop accessibility practices where practical, especially for keyboard navigation, visible focus states, readable contrast, semantic labeling, and status messaging for long-running or failed operations.

### Branding

No explicit branding system is defined in the current brief. The interface should therefore aim for a professional developer-tool aesthetic: structured layouts, high information clarity, restrained visual decoration, and strong visual distinction between operational states such as ready, syncing, warning, failed, and completed.

### Target Device and Platforms: Desktop Only

The target experience is a desktop-only application for Windows, macOS, and Linux. Layouts should optimize for laptop and large-screen workflows used by developers, while remaining usable at smaller desktop window sizes without requiring mobile-responsive patterns.

## Technical Assumptions

### Repository Structure: Monorepo

The project should use a single repository for the desktop application, packaging scripts, database migrations, and supporting build assets. This aligns with the current project structure and keeps product, architecture, and release work coordinated in one place for an MVP desktop product.

### Service Architecture

AiTask should be implemented as a desktop-first monolith with modular internal boundaries for UI, application services, persistence, Git integration, rule management, and external integrations. The application will run locally as a standalone desktop client while connecting to a PostgreSQL database and external services such as Git providers and Slack.

This assumption fits the current product scope because the product’s main complexity is orchestration of local workflows and integrations, not distributed runtime scale. A modular monolith reduces delivery overhead, simplifies packaging, and keeps early architecture focused on reliability and developer productivity.

### Testing Requirements

The project should target Unit + Integration testing as the default quality bar for MVP delivery. Unit tests should cover domain logic, validation, mapping, state transitions, and rule-processing behavior, while integration tests should cover persistence, migrations, Git integration boundaries, import/export flows, and external service client behavior with controlled test doubles or fixtures.

Manual testing should also be expected for cross-platform desktop workflows that are difficult to fully automate early, especially installer behavior, IDE launch behavior, workspace generation, and credential-handling flows.

### Additional Technical Assumptions and Requests

- Primary language and runtime should remain Kotlin on JDK 21, matching the current environment and stack documents.
- UI framework should remain JetBrains Compose Multiplatform for Desktop with Material 3 patterns unless architecture review identifies a blocker.
- Build and dependency management should remain Maven-based to align with the current setup and packaging workflow.
- Persistence should use PostgreSQL with Flyway-managed schema migrations and a Kotlin-friendly data access layer consistent with the documented stack.
- Git operations should be implemented through a JVM-native Git library layer, with support for repository validation, branching, commit, and push flows.
- Long-running operations such as clone, sync, import/export, and health checks should execute asynchronously with visible progress and cancellation-safe behavior where practical.
- Secrets and credentials should be encrypted at rest, excluded from logs and exports unless intentionally included in a secure backup design, and handled through a dedicated credential management component.
- Native distribution should target Windows, macOS, and Linux installers using the current packaging approach, including `jpackage`-based builds.
- The architecture should allow adding additional integrations later without reworking the core task, project, and workspace domain model.

## Epic List

1. Epic 1: Foundation and First Task Launch Flow: Establish the desktop application foundation, persistence, basic project and task setup, and the first end-to-end workflow that opens a task workspace in the configured IDE.
2. Epic 2: Multi-Repository Git Automation and Rule Application: Add advanced repository management, task branch automation, secure credential handling, repository validation, and automatic rule-aware workspace preparation.
3. Epic 3: Visibility and Operational Control: Deliver dashboard metrics, activity history, health monitoring, workspace retention controls, and improved management views for ongoing developer operations.
4. Epic 4: External Integrations, Portability, and Distribution: Deliver Slack notifications, OAuth-based integrations, import/export and backup/restore, and production-ready cross-platform packaging.

## Epic 1 Foundation and First Task Launch Flow

Establish the first usable version of AiTask as a desktop application that stores core data locally, allows a developer to define a project and its primary repository, create a task, generate a task workspace, and open that workspace in a configured IDE. This epic proves the product’s primary value proposition in a narrow but deployable form, while creating the technical foundation for later Git automation, multi-repository handling, and integrations.

### Story 1.1 Desktop App Foundation and Persistence Bootstrap

As a developer,
I want the desktop application to start reliably with its local services configured,
so that I can use AiTask as an installable product rather than a prototype.

#### Acceptance Criteria

1. The application launches into a stable desktop shell with navigation placeholders for dashboard, projects, tasks, rules, and settings.
2. The application initializes database connectivity and applies schema migrations automatically on startup or through a supported startup flow.
3. The application displays a clear success or failure state when startup dependencies such as the database are unavailable.
4. The application provides a simple canary experience, such as a home screen or status view, confirming that the desktop app, persistence layer, and base navigation are working.
5. The application logs startup and persistence initialization events without exposing secrets.

### Story 1.2 Basic Project Creation with Single Repository Configuration

As a developer,
I want to create a project with its primary repository and workspace settings,
so that AiTask can prepare task work for a real codebase.

#### Acceptance Criteria

1. A user can create, edit, view, and archive a project with name, description, workspace path, and branch naming template.
2. A user can attach one primary repository to the project with provider, clone URL, and repository name or label.
3. A user can define at least one preferred IDE for the repository from the supported IDE list.
4. Project data persists between application sessions.
5. Validation prevents saving incomplete or clearly invalid project or repository configurations and gives actionable feedback.

### Story 1.3 Task Management for the Core Workflow

As a developer,
I want to create and manage tasks within a project,
so that I can track work items that will drive workspace generation and IDE launch.

#### Acceptance Criteria

1. A user can create, view, edit, archive, and delete tasks within an existing project.
2. A task records at minimum a title, description, task type, and status.
3. Supported task types include Feature, Bug Fix, Research, Enhancement, Documentation, and Refactoring.
4. Supported task statuses include Pending, In Progress, Completed, and Archived.
5. Task lists can be filtered by project and status.

### Story 1.4 Workspace Generation for a Selected Task

As a developer,
I want AiTask to generate a local workspace for a selected task,
so that I do not need to manually prepare folders and local context before starting work.

#### Acceptance Criteria

1. A user can trigger workspace generation from a task detail or task action flow.
2. The application creates the workspace in the configured project path using task-identifying information.
3. The application retrieves the project’s configured primary repository into the generated workspace using the current repository retrieval strategy.
4. The application provides visible progress and final success or failure feedback during workspace preparation.
5. If workspace generation fails, the application leaves the task and project data intact and provides an actionable error message.

### Story 1.5 Launch Task Workspace in Configured IDE

As a developer,
I want to open a prepared task workspace in my configured IDE,
so that I can move directly from task selection into coding.

#### Acceptance Criteria

1. A user can launch the configured IDE from the selected task after workspace preparation completes successfully.
2. The application only presents IDE options configured for the project’s repository.
3. The application opens the prepared workspace path in the selected IDE.
4. The task launch flow updates the task status to In Progress when the workspace is opened or when the user explicitly starts the task, based on the final UX decision.
5. The application records a task-launch activity entry for successful workspace opening.

## Epic 2 Multi-Repository Git Automation and Rule Application

Expand the initial task-launch workflow so AiTask can support more realistic developer environments with multiple repositories, task-specific branches, secure credential handling, and automated application of rule sets tied to projects, repositories, IDEs, and AI tools. This epic deepens the product’s core differentiation by reducing the manual setup work that still remains after the first launch flow is in place.

### Story 2.1 Multi-Repository Project Configuration

As a developer,
I want to configure multiple repositories within a project,
so that AiTask can support real-world codebases that span several services or modules.

#### Acceptance Criteria

1. A user can add, edit, archive, and remove multiple repositories within a project.
2. Each repository stores provider, clone URL, label, local role or purpose, and one or more preferred IDE options.
3. The application clearly distinguishes the primary repository from additional repositories when configured.
4. Repository configuration changes persist between sessions.
5. Validation prevents duplicate or invalid repository definitions within the same project.

### Story 2.2 Task Workspace Repository Selection

As a developer,
I want to choose which repositories are included in a task workspace,
so that I can prepare only the codebases relevant to the current task.

#### Acceptance Criteria

1. The task launch flow allows the user to select one, many, or all configured repositories for inclusion in the workspace.
2. The application remembers or suggests sensible default repository selections for repeated use where project configuration supports it.
3. The workspace preparation summary clearly shows which repositories will be retrieved before the user confirms launch.
4. The system creates a coherent workspace structure for the selected repositories.
5. If one selected repository fails during preparation, the application reports which repository failed and what remains usable.

### Story 2.3 Task Branch Automation and Repository Validation

As a developer,
I want AiTask to validate repositories and create task-specific branches automatically,
so that I can start work with the correct Git state without manual setup.

#### Acceptance Criteria

1. The application validates repository access and basic repository metadata before or during workspace preparation.
2. The application creates task-specific branches for selected repositories using the project’s branch naming template.
3. The application prevents branch creation from proceeding when repository validation fails and provides actionable error feedback.
4. The application records Git preparation outcomes in task activity history.
5. The branch automation flow works consistently across supported Git providers.

### Story 2.4 Secure Git Credentials and Access Configuration

As a developer,
I want AiTask to handle repository credentials securely,
so that I can access repositories across providers without exposing secrets or repeating setup unnecessarily.

#### Acceptance Criteria

1. The application supports configuring repository access through SSH or HTTPS authentication.
2. The application can use local SSH configuration when available to resolve account-specific key mappings.
3. HTTPS credentials and related secrets are stored in encrypted form.
4. The application provides clear feedback when credentials are missing, invalid, or insufficient for repository access.
5. Credential-handling flows avoid exposing secrets in logs, UI messages, or exported data.

### Story 2.5 Rule Set Management Across Projects and Repositories

As a developer,
I want to manage reusable AI and IDE rule sets,
so that I can apply consistent development guidance across projects and tools.

#### Acceptance Criteria

1. A user can create, edit, archive, attach, and detach reusable rule sets.
2. Rule sets can be associated at global, project, repository, and IDE or AI-tool levels.
3. The application makes the effective rule associations visible before task launch.
4. Rule set changes persist between sessions and remain linked to their assigned scope.
5. The application supports importing and exporting rule sets independently of full project backup flows.

### Story 2.6 Rule-Aware Workspace Preparation

As a developer,
I want AiTask to apply the correct rules when preparing my task workspace,
so that my IDE and AI tools start with the right project context automatically.

#### Acceptance Criteria

1. During task launch preparation, the application determines the effective rules for the selected project, repositories, IDE, and AI-tool context.
2. The application applies those effective rules to the workspace in a way compatible with the selected IDE or AI tooling.
3. The launch flow shows which rule sets were applied or skipped.
4. If rule application partially fails, the application reports the issue without masking repository or workspace preparation status.
5. Successful rule application is recorded in task activity history.

## Epic 3 Visibility and Operational Control

Strengthen AiTask as an everyday operating console by giving developers and team leads clear visibility into task progress, system status, recent actions, and workspace lifecycle controls. This epic improves operational confidence and usability after the core workspace-launch automation is in place, making the product easier to manage over time and across multiple projects.

### Story 3.1 Dashboard Overview for Active Work

As a developer,
I want a dashboard that summarizes my projects and tasks,
so that I can quickly understand current work without opening each item individually.

#### Acceptance Criteria

1. The dashboard shows summary metrics for projects, tasks by status, and recently active work.
2. The dashboard highlights tasks currently in progress or recently launched.
3. The dashboard provides navigation shortcuts into the most relevant project and task views.
4. Dashboard content reflects persisted project and task data accurately after restart.
5. Empty or first-use states provide clear guidance rather than blank panels.

### Story 3.2 Activity History and Recent Events

As a developer,
I want to review recent application activity,
so that I can understand what happened during task setup, launch, and management workflows.

#### Acceptance Criteria

1. The application records and displays recent activity entries for key events including task creation, workspace preparation, IDE launch, Git preparation, and rule application.
2. Activity entries include enough context to identify the relevant project, task, action, and result.
3. Users can view activity in chronological order with the most recent events first.
4. Users can filter activity by project, task, or event type.
5. Failed actions are visually distinguishable from successful actions in the activity history.

### Story 3.3 Health Monitoring and Connectivity Status

As a developer,
I want visibility into system and integration health,
so that I can diagnose why AiTask may not be able to complete workflow automation.

#### Acceptance Criteria

1. The application displays current health status for the database, configured repositories, and enabled external integrations.
2. Health checks distinguish between healthy, degraded, and failed states where applicable.
3. The user can manually refresh health status from the UI.
4. Failure states include actionable context rather than generic error labels.
5. Health status does not block normal browsing of locally available project and task data when an external service is unavailable.

### Story 3.4 Workspace Retention and Cleanup Controls

As a developer,
I want control over what happens to task workspaces after work changes state,
so that I can balance disk usage with safety for unfinished or reference work.

#### Acceptance Criteria

1. Users can configure workspace behavior for completed and deleted tasks, including retain, archive, or clean up options.
2. The application warns users before destructive cleanup actions that remove local workspace data.
3. Completed tasks clearly show the current workspace state or retention outcome.
4. Cleanup or archival actions are recorded in activity history.
5. Failed cleanup actions do not remove task metadata and provide actionable feedback.

### Story 3.5 Enhanced Management Views and Filtering

As a developer,
I want stronger filtering and management views for projects, tasks, and rules,
so that I can operate efficiently as the amount of tracked work grows.

#### Acceptance Criteria

1. Project, task, and rule views support search and filtering based on relevant metadata.
2. The task view supports filtering by status, task type, and project.
3. The project view supports filtering by tags, teams, or other configured metadata when available.
4. Rule management views support locating rules by scope or linked project or repository.
5. Filter and search interactions return results quickly enough to support normal desktop productivity use.

## Epic 4 External Integrations, Portability, and Distribution

Extend AiTask beyond local workflow orchestration by adding external notifications, portable data movement, and production-ready packaging for end-user installation. This epic prepares the product for broader real-world adoption by making it easier to connect with external systems, move data between environments, and distribute the application across supported desktop platforms.

### Story 4.1 Slack Notification Configuration and Delivery

As a team-oriented developer,
I want AiTask to send task updates to Slack,
so that project activity is visible in the communication tools my team already uses.

#### Acceptance Criteria

1. A user can configure Slack integration settings for a project, including destination channel details.
2. A user can choose which task lifecycle events trigger Slack notifications.
3. The application sends notifications for configured events with enough context to identify the relevant task and project.
4. Failed Slack delivery attempts provide visible feedback without blocking the underlying task action.
5. Slack notification activity is recorded in the application history.

### Story 4.2 OAuth-Based External Integration Access

As a developer,
I want AiTask to use a reusable external authentication framework,
so that integrations can connect securely without bespoke login handling for each provider.

#### Acceptance Criteria

1. The application supports an OAuth2-based authorization flow for supported external integrations.
2. The authentication state for configured integrations is visible in the application settings or integration views.
3. The application can detect expired or invalid external authorization and prompt the user to reconnect.
4. External authentication secrets or tokens are stored securely.
5. The OAuth-based framework is reusable for more than one external integration type.

### Story 4.3 Import and Export of Projects, Tasks, and Rules

As a developer,
I want to export and import my AiTask data,
so that I can move project configurations and work context between installations.

#### Acceptance Criteria

1. A user can export projects, tasks, repositories, and rules into a structured portable format.
2. A user can import previously exported data into another installation or environment.
3. The import flow validates incoming data and reports conflicts or incompatible content before applying changes.
4. Imported data preserves key relationships between projects, tasks, repositories, and rules.
5. The application confirms successful import and export completion with a clear summary of processed records.

### Story 4.4 Backup and Restore for Local Recovery

As a developer,
I want backup and restore capabilities,
so that I can recover my AiTask data after environment loss, migration, or corruption.

#### Acceptance Criteria

1. A user can create a backup of application data suitable for later restoration.
2. A user can restore from a previously created backup through a guided recovery flow.
3. The restore flow warns users about overwrite or merge implications before applying restored data.
4. Restore operations validate backup integrity before changing current data.
5. Backup and restore actions are recorded in the application activity history.

### Story 4.5 Cross-Platform Packaging and Installer Readiness

As a developer,
I want AiTask to be packaged for my operating system,
so that I can install and run it as a real desktop product.

#### Acceptance Criteria

1. The application can be packaged into installable distributions for Windows, macOS, and Linux.
2. Packaged builds launch successfully into the expected desktop shell and core startup flow.
3. Packaging outputs are versioned and suitable for repeatable release creation.
4. Installer or package validation confirms that required runtime dependencies are included or clearly documented.
5. Release packaging does not expose secrets, environment-specific credentials, or development-only configuration.
