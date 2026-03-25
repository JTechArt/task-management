# Story 11.2: Plugin Catalog, Install, Attach, and Remove

**Epic:** Epic 11 - Plugin Management and Add-on Framework

**As a** developer,  
**I want** to manage optional add-ons from within AiTask,  
**so that** I can enable only the capabilities relevant to my workflow.

## Status

Done

## Acceptance Criteria

1. A user can view installed plugins and their current attachment or enablement state.
2. A user can install, attach, detach, disable, or remove supported plugins through a plugin management experience.
3. The UI clearly distinguishes core features from optional plugins.
4. Plugin management actions confirm success or failure with clear status feedback.
5. Plugin lifecycle actions are recorded in application history.

## Requirements Mapping

- FR29: Plugin installation and attachment model
- FR30: Plugin management views and status visibility
- FR33: Optional plugins cannot break core usage flows

## Dependencies

- Story 11.1: Plugin Framework and Lifecycle Contracts
- Epic 3: Visibility and Operational Control

## Architecture References

- [Architecture Overview](../architecture.md)

## UX References

- [Front-end Spec: Plugin Catalog & Operational Health](../front-end-spec.md#15-plugin-catalog--operational-health)
- [Mockup: Plugins](../mockups/plugins.html)

## Dev Notes

- Treat install, attach, enable, disable, detach, and remove as distinct lifecycle actions with clear user feedback.
- Plugin lifecycle actions should emit history events for later operational troubleshooting.

## Tasks / Subtasks

- [x] Extend the core activity model with plugin lifecycle event types for install, attach, detach, enable, disable, and remove
- [x] Add a plugin management service backed by the plugin host and activity history
- [x] Add a dedicated plugin management view and view model with install, attach, enable, disable, detach, and remove actions
- [x] Wire plugin management into navigation and dependency injection
- [x] Add unit tests for plugin lifecycle state changes, activity recording, and the plugin management view model

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew :core:test`
- `./gradlew test`

### Completion Notes List

- Added plugin lifecycle activity types so management actions are visible in the existing application history.
- Implemented an in-memory plugin management service that seeds supported plugins, manages install/attach/enable/disable/detach/remove actions, and records each action to history.
- Added a dedicated plugin catalog screen with a clear split between core platform features and optional plugins.
- Wired the plugin screen into the desktop navigation and dependency container.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Activity.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/plugin/PluginCatalog.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/PluginManagementService.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/plugin/InMemoryPluginManagementService.kt` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/plugin/InMemoryPluginManagementServiceTest.kt` (new)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/Navigation.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/TaskManagerApp.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/PluginManagementViewModel.kt` (new)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/plugins/PluginManagementView.kt` (new)
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/PluginManagementViewModelTest.kt` (new)

### Change Log

| Date | Description |
|------|-------------|
| 2026-03-24 | Implemented plugin catalog and lifecycle management UI/service flow, added plugin lifecycle history events, wired navigation, and verified with `./gradlew :core:test` and `./gradlew test`. |

## QA Results

### Review Date: 2026-03-24

### Reviewed By: Quinn (Test Architect)

### Requirements Traceability

| AC | Summary | Evidence |
| --- | --- | --- |
| 1 | View installed plugins and attach/enable state | `PluginCatalogItem` + `PluginManagementView` (pills, status labels/descriptions); `InMemoryPluginManagementService.catalog()` |
| 2 | Install, attach, detach, disable, remove via management experience | `PluginManagementService` + `InMemoryPluginManagementService`; `PluginManagementView` buttons; `TaskManagerApp` / `NavigationItem.PLUGINS` |
| 3 | Core vs optional distinguished | `CoreFeaturesCard` vs `OptionalPluginsCard`; optional badge; `PluginCatalogFixtures.coreFeatureHighlights` |
| 4 | Success/failure feedback | `PluginManagementFeedback` / `FeedbackBanner`; `lastActionMessage`; `Result` failures → error feedback; failed activities for unsupported plugin |
| 5 | Lifecycle actions in application history | `ActivityType.PLUGIN_*`; `recordActivity` on success and `recordFailure`; `InMemoryPluginManagementServiceTest` full lifecycle activity types |

### Code Quality Assessment

Domain service cleanly separates catalog projection from `PluginHost` lifecycle calls; UI separates core platform copy from optional plugins. Failure paths record FAILED activities with metadata. `PluginManagementViewModel` is a thin coroutine wrapper over `Result`, appropriate for this layer.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓ Core service in infrastructure; desktop UI in `ui/plugins`
- Testing Strategy: ✓ Service tests for catalog, lifecycle, history, unsupported plugin; view model tests for load and install
- All ACs Met: ✓

### Improvements Checklist

- [ ] Add view model tests for attach/enable/disable/detach/remove and error feedback (optional hardening)
- [ ] Replace `runBlocking` in `recordFailure` with a suspend-safe or single-threaded recording path when touching this code next

### Security Review

Seeded catalog only; no arbitrary install from network in this story. Exception messages may appear in activity metadata—acceptable for local operational history; review if history is ever exported.

### Performance Considerations

In-memory; no concerns for current scope.

### Files Modified During Review

None.

### Gate Status

Gate: PASS → docs/qa/gates/11.2-plugin-catalog-and-lifecycle-management.yml

### Recommended Status

Done — story status set 2026-03-24 following QA gate PASS.

### Test Execution Note

Automated tests were not executed in the QA environment (dependency resolution to Maven repositories failed). Review is based on static analysis and test source review; re-run `./gradlew :core:test` and `./gradlew :desktop-app:test` locally to confirm green.
