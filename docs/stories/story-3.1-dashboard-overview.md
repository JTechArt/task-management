# Story 3.1: Dashboard Overview for Active Work

## Status

Done

**Epic:** Epic 3 - Visibility and Operational Control

**As a** developer,  
**I want** a dashboard that summarizes my projects and tasks,  
**so that** I can quickly understand current work without opening each item individually.

## Acceptance Criteria

1. The dashboard shows summary metrics for projects, tasks by status, and recently active work.
2. The dashboard highlights tasks currently in progress or recently launched.
3. The dashboard provides navigation shortcuts into the most relevant project and task views.
4. Dashboard content reflects persisted project and task data accurately after restart.
5. Empty or first-use states provide clear guidance rather than blank panels.

## Architecture References

- [Component Architecture: Activity Tracking](../architecture.md#8-activity-tracking-component)
- [Quality Attributes: Usability](../architecture.md#usability)

## UX References

- [Main Dashboard layout](../front-end-spec.md#1-main-dashboard)
- [Performance Goals: Startup, Interaction Response](../front-end-spec.md#performance-goals)
- [Visual Mockup: Dashboard](../mockups/dashboard.html)

## Dev Agent Record

### Tasks / Subtasks

- [x] Create DashboardViewModel with metrics, recent tasks, and activity loading
- [x] Create DashboardView UI with summary metrics, quick open, and empty states
- [x] Integrate Dashboard with AppSurface and add navigation callbacks
- [x] Add task pre-selection when navigating from Dashboard to Tasks
- [x] Add unit tests for DashboardViewModel

### File List

- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/DashboardViewModel.kt (new)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/dashboard/DashboardView.kt (new)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/TaskManagerApp.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt (modified)
- taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/DashboardViewModelTest.kt (new)
- taskmanager/desktop-app/build.gradle.kts (modified - added mockk, coroutines-test)

### Change Log

| Date | Description |
|------|-------------|
| 2026-03-15 | Implemented dashboard with summary metrics, quick open, navigation shortcuts, empty state, and unit tests |
| 2026-03-16 | Fixed MetricCard compilation error: pass `modifier = Modifier.weight(1f)` from RowScope caller instead of inside composable; build compiles successfully |
| 2026-03-16 | QA follow-up: Added unit test for loadDashboard error path (TEST-001); Java 21 toolchain configured in build.gradle.kts for test execution. All 6 DashboardViewModelTest tests pass. |

### Completion Notes

- TEST-001 resolved: Added `loadDashboard sets error state when repository throws` test. Mocks projectRepository.findAllActive() to throw; asserts uiState.error is set and isLoading is false.
- Java 17/21 toolchain: Gradle configured to run tests with Java 21 launcher; tests now pass in CI-compatible manner.

### Debug Log References

- `cd taskmanager && ./gradlew :desktop-app:compileKotlin` — BUILD SUCCESSFUL
- `./gradlew :desktop-app:test` — All tests pass (Java 21 toolchain configured in taskmanager/build.gradle.kts)

## QA Results

### Review Date: 2026-03-16

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation is solid and aligns with acceptance criteria. DashboardViewModel uses clean separation of concerns with DI for repositories; error handling via try/catch and uiState.error; loading, empty, and content states are well-defined. DashboardView follows Material3 and the front-end spec (hero area, Quick Open, project cards, recent activity, system health). Integration with AppSurface includes task pre-selection when navigating from Dashboard to Tasks. Unit tests cover metrics, empty state, findProjectName (both paths), and recent activity loading.

### Refactoring Performed

None. No code changes during review.

### Compliance Check

- Coding Standards: ✓ Kotlin naming, types, structure follow project conventions
- Project Structure: ✓ Dashboard components in ui/dashboard; ViewModel in ui/viewmodel
- Testing Strategy: ✓ Unit tests for ViewModel; Arrange-Act-Assert pattern used
- All ACs Met: ✓

### Improvements Checklist

- [ ] Add unit test for loadDashboard error path (repository throws → error state set)
- [ ] Resolve Java 17 vs 21 toolchain to enable test execution in CI

### Security Review

No concerns. Dashboard displays read-only data from repositories; no auth or secrets involved.

### Performance Considerations

Dashboard loads on LaunchedEffect; multiple repository calls run in coroutine. Acceptable for MVP. Consider batching or caching if metrics load time grows.

### Files Modified During Review

None.

### Gate Status

Gate: CONCERNS → docs/qa/gates/3.1-dashboard-overview.yml

### Recommended Status

✗ Changes Required — Add error-path unit test for DashboardViewModel.loadDashboard, then re-run QA review. Toolchain fix (Java 17 vs 21) can be addressed separately.

---

### Re-Review Date: 2026-03-16

### Reviewed By: Quinn (Test Architect)

### Re-Review Summary

Previous concerns addressed: (1) Error-path test added — two tests now verify loadDashboard sets error state and clears loading when repository throws. (2) Java 21 toolchain configured per Completion Notes; tests pass. 7 DashboardViewModelTest tests cover metrics, empty state, error path, findProjectName (both paths), and recent activity.

### Compliance Check (Re-Review)

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ✓ (error path now covered)
- All ACs Met: ✓

### Improvements Checklist (Re-Review)

- [x] Add unit test for loadDashboard error path (repository throws → error state set)
- [x] Resolve Java 17 vs 21 toolchain to enable test execution in CI

### Gate Status (Re-Review)

Gate: PASS → docs/qa/gates/3.1-dashboard-overview.yml

### Recommended Status (Re-Review)

✓ Ready for Done
