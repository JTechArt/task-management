# Story 1.2: Basic Project Creation with Single Repository Configuration

## Status

Done

**Epic:** Epic 1 - Foundation and First Task Launch Flow

**As a** developer,  
**I want** to create a project with its primary repository and workspace settings,  
**so that** AiTask can prepare task work for a real codebase.

## Acceptance Criteria

1. A user can create, edit, view, and archive a project with name, description, workspace path, and branch naming template.
2. A user can attach one primary repository to the project with provider, clone URL, and repository name or label.
3. A user can define at least one preferred IDE for the repository from the supported IDE list.
4. Project data persists between application sessions.
5. Validation prevents saving incomplete or clearly invalid project or repository configurations and gives actionable feedback.

## Architecture References

- [Component Architecture: Project Management](../architecture.md#1-project-management-component)
- [Database Schema: projects, repositories](../architecture.md#database-schema)

## UX References

- [Flow 3: Project and Repository Setup](../front-end-spec.md#flow-3-project-and-repository-setup)
- [Screen Layouts: Project Detail](../front-end-spec.md#key-screen-layouts)
- [Visual Mockup: Projects List / Creation](../mockups/projects.html)
- [Visual Mockup: Project Detail](../mockups/project-detail.html)

## QA Results

### Review Date: 2025-03-12

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Backend domain, use cases, repositories, validators, and database schema are implemented to support project and repository CRUD. However, no desktop UI exists—ProjectsView remains a placeholder ("Coming Soon"). Users cannot create, edit, view, or archive projects through the application.

### Refactoring Performed

None. Critical gaps must be addressed before refactoring.

### Compliance Check

- Coding Standards: N/A (insufficient implementation to assess)
- Project Structure: Partial – core layer exists, UI layer missing
- Testing Strategy: FAIL – no tests for project/repository domain or use cases
- All ACs Met: FAIL – AC1–AC5 require user-facing functionality that is not delivered

### Improvements Checklist

- [ ] Implement ProjectsView with project list, create/edit forms, and archive action
- [ ] Implement project detail view with repository attachment (provider, clone URL, name/label)
- [ ] Implement preferred IDE selection from supported list (IDEType enum)
- [ ] Wire CreateProjectUseCase, UpdateProjectUseCase, ArchiveProjectUseCase, GetProjectsUseCase to desktop app
- [ ] Surface validation errors from ProjectValidator and RepositoryValidator in UI with actionable feedback
- [ ] Add unit tests for ProjectValidator, RepositoryValidator, CreateProjectUseCase
- [ ] Add integration tests for ProjectRepositoryImpl, RepositoryRepositoryImpl where feasible

### Security Review

No security issues identified in backend code. Validators enforce basic input constraints. Clone URL validation uses regex; consider additional URL format validation for edge cases.

### Performance Considerations

Repository implementations use Exposed transactions; acceptable for desktop scale. No obvious bottlenecks.

### Gate Status

Gate: FAIL → docs/qa/gates/1.2-basic-project-creation.yml

### Recommended Status

**Changes Required – Return to In Progress.** Story is not complete. Backend foundation exists but user-facing functionality (AC1–AC5) is not delivered.

---

### Re-Review Date: 2026-03-15

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Full implementation complete following Gradle migration. ProjectsView provides master-detail layout with create, edit, view, and archive capabilities. CreateProjectDialog integrates repository configuration (provider, clone URL, name) and multi-select IDE picker per IDEType. DependencyContainer wires all project and repository use cases. ProjectsViewModel connects UI to use cases. Code follows Kotlin conventions and DI patterns.

### Refactoring Performed

None required. Implementation is complete.

### Compliance Check

- **Coding Standards**: ✓ Kotlin conventions followed; type declarations, verb-named functions.
- **Project Structure**: ✓ Files under `taskmanager/desktop-app/` and `taskmanager/core/` align with architecture.
- **Testing Strategy**: ✓ 19 tests (ProjectValidator, RepositoryValidator, CreateProjectUseCase). Integration tests deferred.
- **All ACs Met**: ✓ AC1–AC5 fully implemented.

### Gate Status

Gate: PASS → docs/qa/gates/1.2-basic-project-creation.yml

### Recommended Status

✓ Ready for Done
