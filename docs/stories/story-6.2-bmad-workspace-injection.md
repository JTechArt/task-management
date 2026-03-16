# Story 6.2: BMAD Setup Injection into Workspace

**Epic:** Epic 6 - BMAD Methodology Integration

**As a** developer,  
**I want** BMAD setup files to be automatically injected into the task workspace when BMAD is selected,  
**so that** I have the correct BMAD structure (.bmad-core, AGENTS.md) without manual copying.

## Status

Draft

## Acceptance Criteria

1. Upon selecting BMAD, the system injects or links BMAD setup files (e.g., `.bmad-core`, `AGENTS.md`) into the task workspace during workspace generation.
2. Injected files are placed in the expected locations and do not overwrite existing BMAD content unless configured to do so.
3. The injection occurs as part of workspace preparation before IDE launch.
4. Injection failures are reported clearly and do not silently skip.
5. Workspace generation activity records whether BMAD injection was applied.

## Requirements Mapping

- BMAD-2: BMAD setup injection into task workspace

## Dependencies

- Story 6.1: BMAD as Selectable Methodology
- Story 1.4: Workspace Generation

## Architecture References

- [Component Architecture: Workspace Management](../architecture.md#6-workspace-management-component)

## Tasks / Subtasks

- [ ] Task 1: Define BMAD bundle source (local path or bundled)
  - [ ] Configuration for BMAD bundle location
  - [ ] Copy or link strategy for .bmad-core, AGENTS.md
- [ ] Task 2: Integrate BMAD injection into workspace generation flow
  - [ ] Run after repository retrieval, before IDE launch
  - [ ] Handle overwrite vs skip when content exists
- [ ] Task 3: Error handling and activity recording
  - [ ] Clear error messages on injection failure
  - [ ] Activity record: BMAD_INJECTION_APPLIED or BMAD_INJECTION_FAILED
- [ ] Task 4: Unit tests for injection logic

## Dev Notes

- BMAD setup typically includes `.bmad-core/` with config, tasks, templates, checklists
- Consider version compatibility with different BMAD releases
