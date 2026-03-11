# Epic 2: Multi-Repository Git Automation and Rule Application

## Epic Goal

Expand the initial task-launch workflow so AiTask can support more realistic developer environments with multiple repositories, task-specific branches, secure credential handling, and automated application of rule sets tied to projects, repositories, IDEs, and AI tools. This epic deepens the product's core differentiation by reducing the manual setup work that remains after the first launch flow is in place.

## Requirements Mapping

- **FR2, FR9–FR15:** Multi-repository, Git automation, credentials
- **FR18–FR20:** Rule management and application
- **NFR3, NFR6:** Security, error handling

## Architecture References

- [Component Architecture: Git Integration](../../architecture.md#3-git-integration-component)
- [Component Architecture: Rule Management](../../architecture.md#5-rule-management-component)
- [Component Architecture: Workspace Management](../../architecture.md#6-workspace-management-component)
- [Security Architecture: Credential Management](../../architecture.md#credential-management)
- [Integration Architecture: Git Provider Integration](../../architecture.md#1-git-provider-integration)
- [Workspace Structure and Metadata](../../architecture.md#workspace-structure)

## UX References

- [Flow 1: Task Launch – Multi-repo selection](../../front-end-spec.md#flow-1-task-launch-primary-mvp-flow)
- [Flow 3: Project and Repository Setup](../../front-end-spec.md#flow-3-project-and-repository-setup)
- [Task Launch Flow (Stepper)](../../front-end-spec.md#3-task-launch-flow-modal-or-stepper)
- [Rule Management screen](../../front-end-spec.md#4-rule-management)

---

## Story 2.1: Multi-Repository Project Configuration

**As a** developer,  
**I want** to configure multiple repositories within a project,  
**so that** AiTask can support real-world codebases that span several services or modules.

### Acceptance Criteria

1. A user can add, edit, archive, and remove multiple repositories within a project.
2. Each repository stores provider, clone URL, label, local role or purpose, and one or more preferred IDE options.
3. The application clearly distinguishes the primary repository from additional repositories when configured.
4. Repository configuration changes persist between sessions.
5. Validation prevents duplicate or invalid repository definitions within the same project.

---

## Story 2.2: Task Workspace Repository Selection

**As a** developer,  
**I want** to choose which repositories are included in a task workspace,  
**so that** I can prepare only the codebases relevant to the current task.

### Acceptance Criteria

1. The task launch flow allows the user to select one, many, or all configured repositories for inclusion in the workspace.
2. The application remembers or suggests sensible default repository selections for repeated use where project configuration supports it.
3. The workspace preparation summary clearly shows which repositories will be retrieved before the user confirms launch.
4. The system creates a coherent workspace structure for the selected repositories.
5. If one selected repository fails during preparation, the application reports which repository failed and what remains usable.

---

## Story 2.3: Task Branch Automation and Repository Validation

**As a** developer,  
**I want** AiTask to validate repositories and create task-specific branches automatically,  
**so that** I can start work with the correct Git state without manual setup.

### Acceptance Criteria

1. The application validates repository access and basic repository metadata before or during workspace preparation.
2. The application creates task-specific branches for selected repositories using the project's branch naming template.
3. The application prevents branch creation from proceeding when repository validation fails and provides actionable error feedback.
4. The application records Git preparation outcomes in task activity history.
5. The branch automation flow works consistently across supported Git providers.

---

## Story 2.4: Secure Git Credentials and Access Configuration

**As a** developer,  
**I want** AiTask to handle repository credentials securely,  
**so that** I can access repositories across providers without exposing secrets or repeating setup unnecessarily.

### Acceptance Criteria

1. The application supports configuring repository access through SSH or HTTPS authentication.
2. The application can use local SSH configuration when available to resolve account-specific key mappings.
3. HTTPS credentials and related secrets are stored in encrypted form.
4. The application provides clear feedback when credentials are missing, invalid, or insufficient for repository access.
5. Credential-handling flows avoid exposing secrets in logs, UI messages, or exported data.

---

## Story 2.5: Rule Set Management Across Projects and Repositories

**As a** developer,  
**I want** to manage reusable AI and IDE rule sets,  
**so that** I can apply consistent development guidance across projects and tools.

### Acceptance Criteria

1. A user can create, edit, archive, attach, and detach reusable rule sets.
2. Rule sets can be associated at global, project, repository, and IDE or AI-tool levels.
3. The application makes the effective rule associations visible before task launch.
4. Rule set changes persist between sessions and remain linked to their assigned scope.
5. The application supports importing and exporting rule sets independently of full project backup flows.

---

## Story 2.6: Rule-Aware Workspace Preparation

**As a** developer,  
**I want** AiTask to apply the correct rules when preparing my task workspace,  
**so that** my IDE and AI tools start with the right project context automatically.

### Acceptance Criteria

1. During task launch preparation, the application determines the effective rules for the selected project, repositories, IDE, and AI-tool context.
2. The application applies those effective rules to the workspace in a way compatible with the selected IDE or AI tooling.
3. The launch flow shows which rule sets were applied or skipped.
4. If rule application partially fails, the application reports the issue without masking repository or workspace preparation status.
5. Successful rule application is recorded in task activity history.
