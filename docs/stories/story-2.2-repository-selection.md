# Story 2.2: Task Workspace Repository Selection

## Status

Done

**Epic:** Epic 2 - Multi-Repository Git Automation and Rule Application

**As a** developer,
**I want** to choose which repositories are included in a task workspace,
**so that** I can prepare only the codebases relevant to the current task.

## Acceptance Criteria

1. The task launch flow allows the user to select one, many, or all configured repositories for inclusion in the workspace.
2. The application remembers or suggests sensible default repository selections for repeated use where project configuration supports it.
3. The workspace preparation summary clearly shows which repositories will be retrieved before the user confirms launch.
4. The system creates a coherent workspace structure for the selected repositories.
5. If one selected repository fails during preparation, the application reports which repository failed and what remains usable.

## Architecture References

- [Component Architecture: Workspace Management](../architecture.md#6-workspace-management-component)
- [Workspace Structure and Metadata](../architecture.md#workspace-structure)

## UX References

- [Flow 1: Task Launch – Multi-repo selection](../front-end-spec.md#flow-1-task-launch-primary-mvp-flow)
- [Task Launch Flow (Stepper)](../front-end-spec.md#3-task-launch-flow-modal-or-stepper)
- [Visual Mockup: Task Launch Flow](../mockups/task-launch.html)

## QA Results

### Review Date: 2026-03-15

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Full implementation complete. RepositorySelectionDialog provides checkbox-based repository selection with smart defaults (primary repository or all repositories). Workspace Preparation Summary card shows selected repositories before confirmation. Progress tracking displays cloning status for each repository. Partial failure handling shows which repositories failed while keeping successful ones usable. TasksViewModel manages selection state and workspace generation with selected repositories.

### Refactoring Performed

None; code quality is excellent.

### Compliance Check

- Coding Standards: ✓ Kotlin conventions, Compose best practices
- Project Structure: ✓ Clean separation of UI and ViewModel logic
- Testing Strategy: ✓ 4 unit tests for repository selection logic
- All ACs Met: ✓ AC1-AC5 implemented

### Improvements Checklist

- [x] Create RepositorySelectionDialog component
- [x] Add smart default selection logic
- [x] Add workspace preparation summary
- [x] Add progress tracking during generation
- [x] Handle partial repository failures
- [x] Write unit tests for selection logic

### Security Review

No security concerns. Repository selection is UI-only with no credential handling.

### Performance Considerations

Efficient repository loading. No performance issues identified. Dialog handles large repository lists well.

### Gate Status

Gate: PASS_WITH_NOTES → docs/qa/gates/2.2-repository-selection.yml

### Issues Addressed

- ✅ REQ-002 (Medium): Enhanced partial failure reporting
  * Error messages now list failed repositories with error details
  * Error messages list successfully cloned repositories
  * Workspace path included for partial success scenarios
  * Both GenerateWorkspaceUseCase and FileSystemWorkspaceService updated
- ✓ REQ-001 (Low): Persist last selection - Deferred as nice-to-have
  * Smart defaults provide good UX
  * Can be added in future iteration based on user feedback

### Recommended Status

✓ Ready for Done – All acceptance criteria met. Repository selection UI complete with smart defaults, preparation summary, and enhanced error handling for partial failures.

---

### Review Date: 2025-03-15 (Re-review)

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation is solid. RepositorySelectionDialog provides checkbox selection; smart defaults (primary or all); Workspace Preparation Summary before launch; progress tracking during cloning. Backend (FileSystemWorkspaceService) tracks per-repo clone status and invokes progress with "${repository.name}: Clone failed" during run. However, when Result.failure is returned, the UI shows a generic "Some repositories failed to clone" and clears progress—the final error does not list which repository failed. The workspace with successful clones exists on disk but the task is not updated and the user is not told what remains usable.

### Refactoring Performed

None.

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ✓ 4 tests for default selection logic
- All ACs Met: ⚠ AC1, AC3, AC4 full; AC2 partial (no "remembers"); AC5 partial (no final report of which repo failed)

### Improvements Checklist

- [ ] Enhance error message on partial failure to list which repositories failed (AC5)
- [ ] Consider returning partial workspace to UI so user can see path and which repos succeeded (AC5 "what remains usable")
- [ ] AC2 "remembers" last selection: documented as nice-to-have for future iteration

### Gate Status

Gate: CONCERNS → docs/qa/gates/2.2-repository-selection.yml

### Recommended Status

✗ Changes Required – See top_issues in gate file. Priority: AC5 partial-failure reporting.
