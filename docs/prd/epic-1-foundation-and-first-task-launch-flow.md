# Epic 1: Foundation and First Task Launch Flow

## Epic Goal

Establish the first usable version of AiTask as a desktop application that stores core data locally, allows a developer to define a project and its primary repository, create a task, generate a task workspace, and open that workspace in a configured IDE. This epic proves the product's primary value proposition in a narrow but deployable form while creating the technical foundation for later Git automation, multi-repository handling, and integrations.

## Requirements Mapping

- **FR1–FR4:** Project and repository configuration
- **FR5–FR7:** Task management
- **FR8–FR10:** Workspace generation
- **FR11–FR18:** Git and IDE integration (basic)
- **NFR1, NFR2, NFR4–NFR6:** Desktop experience, performance, error handling

## Architecture References

- [Component Architecture: Project Management](../../architecture.md#1-project-management-component)
- [Component Architecture: Task Management](../../architecture.md#2-task-management-component)
- [Component Architecture: IDE Integration](../../architecture.md#4-ide-integration-component)
- [Component Architecture: Workspace Management](../../architecture.md#6-workspace-management-component)
- [Database Schema: projects, repositories, tasks](../../architecture.md#database-schema)
- [Project Structure: taskmanager/](../../architecture.md#project-structure)

## UX References

- [Flow 1: Task Launch (Primary MVP Flow)](../../front-end-spec.md#flow-1-task-launch-primary-mvp-flow)
- [Flow 2: Create and Start New Task](../../front-end-spec.md#flow-2-create-and-start-new-task)
- [Flow 3: Project and Repository Setup](../../front-end-spec.md#flow-3-project-and-repository-setup)
- [Screen Layouts: Dashboard, Project Detail, Task Launch Flow](../../front-end-spec.md#key-screen-layouts)

---

## Story 1.1: Desktop App Foundation and Persistence Bootstrap

**As a** developer,  
**I want** the desktop application to start reliably with its local services configured,  
**so that** I can use AiTask as an installable product rather than a prototype.

### Acceptance Criteria

1. The application launches into a stable desktop shell with navigation placeholders for dashboard, projects, tasks, rules, and settings.
2. The application initializes database connectivity and applies schema migrations automatically on startup or through a supported startup flow.
3. The application displays a clear success or failure state when startup dependencies such as the database are unavailable.
4. The application provides a simple canary experience, such as a home screen or status view, confirming that the desktop app, persistence layer, and base navigation are working.
5. The application logs startup and persistence initialization events without exposing secrets.

---

## Story 1.2: Basic Project Creation with Single Repository Configuration

**As a** developer,  
**I want** to create a project with its primary repository and workspace settings,  
**so that** AiTask can prepare task work for a real codebase.

### Acceptance Criteria

1. A user can create, edit, view, and archive a project with name, description, workspace path, and branch naming template.
2. A user can attach one primary repository to the project with provider, clone URL, and repository name or label.
3. A user can define at least one preferred IDE for the repository from the supported IDE list.
4. Project data persists between application sessions.
5. Validation prevents saving incomplete or clearly invalid project or repository configurations and gives actionable feedback.

---

## Story 1.3: Task Management for the Core Workflow

**As a** developer,  
**I want** to create and manage tasks within a project,  
**so that** I can track work items that will drive workspace generation and IDE launch.

### Acceptance Criteria

1. A user can create, view, edit, archive, and delete tasks within an existing project.
2. A task records at minimum a title, description, task type, and status.
3. Supported task types include Feature, Bug Fix, Research, Enhancement, Documentation, and Refactoring.
4. Supported task statuses include Pending, In Progress, Completed, and Archived.
5. Task lists can be filtered by project and status.

---

## Story 1.4: Workspace Generation for a Selected Task

**As a** developer,  
**I want** AiTask to generate a local workspace for a selected task,  
**so that** I do not need to manually prepare folders and local context before starting work.

### Acceptance Criteria

1. A user can trigger workspace generation from a task detail or task action flow.
2. The application creates the workspace in the configured project path using task-identifying information.
3. The application retrieves the project's configured primary repository into the generated workspace using the current repository retrieval strategy.
4. The application provides visible progress and final success or failure feedback during workspace preparation.
5. If workspace generation fails, the application leaves the task and project data intact and provides an actionable error message.

---

## Story 1.5: Launch Task Workspace in Configured IDE

**As a** developer,  
**I want** to open a prepared task workspace in my configured IDE,  
**so that** I can move directly from task selection into coding.

### Acceptance Criteria

1. A user can launch the configured IDE from the selected task after workspace preparation completes successfully.
2. The application only presents IDE options configured for the project's repository.
3. The application opens the prepared workspace path in the selected IDE.
4. The task launch flow updates the task status to In Progress when the workspace is opened or when the user explicitly starts the task, based on the final UX decision.
5. The application records a task-launch activity entry for successful workspace opening.
