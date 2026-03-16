# Story 6.1: BMAD as Selectable Methodology

**Epic:** Epic 6 - BMAD Methodology Integration

**As a** developer using the BMAD method,  
**I want** to select BMAD as the methodology for a project or task,  
**so that** AiTask can configure my workspace and tools accordingly.

## Status

Draft

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

## Tasks / Subtasks

- [ ] Task 1: Add methodology field to project and task models
  - [ ] Migration for methodology column (project, task)
  - [ ] Methodology enum: NONE, BMAD, CUSTOM
- [ ] Task 2: Add methodology selection UI to project and task configuration
  - [ ] Dropdown in project detail
  - [ ] Optional override in task detail
- [ ] Task 3: Persistence and validation
  - [ ] Confirm dialog when changing methodology and rule sets exist
- [ ] Task 4: Unit tests for model and configuration logic

## Dev Notes

- Methodology drives downstream behavior (workspace injection, tool pre-selection) in later stories
- BMAD-2 injection and BMAD-3 tools are in stories 6.2 and 6.3
