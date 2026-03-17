# Story 5.1: Pre-Run Script Configuration and Execution

**Epic:** Epic 5 - Pre-Run Scripts and Environment Validation

**As a** developer,  
**I want** to configure project- or task-level pre-run scripts,  
**so that** my environment is validated before the IDE launches and I avoid starting work in an incorrect setup.

## Status

Draft

## Acceptance Criteria

1. A user can define one or more pre-run scripts per project or repository.
2. When "Open in IDE" is triggered, pre-run scripts execute first; on success, the IDE launches; on failure, the user sees a clear error and the IDE does not launch.
3. Pre-run scripts support checking Node.js, Java, and Python versions and required environment variables.
4. Pre-run script failures block IDE launch and surface clear, actionable error messages to the user.
5. The application records pre-run script execution outcomes in task activity history.

## Requirements Mapping

- PRE-1: Project- or task-level pre-run script configuration
- PRE-2: Pre-run scripts execute before IDE launch
- PRE-3: Environment checks (Node, Java, Python, env vars)
- PRE-6: Failures block IDE launch with clear error messages

## Architecture References

- [Component Architecture: IDE Integration](../architecture.md#4-ide-integration-component)
- [Component Architecture: Workspace Management](../architecture.md#6-workspace-management-component)

## UX References

- [Front-end Spec: Preflight Scripts & Environment Validation](../front-end-spec.md#11-preflight-scripts--environment-validation)
- [Mockup: Preflight](../mockups/preflight.html)
- [Mockup: Task Launch Flow](../mockups/task-launch.html)

## Tasks / Subtasks

- [ ] Task 1: Add pre-run script data model and migration
  - [ ] Create PreRunScript entity and repository
  - [ ] Add migration for pre_run_scripts table (project_id, repository_id nullable, script_path/inline, execution_order)
- [ ] Task 2: Implement PreRunScriptService for script execution
  - [ ] Execute scripts in configured order before IDE launch
  - [ ] Capture stdout/stderr and exit codes
  - [ ] Integrate into IDE launch flow (before LaunchIDEUseCase)
- [ ] Task 3: Add environment check support
  - [ ] Support Node.js, Java, Python version checks
  - [ ] Support required environment variable checks
- [ ] Task 4: Add pre-run script configuration UI and failure handling
  - [ ] Project settings: add pre-run scripts section
  - [ ] Display clear error messages on failure
  - [ ] Record PRE_RUN_SUCCESS, PRE_RUN_FAILED in activity history
- [ ] Task 5: Add unit tests for PreRunScriptService and configuration validation

## Dev Notes

- Scripts execute in workspace root or project path
- Support shell scripts (.sh, .bash) and optionally .bat for Windows
- Consider security: no arbitrary command injection; validate script paths
- Activity types: add PRE_RUN_SUCCESS, PRE_RUN_FAILED
