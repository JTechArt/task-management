# Story 6.3: BMAD Recommended Tools Pre-Selection

**Epic:** Epic 6 - BMAD Methodology Integration

**As a** developer,  
**I want** BMAD-recommended tools (agents, workflows, tasks, checklists) to be pre-selected when BMAD is chosen,  
**so that** I can use the full BMAD workflow without manually enabling each tool.

## Status

Done

## Acceptance Criteria

1. BMAD-recommended tools (agents, checklists, tasks) appear pre-selected in the project or task configuration.
2. The system surfaces BMAD tool selection in the project/task configuration UI.
3. A user can customize which BMAD tools are active per project or task.
4. Pre-selection aligns with BMAD-Method documentation where applicable.
5. Tool configuration is persisted and applied during task execution or agent invocation.

## Requirements Mapping

- BMAD-3: BMAD-recommended tools automatically selected
- BMAD-5: BMAD tool selection surfaced in project/task configuration UI

## Dependencies

- Story 6.1: BMAD as Selectable Methodology

## Architecture References

- [Component Architecture: Rule Management](../architecture.md#5-rule-management-component)
- [Component Architecture: Project Management](../architecture.md#1-project-management-component)

## UX References

- [Front-end Spec: Methodology & BMAD Configuration](../front-end-spec.md#12-methodology--bmad-configuration)
- [Mockup: Methodology](../mockups/methodology.html)

## Tasks / Subtasks

- [x] Task 1: Define BMAD default tool set (agents, checklists, tasks)
  - [x] Reference BMAD-Method documentation for default list
  - [x] Store as configurable defaults
- [x] Task 2: Implement pre-selection logic when methodology is BMAD
  - [x] Apply defaults to project or task on BMAD selection
  - [x] Allow enable/disable per tool
- [x] Task 3: Add BMAD tool configuration UI
  - [x] List agents, checklists, tasks with checkboxes
  - [x] Persist selections
- [x] Task 4: Wire tool selection to task execution and agent invocation
- [x] Task 5: Unit tests for pre-selection and persistence

## Dev Notes

- Integrate with existing rule management where applicable
- Tool list may be derived from .bmad-core structure when BMAD is injected

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `./gradlew test`

### Completion Notes List

- Added a persisted BMAD tool catalog with default agent, task, and checklist selections aligned to the repo’s BMAD assets.
- Added project-level BMAD tool selections plus optional task-level overrides, with default pre-selection when BMAD methodology is enabled.
- Added BMAD tool checkbox configuration to the project and task detail panels and persisted selections through the existing project/task update flows.
- Extended IDE launch task context generation so active BMAD tools are written into `TASK_CONTEXT.md`, making the configured tool set available during launch-time agent workflows.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/BmadTool.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Project.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Task.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/IDE.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/ProjectEntity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/TaskEntity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/ProjectRepositoryImpl.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/TaskRepositoryImpl.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/CreateProjectUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/UpdateProjectUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/CreateTaskUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/UpdateTaskUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/LaunchIDEUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/ide/DesktopIDEService.kt`
- `taskmanager/core/src/main/resources/db/migration/V12__add_bmad_tool_selection.sql`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ProjectsViewModel.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectsView.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectComponents.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/CreateProjectUseCaseTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/CreateTaskUseCaseTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/UpdateTaskUseCaseTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/LaunchIDEUseCaseTest.kt`

### Change Log

- Added BMAD tool pre-selection, persistence, task overrides, and launch-context propagation.

## QA Results

### Review Date: 2026-03-23

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation is well-structured. `BmadToolCatalog` defines agents, tasks, and checklists aligned with AGENTS.md. Project-level `bmadToolIds` and task-level `bmadToolOverrideIds` (nullable) with `effectiveBmadToolIds(projectBmadToolIds)` for inheritance. Pre-selection applied in CreateProjectUseCase, CreateTaskUseCase, UpdateProjectUseCase, UpdateTaskUseCase when methodology is BMAD and tools empty. UI shows BMAD tool section only when methodology is BMAD. TASK_CONTEXT.md written at IDE launch with active tool labels.

### Refactoring Performed

None. Implementation follows existing patterns.

### Compliance Check

- Coding Standards: ✓ Kotlin conventions followed
- Project Structure: ✓ BmadTool in domain/model; migration V12; repositories handle TEXT[]
- Testing Strategy: ✓ CreateProject (BMAD defaults), CreateTask (BMAD override defaults), UpdateTask (bmadToolOverrideIds), LaunchIDE (activeBmadTools in TaskContext)
- All ACs Met: ✓

### AC Traceability

| AC | Description | Evidence |
|----|-------------|----------|
| 1 | BMAD tools pre-selected in project/task config | CreateProjectUseCase, CreateTaskUseCase, UpdateProjectUseCase, UpdateTaskUseCase apply BmadToolCatalog.defaultToolIds when methodology=BMAD and tools empty |
| 2 | BMAD tool selection surfaced in UI | ProjectDetailView, TaskDetailView show BmadToolSelectionSection when methodology=BMAD; BmadToolCheckboxRow per tool |
| 3 | User can customize per project/task | Checkboxes with Save; task has Reset to Use Project Defaults; ViewModels call updateProjectBmadTools, updateTaskBmadTools, clearTaskBmadToolOverride |
| 4 | Pre-selection aligns with BMAD docs | BmadToolCatalog: agents (ux-expert, sm, qa, po, pm, dev, etc.), tasks (create-next-story, review-story, etc.), checklists (story-dod, story-draft, po-master, pm, architect) |
| 5 | Persisted and applied at execution | Migration V12; LaunchIDEUseCase builds TaskContext with activeBmadTools from effectiveBmadToolIds; DesktopIDEService writes TASK_CONTEXT.md with Active BMAD Tools section |

### Test Coverage

- CreateProjectUseCaseTest: project with methodology=BMAD gets defaultToolIds
- CreateTaskUseCaseTest: task with methodologyOverride=BMAD gets defaultToolIds
- UpdateTaskUseCaseTest: bmadToolOverrideIds persisted; clearBmadToolOverrideIds
- LaunchIDEUseCaseTest: activeBmadTools in TaskContext when project uses BMAD

### Gate Status

Gate: PASS → docs/qa/gates/6.3-bmad-tool-pre-selection.yml

### Recommended Status

✓ Ready for Done
