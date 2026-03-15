# Story 1.5: Launch Task Workspace in Configured IDE

## Status

Done

**Epic:** Epic 1 - Foundation and First Task Launch Flow

**As a** developer,  
**I want** to open a prepared task workspace in my configured IDE,  
**so that** I can move directly from task selection into coding.

## Acceptance Criteria

1. A user can launch the configured IDE from the selected task after workspace preparation completes successfully.
2. The application only presents IDE options configured for the project's repository.
3. The application opens the prepared workspace path in the selected IDE.
4. The task launch flow updates the task status to In Progress when the workspace is opened or when the user explicitly starts the task, based on the final UX decision.
5. The application records a task-launch activity entry for successful workspace opening.

## Architecture References

- [Component Architecture: IDE Integration](../architecture.md#4-ide-integration-component)
- [Component Architecture: Workspace Management](../architecture.md#6-workspace-management-component)

## UX References

- [Flow 1: Task Launch (Primary MVP Flow)](../front-end-spec.md#flow-1-task-launch-primary-mvp-flow)
- [Task Launch Flow (Stepper)](../front-end-spec.md#3-task-launch-flow-modal-or-stepper)
- [Visual Mockup: Task Launch Flow](../mockups/task-launch.html)

## QA Results

### Review Date: 2026-03-15

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Full implementation complete. TaskDetailView includes IDE launch buttons filtered by preferredIDEs, wired to LaunchIDEUseCase via TasksViewModel. DependencyContainer wires LaunchIDEUseCase, IDEService, and ActivityRepository. UI filters available IDEs by repository.preferredIDEs. All acceptance criteria met.

### Compliance Check

- Coding Standards: ✓ Kotlin conventions followed
- Project Structure: ✓ Aligns with taskmanager/desktop-app structure
- Testing Strategy: ✓ 14 tests (LaunchIDEUseCaseTest 6, DesktopIDEServiceTest 8)
- All ACs Met: ✓ AC1–AC5 implemented

### Gate Status

Gate: PASS → docs/qa/gates/1.5-ide-launch.yml

### Recommended Status

✓ Ready for Done

## QA Results

### Review Date: 2026-03-15

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Full implementation complete. TaskDetailView includes IDE launch buttons filtered by preferredIDEs. LaunchIDEUseCase, IDEService, and ActivityRepository wired in DependencyContainer. TasksViewModel handles IDE launch with success/failure feedback. 14 tests (LaunchIDEUseCaseTest 6, DesktopIDEServiceTest 8). UI filters available IDEs by preferredIDEs list per AC2.

### Compliance Check

- **Coding Standards**: ✓ Kotlin conventions followed.
- **Project Structure**: ✓ Aligns with architecture.
- **Testing Strategy**: ✓ 14 tests for LaunchIDEUseCase and DesktopIDEService.
- **All ACs Met**: ✓ AC1–AC5 fully implemented.

### Gate Status

Gate: PASS → docs/qa/gates/1.5-ide-launch.yml

### Recommended Status

✓ Ready for Done
