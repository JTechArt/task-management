# Story 3.4: Workspace Retention and Cleanup Controls

## Status

Done

**Epic:** Epic 3 - Visibility and Operational Control

**As a** developer,  
**I want** control over what happens to task workspaces after work changes state,  
**so that** I can balance disk usage with safety for unfinished or reference work.

## Acceptance Criteria

1. Users can configure workspace behavior for completed and deleted tasks, including retain, archive, or clean up options.
2. The application warns users before destructive cleanup actions that remove local workspace data.
3. Completed tasks clearly show the current workspace state or retention outcome.
4. Cleanup or archival actions are recorded in activity history.
5. Failed cleanup actions do not remove task metadata and provide actionable feedback.

## Architecture References

- [Component Architecture: Workspace Management – RetentionPolicy](../architecture.md#6-workspace-management-component)

## UX References

- [Confirmation Dialog component (destructive actions)](../front-end-spec.md#core-components)
- [Visual Mockup: Settings / Backup / Destructive Confirmation](../mockups/settings.html)

## Dev Agent Record

### Tasks / Subtasks

- [x] Add retention_policy to projects (migration V6, Project entity, model, repository)
- [x] Add workspace_cleaned_at to tasks (migration, Task entity, model, TaskRepositoryImpl)
- [x] Add WORKSPACE_CLEANUP ActivityType and CleanupWorkspaceUseCase
- [x] Update CreateProjectUseCase and UpdateProjectUseCase to use retentionPolicy
- [x] Add retention policy dropdown to CreateProjectDialog
- [x] Add DestructiveCleanupConfirmationDialog (Type DELETE to confirm)
- [x] Add workspace state display and "Clean up workspace" button for completed tasks in TaskDetailView
- [x] Wire CleanupWorkspaceUseCase in TasksViewModel and DependencyContainer
- [x] Add CleanupWorkspaceUseCaseTest (6 tests)

### File List

- taskmanager/core/src/main/resources/db/migration/V6__add_workspace_retention.sql (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/ProjectEntity.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/TaskEntity.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/ProjectRepositoryImpl.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/TaskRepositoryImpl.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Project.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Task.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Activity.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/CleanupWorkspaceUseCase.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/CreateProjectUseCase.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/UpdateProjectUseCase.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/CreateProjectDialog.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ProjectsViewModel.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectsView.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt (modified)
- taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/CleanupWorkspaceUseCaseTest.kt (new)

### Change Log

| Date | Description |
|------|-------------|
| 2026-03-16 | Implemented Story 3.4: retention policy (project config), workspace_cleaned_at tracking, CleanupWorkspaceUseCase, destructive confirmation (Type DELETE), workspace state display on completed tasks, activity recording, actionable failure feedback |

### Completion Notes

- AC1: Project retention policy configurable via CreateProjectDialog (KEEP_ALL, DELETE_ON_COMPLETION, DELETE_AFTER_DAYS)
- AC2: DestructiveCleanupConfirmationDialog requires typing "DELETE" to confirm before cleanup
- AC3: Completed tasks show "Workspace state" (Retained / Cleanup completed / Not created)
- AC4: WORKSPACE_CLEANUP activity recorded on success and failure
- AC5: On cleanup failure, task metadata unchanged; activity records FAILED status; actionable message built from error (permission, locked, not found, etc.)

## Implementation Notes

**Completed (Story 3.4):**

- **AC1**: Retention policy (KEEP_ALL, DELETE_ON_COMPLETION, DELETE_AFTER_DAYS) configurable per project. CreateProjectDialog includes retention dropdown. Project model, migration V6, and repository persist retentionPolicy.
- **AC2**: Destructive confirmation dialog requires typing "DELETE" before cleanup. DestructiveCleanupConfirmationDialog with typed confirmation. Clean up workspace button shown only for completed tasks with workspace.
- **AC3**: Completed tasks show workspace state (Retained / Cleaned up / Not created) in TaskDetailView. Workspace Cleanup section with state and Clean up workspace button.
- **AC4**: CleanupWorkspaceUseCase records WORKSPACE_CLEANUP activity (SUCCESS/FAILED). ActivityType.WORKSPACE_CLEANUP added.
- **AC5**: On cleanup failure, task metadata unchanged; activity recorded with FAILED; actionable feedback via buildActionableCleanupFailureMessage.

**Files touched:**
- taskmanager/core: V6 migration, Project/Task models, Projects/Tasks entities, ProjectRepositoryImpl, TaskRepositoryImpl, ActivityType, CleanupWorkspaceUseCase, WorkspaceService/FileSystemWorkspaceService (existing)
- taskmanager/desktop-app: DependencyContainer, CreateProjectDialog (retention), ProjectsViewModel, ProjectsView, TasksViewModel, TaskDetailView, TaskComponents (DestructiveCleanupConfirmationDialog), TasksView
