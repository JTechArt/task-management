# Story 5.2: Per-Module Pre-Run Script Configuration

**Epic:** Epic 5 - Pre-Run Scripts and Environment Validation

**As a** developer working on monorepos,  
**I want** to configure pre-run scripts per repository or project module,  
**so that** different parts of my codebase can have different environment requirements.

## Status

Draft

## Acceptance Criteria

1. A user can attach pre-run scripts to specific repositories within a multi-repository project.
2. When a task workspace includes multiple repositories, the system executes the relevant scripts per selected repository or module.
3. The configuration UI clearly distinguishes project-level vs repository-level pre-run scripts.
4. Script execution order is predictable and documented (e.g., project scripts first, then per-repository scripts).
5. Configuration persists correctly for both project and repository levels.

## Requirements Mapping

- PRE-4: Per-repository or project-module pre-run configuration

## Architecture References

- [Component Architecture: IDE Integration](../architecture.md#4-ide-integration-component)
- [Component Architecture: Workspace Management](../architecture.md#6-workspace-management-component)

## Dependencies

- Story 5.1: Pre-Run Script Configuration and Execution

## Tasks / Subtasks

- [ ] Task 1: Extend PreRunScript model for repository-level association
  - [ ] Add repository_id to pre_run_scripts (nullable for project-level)
  - [ ] Migration for schema change
- [ ] Task 2: Implement per-repository script resolution and execution
  - [ ] Resolve scripts per selected repository in workspace
  - [ ] Define execution order: project scripts first, then per-repository
- [ ] Task 3: Update configuration UI
  - [ ] Add repository-level pre-run section in project detail
  - [ ] Clearly distinguish project vs repository scripts in UI
- [ ] Task 4: Document execution order and add tests

## Dev Notes

- Execution order: project-level scripts first (in execution_order), then per-repository scripts in repo order
- Multi-repo workspaces: execute scripts only for repositories included in the task workspace
