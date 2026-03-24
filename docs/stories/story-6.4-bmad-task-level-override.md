# Story 6.4: BMAD Configuration Override at Task Level

**Epic:** Epic 6 - BMAD Methodology Integration

**As a** developer,  
**I want** to override BMAD configuration at the task level when needed,  
**so that** I can deviate from the project default for specific tasks without affecting other work.

## Status

Done

## Acceptance Criteria

1. BMAD integration is configurable at project level with override capability at task level.
2. Task-level overrides are clearly indicated in the UI and take precedence for that task.
3. When no task override exists, project-level BMAD config applies.
4. Override changes do not affect other tasks or the project default.
5. The override state is visible in both project and task detail views.

## Requirements Mapping

- BMAD-4: BMAD integration configurable at project level with override at task level

## Dependencies

- Story 6.1: BMAD as Selectable Methodology
- Story 6.2: BMAD Setup Injection into Workspace
- Story 6.3: BMAD Recommended Tools Pre-Selection

## Architecture References

- [Component Architecture: Project Management](../architecture.md#1-project-management-component)
- [Component Architecture: Task Management](../architecture.md#2-task-management-component)

## UX References

- [Front-end Spec: Methodology & BMAD Configuration](../front-end-spec.md#12-methodology--bmad-configuration)
- [Mockup: Methodology](../mockups/methodology.html)
- [Mockup: Tasks](../mockups/tasks.html)

## Tasks / Subtasks

- [x] Task 1: Model task-level BMAD override (methodology, tools, injection options)
  - [x] Nullable override fields on task
  - [x] Migration if needed
- [x] Task 2: Implement override resolution (task overrides project when set)
  - [x] Use case or service to resolve effective BMAD config per task
- [x] Task 3: Update task detail UI
  - [x] Override indicators and controls
  - [x] Clear visual distinction from project default
- [x] Task 4: Ensure workspace generation and tool invocation use resolved config
- [x] Task 5: Unit tests for override resolution

## Dev Notes

- Override applies to: methodology selection, tool enable/disable, possibly injection options
- Project default remains unchanged when task overrides; other tasks unaffected

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `./gradlew test`

### Completion Notes List

- Added explicit effective BMAD configuration resolution via `BmadConfigurationResolver`, covering methodology, tool selection, and injection enablement.
- Added nullable task-level BMAD injection override plus migration support so individual tasks can force BMAD setup on or off without changing the project default.
- Updated task detail UI to clearly show inherited versus overridden BMAD settings, including separate source indicators for methodology, tools, and injection behavior.
- Updated workspace generation and IDE launch flows to consume the resolved task BMAD configuration rather than ad hoc field checks.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/BmadConfiguration.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Task.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/BmadConfigurationResolver.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/TaskEntity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/TaskRepositoryImpl.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/CreateTaskUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/UpdateTaskUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/GenerateWorkspaceUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/LaunchIDEUseCase.kt`
- `taskmanager/core/src/main/resources/db/migration/V13__add_task_bmad_injection_override.sql`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/service/BmadConfigurationResolverTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/GenerateWorkspaceUseCaseTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/LaunchIDEUseCaseTest.kt`

### Change Log

- Added explicit task-level BMAD override resolution and injection override support.

## QA Results

### Review Date: 2026-03-23

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation is well-structured. `BmadConfigurationResolver` centralizes resolution of methodology, toolIds, and injectionEnabled from project + task overrides. `BmadConfiguration` includes methodologyOverridden, toolsOverridden, injectionOverridden for UI/audit. Task has `bmadInjectionEnabledOverride` (Boolean?) with migration V13. GenerateWorkspaceUseCase and LaunchIDEUseCase inject BmadConfigurationResolver and use resolved config. Task detail UI shows Methodology Source, BMAD Tools Source, Injection Source with Task Override vs Project Default.

### Refactoring Performed

None. Implementation follows existing patterns.

### Compliance Check

- Coding Standards: ✓ Kotlin conventions followed
- Project Structure: ✓ BmadConfigurationResolver in domain/service; BmadConfiguration in domain/model
- Testing Strategy: ✓ BmadConfigurationResolverTest (inherit, apply overrides); GenerateWorkspaceUseCaseTest (skip injection when override=false); LaunchIDEUseCase uses resolver
- All ACs Met: ✓

### AC Traceability

| AC | Description | Evidence |
|----|-------------|----------|
| 1 | Project-level config with task override | Project methodology, bmadToolIds; Task methodologyOverride, bmadToolOverrideIds, bmadInjectionEnabledOverride; BmadConfigurationResolver.resolve() |
| 2 | Overrides indicated in UI, take precedence | DetailRow "Methodology Source", "BMAD Tools Source", "Injection Source" with "Task Override" when set; effective* methods resolve precedence |
| 3 | No override → project config applies | BmadConfigurationResolver uses task.effectiveMethodology, effectiveBmadToolIds, bmadInjectionEnabledOverride ?: (methodology==BMAD) |
| 4 | Override changes isolated | Task updates affect only that task; project default unchanged; UpdateTaskUseCase per-task |
| 5 | Override state visible in project and task views | Project detail: methodology, BMAD tools; Task detail: Methodology Source, BMAD Tools Source, Injection Source, effective values |

### Test Coverage

- BmadConfigurationResolverTest: inherit project defaults; apply task overrides (methodology, tools, injection)
- GenerateWorkspaceUseCaseTest: skip BMAD injection when bmadInjectionEnabledOverride=false

### Gate Status

Gate: PASS → docs/qa/gates/6.4-bmad-task-level-override.yml

### Recommended Status

✓ Ready for Done
