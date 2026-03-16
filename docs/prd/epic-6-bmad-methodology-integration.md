# Epic 6: BMAD Methodology Integration

## Epic Goal

Integrate the BMAD (BMAD-Method) approach into AiTask so that when a user selects BMAD for a project or task, the workspace is automatically configured with BMAD tooling and recommended tools (agents, workflows, tasks, checklists) are pre-selected. This enables teams adopting BMAD to get started quickly without manual setup.

## Requirements Mapping

- **BMAD-1–BMAD-5:** BMAD as selectable methodology, setup injection, recommended tools, project/task-level config, UI surfacing
- **NFR:** Usability, configuration persistence

## Dependencies

- Builds on Epic 1 (Project/Task configuration), Epic 2 (Rule Management)
- Integrates with workspace generation and project detail UI

## Architecture References

- [Component Architecture: Project Management](../../architecture.md#1-project-management-component)
- [Component Architecture: Rule Management](../../architecture.md#5-rule-management-component)
- [Component Architecture: Workspace Management](../../architecture.md#6-workspace-management-component)

---

## Story 6.1: BMAD as Selectable Methodology

**As a** developer using the BMAD method,  
**I want** to select BMAD as the methodology for a project or task,  
**so that** AiTask can configure my workspace and tools accordingly.

### Acceptance Criteria

1. A user can select "BMAD" as the methodology for a project or task from a dropdown or configuration panel.
2. BMAD is available alongside other methodology options (e.g., None, Custom) in the project and task configuration.
3. Methodology selection persists at project level with optional override at task level.
4. The selection is visible in the project detail and task detail views.
5. Changing methodology does not destructively alter existing rule sets or workspace content without user confirmation.

---

## Story 6.2: BMAD Setup Injection into Workspace

**As a** developer,  
**I want** BMAD setup files to be automatically injected into the task workspace when BMAD is selected,  
**so that** I have the correct BMAD structure (.bmad-core, AGENTS.md) without manual copying.

### Acceptance Criteria

1. Upon selecting BMAD, the system injects or links BMAD setup files (e.g., `.bmad-core`, `AGENTS.md`) into the task workspace during workspace generation.
2. Injected files are placed in the expected locations and do not overwrite existing BMAD content unless configured to do so.
3. The injection occurs as part of workspace preparation before IDE launch.
4. Injection failures are reported clearly and do not silently skip.
5. Workspace generation activity records whether BMAD injection was applied.

---

## Story 6.3: BMAD Recommended Tools Pre-Selection

**As a** developer,  
**I want** BMAD-recommended tools (agents, workflows, tasks, checklists) to be pre-selected when BMAD is chosen,  
**so that** I can use the full BMAD workflow without manually enabling each tool.

### Acceptance Criteria

1. BMAD-recommended tools (agents, checklists, tasks) appear pre-selected in the project or task configuration.
2. The system surfaces BMAD tool selection in the project/task configuration UI.
3. A user can customize which BMAD tools are active per project or task.
4. Pre-selection aligns with BMAD-Method documentation where applicable.
5. Tool configuration is persisted and applied during task execution or agent invocation.

---

## Story 6.4: BMAD Configuration Override at Task Level

**As a** developer,  
**I want** to override BMAD configuration at the task level when needed,  
**so that** I can deviate from the project default for specific tasks without affecting other work.

### Acceptance Criteria

1. BMAD integration is configurable at project level with override capability at task level.
2. Task-level overrides are clearly indicated in the UI and take precedence for that task.
3. When no task override exists, project-level BMAD config applies.
4. Override changes do not affect other tasks or the project default.
5. The override state is visible in both project and task detail views.
