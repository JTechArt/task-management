# Story 5.2: Per-Module Pre-Run Script Configuration

**Epic:** Epic 5 - Pre-Run Scripts and Environment Validation

**As a** developer working on monorepos,  
**I want** to configure pre-run scripts per repository or project module,  
**so that** different parts of my codebase can have different environment requirements.

## Status

Done

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

## UX References

- [Front-end Spec: Preflight Scripts & Environment Validation](../front-end-spec.md#11-preflight-scripts--environment-validation)
- [Mockup: Preflight](../mockups/preflight.html)
- [Mockup: Project Detail](../mockups/project-detail.html)

## Dependencies

- Story 5.1: Pre-Run Script Configuration and Execution

## Tasks / Subtasks

- [x] Task 1: Extend PreRunScript model for repository-level association
  - [x] Add repository_id to pre_run_scripts (nullable for project-level)
  - [x] Migration for schema change
- [x] Task 2: Implement per-repository script resolution and execution
  - [x] Resolve scripts per selected repository in workspace
  - [x] Define execution order: project scripts first, then per-repository
- [x] Task 3: Update configuration UI
  - [x] Add repository-level pre-run section in project detail
  - [x] Clearly distinguish project vs repository scripts in UI
- [x] Task 4: Document execution order and add tests

## Dev Notes

- Execution order: project-level scripts first (in execution_order), then per-repository scripts in repo order
- Multi-repo workspaces: execute scripts only for repositories included in the task workspace

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew :core:test --tests 'com.aitask.core.domain.usecase.LaunchIDEUseCaseTest' --tests 'com.aitask.core.infrastructure.prerun.LocalPreRunScriptServiceTest'`
- `./gradlew :desktop-app:compileKotlin`
- `./gradlew test`

### Completion Notes List

- Reused the repository-scoped `pre_run_scripts.repository_id` support introduced in story 5.1 and validated it as the persistence mechanism for per-module configuration.
- Updated `LaunchIDEUseCase` to read `workspace-metadata.json` and execute only project scripts plus repository scripts for repositories actually included in the task workspace.
- Preserved deterministic execution order by running project-level scripts first, then repository-level scripts in the workspace repository order captured during workspace generation.
- Updated the Project Detail Pre-Run tab to separate project-level and repository-level sections and to document the execution order directly in the UI.
- Added and updated tests to cover repository-aware launch ordering and keep the pre-run executor suite compiling cleanly.

### File List

- `docs/stories/story-5.2-per-module-pre-run-scripts.md`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Workspace.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/LaunchIDEUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/workspace/FileSystemWorkspaceService.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/LaunchIDEUseCaseTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/prerun/LocalPreRunScriptServiceTest.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectComponents.kt`

### Change Log

- 2026-03-22: Added repository-aware pre-run script resolution using workspace metadata, explicit project-vs-repository UI grouping, and ordering tests for story 5.2.

## QA Results

### Review Date: 2026-03-22

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation meets all acceptance criteria. LaunchIDEUseCase reads `workspace-metadata.json` to resolve selected repository IDs; executes project-level scripts first (repositoryId == null), then repository-level scripts in workspace repository order. UI groups scripts into "Project-Level Scripts" and "Repository-Level Scripts" with clear section headers. Execution order documented in UI: "project scripts first, then repository scripts in workspace repository order."

### Refactoring Performed

None. Implementation aligns with requirements and existing patterns.

### Compliance Check

- Coding Standards: ✓ Kotlin conventions followed.
- Project Structure: ✓ Consistent with 5.1; no new modules.
- Testing Strategy: ✓ LaunchIDEUseCaseTest verifies project-then-repository ordering and selected-repo filtering.
- All ACs Met: ✓ AC1–AC5 covered.

### Improvements Checklist

- [x] No blocking issues identified
- [ ] Consider integration test for full workspace → metadata → launch flow (optional)

### Security Review

No new security surface. workspace-metadata.json is written by FileSystemWorkspaceService during workspace preparation; LaunchIDEUseCase reads it for script resolution. No user-supplied JSON.

### Performance Considerations

Single metadata file read per launch; in-memory filtering. No concerns.

### Gate Status

Gate: PASS → docs/qa/gates/5.2-per-module-pre-run-scripts.yml

### Recommended Status

✓ Ready for Done
