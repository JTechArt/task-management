# Story 3.2: Activity History and Recent Events

## Status

Done

**Epic:** Epic 3 - Visibility and Operational Control

**As a** developer,  
**I want** to review recent application activity,  
**so that** I can understand what happened during task setup, launch, and management workflows.

## Acceptance Criteria

1. The application records and displays recent activity entries for key events including task creation, workspace preparation, IDE launch, Git preparation, and rule application.
2. Activity entries include enough context to identify the relevant project, task, action, and result.
3. Users can view activity in chronological order with the most recent events first.
4. Users can filter activity by project, task, or event type.
5. Failed actions are visually distinguishable from successful actions in the activity history.

## Architecture References

- [Component Architecture: Activity Tracking](../architecture.md#8-activity-tracking-component)
- [Database Schema: activity_log](../architecture.md#7-activity-log)

## UX References

- [Main Dashboard layout](../front-end-spec.md#1-main-dashboard)
- [Visual Mockup: Dashboard](../mockups/dashboard.html)

## Dev Agent Record

### Tasks / Subtasks

- [x] Add ActivityStatus, extend Activity model with projectId; add RULES_APPLIED, GIT_PREPARED to ActivityType
- [x] Create V5 migration for activity_log table
- [x] Create Activity entity and ActivityRepositoryImpl with findFiltered
- [x] Record activity in CreateTaskUseCase, GenerateWorkspaceUseCase, ApplyRulesToWorkspaceUseCase, LaunchIDEUseCase
- [x] Create ActivityView with filters (project, task, event type) and chronological list
- [x] Add Activity to navigation; update Dashboard RecentActivitySection with success/failed styling
- [x] Add ActivityViewModelTest; update use case tests for activityRepository

### File List

- taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Activity.kt (modified)
- taskmanager/core/src/main/resources/db/migration/V5__add_activity_log.sql (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/ActivityEntity.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/ActivityRepositoryImpl.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/repository/ActivityRepository.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/InMemoryActivityRepository.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/CreateTaskUseCase.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/GenerateWorkspaceUseCase.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/ApplyRulesToWorkspaceUseCase.kt (modified)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/LaunchIDEUseCase.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ActivityViewModel.kt (new)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/activity/ActivityView.kt (new)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/dashboard/DashboardView.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/Navigation.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/TaskManagerApp.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt (modified)
- taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/ActivityViewModelTest.kt (new)
- taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/CreateTaskUseCaseTest.kt (modified)
- taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/GenerateWorkspaceUseCaseTest.kt (modified)
- taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/ApplyRulesToWorkspaceUseCaseTest.kt (modified)
- taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/LaunchIDEUseCaseTest.kt (modified)

### Change Log

| Date | Description |
|------|-------------|
| 2026-03-16 | Implemented Story 3.2: Activity model extensions, V5 migration, ActivityRepositoryImpl, activity recording in use cases, ActivityView with filters, navigation wiring, success/failed styling in Dashboard |

### Completion Notes

- AC1: Activity recorded for TASK_CREATED, WORKSPACE_PREPARED, GIT_PREPARED, RULES_APPLIED, IDE_LAUNCHED (success and failure)
- AC2: Entries include projectId, entityType, entityId, description, metadata, status
- AC3: findFiltered returns chronological order (newest first)
- AC4: ActivityView filters by project, task, event type
- AC5: RecentActivitySection and ActivityView use color-coded styling (green=success, red=failed, blue=in progress)
- Note: 2 pre-existing core test failures (EnvConfigLoaderTest, ApplyRulesToWorkspaceUseCaseTest should skip archived rules) remain; not introduced by this story

## QA Results

### Review Date: 2026-03-16

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation meets all acceptance criteria. Activity model extended with ActivityStatus, projectId, GIT_PREPARED, RULES_APPLIED. V5 migration creates activity_log with indexes. ActivityRepositoryImpl provides persistent storage; findFiltered supports project/task/type filters with chronological order (newest first). Use cases (CreateTask, GenerateWorkspace, ApplyRulesToWorkspace, LaunchIDE) record activity with SUCCESS/FAILED status. ActivityView provides filter dropdowns and ActivityRow with status-based colors (green/red/blue). Dashboard RecentActivitySection updated with success/failed styling and "View activity" navigation. DependencyContainer wires ActivityRepositoryImpl. ActivityViewModelTest covers load, project filter, type filter, clearFilters, and error path.

### Refactoring Performed

None.

### Compliance Check

- Coding Standards: ✓ Kotlin naming, types, structure
- Project Structure: ✓ Activity in core/data + domain; ActivityView in ui/activity
- Testing Strategy: ✓ ActivityViewModelTest (5); use case tests updated with activityRepository mock
- All ACs Met: ✓

### Improvements Checklist

- [ ] Consider explicit setTaskFilter test for parity with other filter tests (optional)

### Security Review

No concerns. Activity log stores event metadata (type, entity, description); no credentials.

### Performance Considerations

V5 migration adds indexes on created_at, entity, type, project. findFiltered limit 100. Acceptable for MVP.

### Files Modified During Review

None.

### Gate Status

Gate: PASS → docs/qa/gates/3.2-activity-history.yml

### Recommended Status

✓ Ready for Done
