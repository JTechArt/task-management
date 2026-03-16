# Story 6.4: BMAD Configuration Override at Task Level

**Epic:** Epic 6 - BMAD Methodology Integration

**As a** developer,  
**I want** to override BMAD configuration at the task level when needed,  
**so that** I can deviate from the project default for specific tasks without affecting other work.

## Status

Draft

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

## Tasks / Subtasks

- [ ] Task 1: Model task-level BMAD override (methodology, tools, injection options)
  - [ ] Nullable override fields on task
  - [ ] Migration if needed
- [ ] Task 2: Implement override resolution (task overrides project when set)
  - [ ] Use case or service to resolve effective BMAD config per task
- [ ] Task 3: Update task detail UI
  - [ ] Override indicators and controls
  - [ ] Clear visual distinction from project default
- [ ] Task 4: Ensure workspace generation and tool invocation use resolved config
- [ ] Task 5: Unit tests for override resolution

## Dev Notes

- Override applies to: methodology selection, tool enable/disable, possibly injection options
- Project default remains unchanged when task overrides; other tasks unaffected
