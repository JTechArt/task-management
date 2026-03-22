# Story 6.3: BMAD Recommended Tools Pre-Selection

**Epic:** Epic 6 - BMAD Methodology Integration

**As a** developer,  
**I want** BMAD-recommended tools (agents, workflows, tasks, checklists) to be pre-selected when BMAD is chosen,  
**so that** I can use the full BMAD workflow without manually enabling each tool.

## Status

Draft

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

- [ ] Task 1: Define BMAD default tool set (agents, checklists, tasks)
  - [ ] Reference BMAD-Method documentation for default list
  - [ ] Store as configurable defaults
- [ ] Task 2: Implement pre-selection logic when methodology is BMAD
  - [ ] Apply defaults to project or task on BMAD selection
  - [ ] Allow enable/disable per tool
- [ ] Task 3: Add BMAD tool configuration UI
  - [ ] List agents, checklists, tasks with checkboxes
  - [ ] Persist selections
- [ ] Task 4: Wire tool selection to task execution and agent invocation
- [ ] Task 5: Unit tests for pre-selection and persistence

## Dev Notes

- Integrate with existing rule management where applicable
- Tool list may be derived from .bmad-core structure when BMAD is injected
