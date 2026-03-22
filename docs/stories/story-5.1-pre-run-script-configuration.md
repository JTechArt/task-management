# Story 5.1: Pre-Run Script Configuration and Execution

**Epic:** Epic 5 - Pre-Run Scripts and Environment Validation

**As a** developer,  
**I want** to configure project- or task-level pre-run scripts,  
**so that** my environment is validated before the IDE launches and I avoid starting work in an incorrect setup.

## Status

Done

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

- [x] Task 1: Add pre-run script data model and migration
  - [x] Create PreRunScript entity and repository
  - [x] Add migration for pre_run_scripts table (project_id, repository_id nullable, script_path/inline, execution_order)
- [x] Task 2: Implement PreRunScriptService for script execution
  - [x] Execute scripts in configured order before IDE launch
  - [x] Capture stdout/stderr and exit codes
  - [x] Integrate into IDE launch flow (before LaunchIDEUseCase)
- [x] Task 3: Add environment check support
  - [x] Support Node.js, Java, Python version checks
  - [x] Support required environment variable checks
- [x] Task 4: Add pre-run script configuration UI and failure handling
  - [x] Project settings: add pre-run scripts section
  - [x] Display clear error messages on failure
  - [x] Record PRE_RUN_SUCCESS, PRE_RUN_FAILED in activity history
- [x] Task 5: Add unit tests for PreRunScriptService and configuration validation

## Dev Notes

- Scripts execute in workspace root or project path
- Support shell scripts (.sh, .bash) and optionally .bat for Windows
- Consider security: no arbitrary command injection; validate script paths
- Activity types: add PRE_RUN_SUCCESS, PRE_RUN_FAILED

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew :desktop-app:compileKotlin`
- `./gradlew :core:test --tests 'com.aitask.core.domain.usecase.LaunchIDEUseCaseTest' --tests 'com.aitask.core.infrastructure.prerun.LocalPreRunScriptServiceTest'`
- `./gradlew test`

### Completion Notes List

- Added a persisted pre-run script model, repository, and Flyway migration for project- and repository-scoped checks.
- Implemented local pre-run execution with inline commands, script paths, runtime version checks, and required environment variable checks.
- Wired pre-run execution into `LaunchIDEUseCase` so failures block IDE launch and emit `PRE_RUN_SUCCESS` or `PRE_RUN_FAILED` activity records.
- Added a project-detail pre-run configuration tab with add, edit, and delete flows for checks.
- Added unit coverage for launch orchestration and the local pre-run executor.

### File List

- `docs/stories/story-5.1-pre-run-script-configuration.md`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/PreRunScriptEntity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/PreRunScriptRepositoryImpl.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Activity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/PreRunScript.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/repository/PreRunScriptRepository.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/PreRunScriptService.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/LaunchIDEUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/prerun/LocalPreRunScriptService.kt`
- `taskmanager/core/src/main/resources/db/migration/V9__add_pre_run_scripts.sql`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/LaunchIDEUseCaseTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/prerun/LocalPreRunScriptServiceTest.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectComponents.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectsView.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ProjectsViewModel.kt`

### Change Log

- 2026-03-22: Added persisted pre-run script configuration, launch-time execution, activity logging, project UI management, and automated tests for story 5.1.

## QA Results

### Review Date: 2026-03-22

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation meets all acceptance criteria. Pre-run scripts are integrated into `LaunchIDEUseCase` with proper ordering, activity recording (PRE_RUN_SUCCESS, PRE_RUN_FAILED), and failure handling that blocks IDE launch. Configuration UI in ProjectComponents (Pre-Run tab, PreRunScriptDialog, PreRunScriptCard) supports add/edit/delete. LocalPreRunScriptService covers INLINE_COMMAND, SCRIPT_PATH, NODE_VERSION, JAVA_VERSION, PYTHON_VERSION, and ENVIRONMENT_VARIABLE types.

### Refactoring Performed

- **File**: `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/prerun/LocalPreRunScriptService.kt`
  - **Change**: Added `isPathWithinWorkspace()` validation for SCRIPT_PATH to prevent path traversal.
  - **Why**: User-supplied script paths could escape the workspace (e.g. `../../etc/passwd`). Security rule: never use raw user input for file paths without containment check.
  - **How**: Resolve canonical paths and ensure script path is within workspace directory; reject otherwise.
- **File**: `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/prerun/LocalPreRunScriptServiceTest.kt`
  - **Change**: Added test `should reject script path outside workspace` to verify path traversal prevention.
  - **Why**: Regression protection for the security fix.
  - **How**: Assert that a script path pointing outside workspace fails with "within workspace" message.

### Compliance Check

- Coding Standards: ✓ Kotlin conventions followed; functions are focused.
- Project Structure: ✓ Entities, repositories, services, use cases align with existing patterns.
- Testing Strategy: ✓ Unit tests for LaunchIDEUseCase and LocalPreRunScriptService; Arrange-Act-Assert used.
- All ACs Met: ✓ AC1–AC5 covered by implementation and tests.

### Improvements Checklist

- [x] Refactored path traversal vulnerability in LocalPreRunScriptService (SCRIPT_PATH)
- [x] Added path-traversal regression test
- [ ] Consider extracting validation logic for requiredValue (version/env identifiers) to reduce shell interpolation risk
- [ ] Add integration test for full IDE launch flow with pre-run scripts
- [ ] Add unit tests for NODE_VERSION, JAVA_VERSION, PYTHON_VERSION execution (optional; current coverage via LaunchIDEUseCase mocks)

### Security Review

- **Path traversal (SCRIPT_PATH)**: Fixed. Script paths (relative or absolute) must resolve within workspace.
- **INLINE_COMMAND**: Inherently executes user-supplied commands; by design per story. Document as accepted risk.
- **requiredValue**: Interpolated into grep/shell; grep -F used for most checks. Low risk for typical inputs; consider allowlist in future.

### Performance Considerations

Pre-run scripts execute sequentially before IDE launch. This is intentional (by design); no concerns.

### Files Modified During Review

- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/prerun/LocalPreRunScriptService.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/prerun/LocalPreRunScriptServiceTest.kt`

### Gate Status

Gate: PASS → docs/qa/gates/5.1-pre-run-script-configuration.yml

### Recommended Status

✓ Ready for Done
