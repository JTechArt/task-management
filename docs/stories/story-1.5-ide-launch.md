# Story 1.5: Launch Task Workspace in Configured IDE

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

