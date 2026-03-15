# Story 1.3: Task Management for the Core Workflow

## Status

Done

**Epic:** Epic 1 - Foundation and First Task Launch Flow

**As a** developer,  
**I want** to create and manage tasks within a project,  
**so that** I can track work items that will drive workspace generation and IDE launch.

## Acceptance Criteria

1. A user can create, view, edit, archive, and delete tasks within an existing project.
2. A task records at minimum a title, description, task type, and status.
3. Supported task types include Feature, Bug Fix, Research, Enhancement, Documentation, and Refactoring.
4. Supported task statuses include Pending, In Progress, Completed, and Archived.
5. Task lists can be filtered by project and status.

## Architecture References

- [Component Architecture: Task Management](../architecture.md#2-task-management-component)
- [Database Schema: tasks](../architecture.md#database-schema)

## UX References

- [Flow 2: Create and Start New Task](../front-end-spec.md#flow-2-create-and-start-new-task)
- [Screen Layouts: Task Launch Flow](../front-end-spec.md#key-screen-layouts)
- [Visual Mockup: Tasks List / Detail](../mockups/tasks.html)

## QA Results

### Review Date: 2025-03-12

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Backend domain, use cases, repository, validator, and database schema are implemented for task CRUD. Task model supports title, description, task type, status; TaskType and TaskStatus enums match AC3/AC4; GetTasksUseCase supports filtering by project and status. However, no desktop UI exists—TasksView remains a placeholder ("Coming Soon"). Users cannot create, view, edit, archive, or delete tasks through the application.

### Refactoring Performed

None. Critical gaps must be addressed before refactoring.

### Compliance Check

- Coding Standards: N/A (insufficient UI implementation to assess)
- Project Structure: Partial – core layer exists, UI layer missing
- Testing Strategy: Partial – TaskValidator, CreateTaskUseCase, GetTasksUseCase have tests; UpdateTaskUseCase and DeleteTaskUseCase lack unit tests
- All ACs Met: FAIL – AC1–AC5 require user-facing functionality that is not delivered

### Improvements Checklist

- [ ] Implement TasksView with task list, create/edit forms, archive and delete actions
- [ ] Add project selector and status filter for task list (AC5)
- [ ] Wire CreateTaskUseCase, UpdateTaskUseCase, DeleteTaskUseCase, GetTasksUseCase to desktop app
- [ ] Surface validation errors from TaskValidator in UI with actionable feedback
- [ ] Add unit tests for UpdateTaskUseCase and DeleteTaskUseCase
- [ ] Add integration tests for TaskRepositoryImpl where feasible

### Security Review

No security issues identified. TaskValidator enforces basic input constraints. No sensitive data in task model.

### Performance Considerations

TaskRepositoryImpl uses Exposed transactions; acceptable for desktop scale. Indexes on project_id, status, and (project_id, status) support filtering. No obvious bottlenecks.

### Gate Status

Gate: FAIL → docs/qa/gates/1.3-task-management.yml

### Recommended Status

**Changes Required – Return to In Progress.** Story is not complete. Backend foundation exists but user-facing functionality (AC1–AC5) is not delivered.

---

### Re-Review Date: 2026-03-12

### Reviewed By: Quinn (Test Architect)

### Re-Review Findings

**Test Coverage – Improved:** UpdateTaskUseCaseTest (5 tests) and DeleteTaskUseCaseTest (4 tests) confirmed. Total task-related tests: 25 (TaskValidatorTest 8, CreateTaskUseCaseTest 4, UpdateTaskUseCaseTest 5, DeleteTaskUseCaseTest 4, GetTasksUseCaseTest 4). All use cases now have unit test coverage.

**UI Status – Unchanged:** TasksView remains a placeholder ("Coming Soon"). No task list, create/edit forms, archive, delete, or filter UI. Use cases not wired to desktop app.

**Gate:** FAIL. Backend complete with comprehensive tests; AC1–AC5 require user-facing functionality that is not implemented.

---

### Re-Review Date: 2026-03-15

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Full implementation complete following Gradle migration. TasksView provides master-detail layout with task list, create dialog, status management, and delete functionality. TaskFilters component implements project and status dropdowns (AC5). TaskDetailView integrated with Generate Workspace and Launch IDE actions. DependencyContainer wires all task use cases. TasksViewModel connects UI to use cases. 25 tests passing across validators and use cases.

### Refactoring Performed

None required. Implementation is complete.

### Compliance Check

- **Coding Standards**: ✓ Kotlin conventions followed.
- **Project Structure**: ✓ Files under `taskmanager/desktop-app/` and `taskmanager/core/` align with architecture.
- **Testing Strategy**: ✓ 25 tests (TaskValidator, CreateTask, UpdateTask, DeleteTask, GetTasks use cases).
- **All ACs Met**: ✓ AC1–AC5 fully implemented.

### Gate Status

Gate: PASS → docs/qa/gates/1.3-task-management.yml

### Recommended Status

✓ Ready for Done
