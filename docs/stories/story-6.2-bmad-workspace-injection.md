# Story 6.2: BMAD Setup Injection into Workspace

**Epic:** Epic 6 - BMAD Methodology Integration

**As a** developer,  
**I want** BMAD setup files to be automatically injected into the task workspace when BMAD is selected,  
**so that** I have the correct BMAD structure (.bmad-core, AGENTS.md) without manual copying.

## Status

Done

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

## UX References

- [Front-end Spec: Methodology & BMAD Configuration](../front-end-spec.md#12-methodology--bmad-configuration)
- [Mockup: Methodology](../mockups/methodology.html)
- [Mockup: Task Launch Flow](../mockups/task-launch.html)

## Tasks / Subtasks

- [x] Task 1: Define BMAD bundle source (local path or bundled)
  - [x] Configuration for BMAD bundle location
  - [x] Copy or link strategy for .bmad-core, AGENTS.md
- [x] Task 2: Integrate BMAD injection into workspace generation flow
  - [x] Run after repository retrieval, before IDE launch
  - [x] Handle overwrite vs skip when content exists
- [x] Task 3: Error handling and activity recording
  - [x] Clear error messages on injection failure
  - [x] Activity record: BMAD_INJECTION_APPLIED or BMAD_INJECTION_FAILED
- [x] Task 4: Unit tests for injection logic

## Dev Notes

- BMAD setup typically includes `.bmad-core/` with config, tasks, templates, checklists
- Consider version compatibility with different BMAD releases

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `./gradlew test`

### Completion Notes List

- Added a dedicated BMAD workspace injection service with filesystem-based bundle copying, explicit skip-existing behavior, and bundle path configuration via `-Daitask.bmad.bundlePath` plus optional `-Daitask.bmad.overwriteExisting=true`.
- Integrated BMAD injection into `GenerateWorkspaceUseCase` after repository preparation and before rule application, using effective project/task methodology from story 6.1.
- Added success/failure activity types for BMAD injection and clear progress/failure reporting so injection issues do not silently skip.
- Added unit tests for both the new filesystem injector and the workspace-generation flow covering success and failure cases.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/BmadWorkspaceInjectionService.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/workspace/FileSystemBmadWorkspaceInjectionService.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Activity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/GenerateWorkspaceUseCase.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/GenerateWorkspaceUseCaseTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/workspace/FileSystemBmadWorkspaceInjectionServiceTest.kt`

### Change Log

- Added BMAD workspace bundle injection with activity logging and workspace-generation integration.

## QA Results

### Review Date: 2026-03-23

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation is well-structured. `BmadWorkspaceInjectionService` interface with `BmadInjectionResult` provides clear success/failure semantics. `FileSystemBmadWorkspaceInjectionService` uses bundle discovery via `-Daitask.bmad.bundlePath` or upward search from working directory for AGENTS.md and .bmad-core. Skip-existing behavior is explicit (overwrite controlled by `-Daitask.bmad.overwriteExisting`). Integration into `GenerateWorkspaceUseCase` runs after repository preparation and before rule application, gated by `task.effectiveMethodology(project.methodology) == Methodology.BMAD`.

### Refactoring Performed

None. Implementation follows existing patterns and AC requirements.

### Compliance Check

- Coding Standards: ✓ Kotlin conventions followed
- Project Structure: ✓ Domain service in core, infrastructure impl in workspace package
- Testing Strategy: ✓ Unit tests for injector (copy, skip-existing, missing bundle) and use case (BMAD injection success, failure)
- All ACs Met: ✓

### AC Traceability

| AC | Description | Evidence |
|----|-------------|----------|
| 1 | Inject BMAD setup when BMAD selected | GenerateWorkspaceUseCase line 124: `task.effectiveMethodology(project.methodology) == Methodology.BMAD`; injects .bmad-core, AGENTS.md into workspace path |
| 2 | Expected locations, no overwrite unless configured | FileSystemBmadWorkspaceInjectionService copies to workspace/.bmad-core, workspace/AGENTS.md; overwriteExisting from `-Daitask.bmad.overwriteExisting` |
| 3 | Part of workspace prep before IDE launch | Runs after prepareWorkspace (clone), before applyRulesToWorkspaceUseCase; workspace generation precedes IDE launch |
| 4 | Failures reported clearly, no silent skip | Result.failure; BMAD_INJECTION_FAILED activity; onProgress("BMAD injection failed: ..."); returns Result.failure (no silent continue) |
| 5 | Activity records injection applied | BMAD_INJECTION_APPLIED (success) and BMAD_INJECTION_FAILED; metadata includes copiedPaths, skippedPaths, sourcePath |

### Test Coverage

- FileSystemBmadWorkspaceInjectionServiceTest: copy bundle, skip existing (overwrite=false), missing bundle
- GenerateWorkspaceUseCaseTest: BMAD injection when methodology=BMAD (order verify, activity BMAD_INJECTION_APPLIED); BMAD injection failure (Result.failure, no rules applied, BMAD_INJECTION_FAILED activity)

### Gate Status

Gate: PASS → docs/qa/gates/6.2-bmad-workspace-injection.yml

### Recommended Status

✓ Ready for Done
