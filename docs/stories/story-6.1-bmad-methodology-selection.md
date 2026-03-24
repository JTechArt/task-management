# Story 6.1: BMAD as Selectable Methodology

**Epic:** Epic 6 - BMAD Methodology Integration

**As a** developer using the BMAD method,  
**I want** to select BMAD as the methodology for a project or task,  
**so that** AiTask can configure my workspace and tools accordingly.

## Status

Done

## Acceptance Criteria

1. A user can select "BMAD" as the methodology for a project or task from a dropdown or configuration panel.
2. BMAD is available alongside other methodology options (e.g., None, Custom) in the project and task configuration.
3. Methodology selection persists at project level with optional override at task level.
4. The selection is visible in the project detail and task detail views.
5. Changing methodology does not destructively alter existing rule sets or workspace content without user confirmation.

## Requirements Mapping

- BMAD-1: BMAD as selectable methodology for projects and tasks
- BMAD-4: Configurable at project level with override at task level

## Architecture References

- [Component Architecture: Project Management](../architecture.md#1-project-management-component)
- [Component Architecture: Rule Management](../architecture.md#5-rule-management-component)

## UX References

- [Front-end Spec: Methodology & BMAD Configuration](../front-end-spec.md#12-methodology--bmad-configuration)
- [Mockup: Methodology](../mockups/methodology.html)
- [Mockup: Project Detail](../mockups/project-detail.html)

## Tasks / Subtasks

- [x] Task 1: Add methodology field to project and task models
  - [x] Migration for methodology column (project, task)
  - [x] Methodology enum: NONE, BMAD, CUSTOM
- [x] Task 2: Add methodology selection UI to project and task configuration
  - [x] Dropdown in project detail
  - [x] Optional override in task detail
- [x] Task 3: Persistence and validation
  - [x] Confirm dialog when changing methodology and rule sets exist
- [x] Task 4: Unit tests for model and configuration logic

## Dev Notes

- Methodology drives downstream behavior (workspace injection, tool pre-selection) in later stories
- BMAD-2 injection and BMAD-3 tools are in stories 6.2 and 6.3

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `./gradlew test`

### Completion Notes List

- Added persisted methodology support with `Methodology` enum, project-level methodology, and nullable task-level methodology override.
- Added migration `V11__add_methodology_selection.sql` and updated repositories/use cases to store and clear methodology selections safely.
- Added methodology selectors to project creation, project detail, task creation, and task detail UI, including confirmation when changing a project methodology that already has attached rule sets.
- Added and updated unit tests covering methodology creation, persistence, and override clearing behavior.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Methodology.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Project.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Task.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/ProjectEntity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/TaskEntity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/ProjectRepositoryImpl.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/TaskRepositoryImpl.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/CreateProjectUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/UpdateProjectUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/CreateTaskUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/UpdateTaskUseCase.kt`
- `taskmanager/core/src/main/resources/db/migration/V11__add_methodology_selection.sql`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ProjectsViewModel.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/CreateProjectDialog.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectsView.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectComponents.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/CreateTaskDialog.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/CreateProjectUseCaseTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/CreateTaskUseCaseTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/UpdateTaskUseCaseTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/validation/ProjectValidatorTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/validation/TaskValidatorTest.kt`

### Change Log

- Added selectable methodology persistence and UI for projects and tasks.

## QA Results

### Review Date: 2026-03-23

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation is clean and well-structured. The Methodology enum (NONE, BMAD, CUSTOM) is consistently used across domain models, entities, and UI. Project-level methodology with optional task-level override follows the intended design. The `effectiveMethodology(projectMethodology)` function on Task correctly resolves inheritance. Confirmation dialog is shown when changing project methodology and the project has attached rule sets.

### Refactoring Performed

None. No refactoring required; implementation adheres to existing patterns.

### Compliance Check

- Coding Standards: ✓ Kotlin guidelines followed; types explicit, camelCase/PascalCase correct
- Project Structure: ✓ Files in correct modules (core/domain, core/data, desktop-app)
- Testing Strategy: ✓ Unit tests for CreateProject, CreateTask, UpdateTask with methodology; ProjectValidator, TaskValidator include methodology
- All ACs Met: ✓

### AC Traceability

| AC | Description | Evidence |
|----|-------------|----------|
| 1 | Select BMAD from dropdown/configuration | CreateProjectDialog, CreateTaskDialog, ProjectDetailView, TaskDetailView all offer Methodology dropdown |
| 2 | BMAD alongside NONE, CUSTOM | `Methodology` enum has NONE, BMAD, CUSTOM |
| 3 | Project-level persistence, task override | Project.methodology, Task.methodologyOverride, migration V11, UpdateProject/UpdateTask use cases |
| 4 | Visible in detail views | ProjectDetailView DetailRow("Methodology"), TaskDetailView DetailRow("Effective Methodology", "Methodology Override") |
| 5 | No destructive change without confirmation | AlertDialog when hasAttachedRules && selectedMethodology != project.methodology; message states rule sets are not deleted |

### Test Coverage

- CreateProjectUseCaseTest: methodology = BMAD persisted
- CreateTaskUseCaseTest: methodologyOverride = BMAD persisted
- UpdateTaskUseCaseTest: clearMethodologyOverride behavior
- ProjectValidatorTest, TaskValidatorTest: methodology in valid requests

### Gate Status

Gate: PASS → docs/qa/gates/6.1-bmad-methodology-selection.yml

### Recommended Status

✓ Ready for Done
