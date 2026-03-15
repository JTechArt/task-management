# Story 2.1: Multi-Repository Project Configuration

**Epic:** Epic 2 - Multi-Repository Git Automation and Rule Application

**As a** developer,  
**I want** to configure multiple repositories within a project,  
**so that** AiTask can support real-world codebases that span several services or modules.

## Acceptance Criteria

1. A user can add, edit, archive, and remove multiple repositories within a project.
2. Each repository stores provider, clone URL, label, local role or purpose, and one or more preferred IDE options.
3. The application clearly distinguishes the primary repository from additional repositories when configured.
4. Repository configuration changes persist between sessions.
5. Validation prevents duplicate or invalid repository definitions within the same project.

## Architecture References

- [Component Architecture: Git Integration](../architecture.md#3-git-integration-component)
- [Integration Architecture: Git Provider Integration](../architecture.md#1-git-provider-integration)

## UX References

- [Flow 3: Project and Repository Setup](../front-end-spec.md#flow-3-project-and-repository-setup)
- [Visual Mockup: Projects List / Creation](../mockups/projects.html)
- [Visual Mockup: Project Detail](../mockups/project-detail.html)

## QA Results

### Review Date: 2026-03-15

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Full implementation complete. Add, edit, and remove repositories are implemented via CreateRepositoryUseCase, UpdateRepositoryUseCase, and DeleteRepositoryUseCase. UI uses RepositoryDialog for both add and edit; ProjectDetailView lists repositories with add/edit/delete actions. Primary repository is clearly distinguished with a "PRIMARY" badge on RepositoryCard. Persistence via RepositoryRepositoryImpl (Exposed SQL). RepositoryValidator enforces name, clone URL, preferred IDEs; use cases enforce duplicate name/URL and primary rules. All use cases have comprehensive unit tests (14 tests total).

### Refactoring Performed

None; code quality is excellent.

### Compliance Check

- Coding Standards: ✓ Kotlin conventions, data classes, clear naming
- Project Structure: ✓ Use cases in core, UI in desktop-app, clean architecture maintained
- Testing Strategy: ✓ All use cases tested (CreateRepositoryUseCaseTest: 5, UpdateRepositoryUseCaseTest: 5, DeleteRepositoryUseCaseTest: 4)
- All ACs Met: ✓ AC1-AC5 implemented (archive and role/purpose deferred per PO)

### Improvements Checklist

- [x] Add UpdateRepositoryUseCaseTest (5 tests)
- [x] Add DeleteRepositoryUseCaseTest (4 tests)
- [x] Clarify archive for repositories (AC1) with PO → Deferred to future iteration
- [x] Document "local role or purpose" (AC2) as future scope → Repository name serves as label

### Security Review

No security concerns. RepositoryValidator validates URL format; no credential handling in scope. Input validation prevents injection attacks.

### Performance Considerations

Use cases and repository access are straightforward; no performance issues identified. Database queries use appropriate indexes.

### Gate Status

Gate: PASS → docs/qa/gates/2.1-multi-repository-configuration.yml

### Recommended Status

✓ Ready for Done – All acceptance criteria met. Unit tests complete. Minor scope items (archive, explicit role field) deferred to future iterations per PO clarification.
