# Story 11.3: Plugin Configuration and Dependency Validation

**Epic:** Epic 11 - Plugin Management and Add-on Framework

**As a** developer,  
**I want** each plugin to expose its own configuration and prerequisite checks,  
**so that** a plugin only runs when its required tools and services are available.

## Status

Done

## Acceptance Criteria

1. Each plugin can define structured configuration fields, secrets, schedules, and validation rules.
2. The application validates plugin prerequisites such as local binaries, endpoints, credentials, or companion apps before enablement.
3. Missing prerequisites are presented with actionable remediation guidance.
4. Plugin configuration persists between sessions and is scoped appropriately to app-wide or project-level use.
5. Invalid plugin configuration does not corrupt previously valid plugin state.

## Requirements Mapping

- FR31: Per-plugin configuration and persistence
- FR32: Plugin prerequisite and dependency validation
- FR33: Invalid plugin state cannot break core operations

## Dependencies

- Story 11.1: Plugin Framework and Lifecycle Contracts
- Story 11.2: Plugin Catalog, Install, Attach, and Remove

## Architecture References

- [Architecture Overview](../architecture.md)

## UX References

- [Front-end Spec: Plugin Catalog & Operational Health](../front-end-spec.md#15-plugin-catalog--operational-health)
- [Mockup: Plugins](../mockups/plugins.html)

## Dev Notes

- Validation needs to support both local prerequisites and external service checks.
- Preserve last known-good plugin configuration when new settings fail validation.

## Tasks / Subtasks

- [x] Extend the plugin domain with structured configuration, prerequisite, and validation models
- [x] Add repository and service plumbing for persisting and validating plugin configuration snapshots
- [x] Wire plugin configuration editing and validation into the desktop plugin management experience
- [x] Add migration and dependency wiring for plugin configuration persistence and prerequisite probing
- [x] Add unit tests covering configuration persistence, validation failures, prerequisite checks, and the configuration editor flow

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew :core:test :desktop-app:test`
- `./gradlew test`

### Completion Notes List

- Added structured plugin configuration and prerequisite models so plugins can declare fields, secrets, schedules, and validation rules.
- Implemented plugin configuration persistence with last-known-good tracking and validation-aware save behavior.
- Added prerequisite probing for local binaries, endpoints, credentials, and companion apps, then wired it into enablement checks.
- Extended the desktop plugin management experience with a configuration dialog, validation feedback, and scoped save actions.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Activity.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/plugin/PluginFramework.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/plugin/PluginCatalog.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/plugin/PluginConfiguration.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/PluginManagementService.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/PluginPrerequisiteProbe.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/repository/PluginConfigurationRepository.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/PluginConfigurationEntity.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/InMemoryPluginConfigurationRepository.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/PluginConfigurationRepositoryImpl.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/plugin/DefaultPluginPrerequisiteProbe.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/plugin/InMemoryPluginManagementService.kt` (modified)
- `taskmanager/core/src/main/resources/db/migration/V14__add_plugin_configurations.sql` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/plugin/InMemoryPluginManagementServiceTest.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/plugins/PluginManagementView.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/PluginManagementViewModel.kt` (modified)
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/PluginManagementViewModelTest.kt` (modified)

### Change Log

| Date | Description |
|------|-------------|
| 2026-03-24 | Implemented plugin configuration persistence, prerequisite validation, scoped configuration editing, and verification tests; marked story Ready for Review. |

## QA Results

### Review Date: 2026-03-24

### Reviewed By: Quinn (Test Architect)

### Requirements Traceability

| AC | Summary | Evidence |
| --- | --- | --- |
| 1 | Structured fields, secrets, schedules, validation rules | `PluginConfigurationField`, `PluginConfigurationFieldType` (including `SECRET`, `SCHEDULE`), `PluginValidationRule`, `PluginConfigurationSchema`; fixtures in `PluginCatalogFixtures` |
| 2 | Validate prerequisites before enablement | `ensureConfigurationReady` in `enable`; `DefaultPluginPrerequisiteProbe` (binary, endpoint, credential, companion app); `InMemoryPluginManagementServiceTest` `validation surfaces missing prerequisites before enablement` |
| 3 | Actionable remediation | `PluginPrerequisite.remediation`; UI lists prerequisite guidance and `ConfigurationValidationSummary` / `PrerequisiteRow` with remediation text |
| 4 | Persistence and APP vs PROJECT scope | `PluginConfigurationScope`; `PluginConfigurationRepository` + `V14__add_plugin_configurations.sql`; `PluginConfigurationRepositoryImpl`; project scope key field in `PluginConfigurationDialog` |
| 5 | Invalid config does not corrupt valid state | `saveConfiguration` validates before persist; test `invalid configuration does not replace the last valid snapshot`; SQL `last_known_good_configuration_json` updated only when `VALID` |

### Code Quality Assessment

Domain models are explicit and serializable; validation composes field rules, per-rule prerequisites, and schema-level prerequisites. `InMemoryPluginManagementService` centralizes evaluation and blocks enablement when validation fails. Desktop view model exposes editor state, validate, and save with failure mapped to `PluginConfigurationValidationException`.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓ Domain, infrastructure probe, data repository, desktop UI layers respected
- Testing Strategy: ✓ Service tests for persistence, invalid save, prerequisites; view model test for configuration editor flow
- All ACs Met: ✓

### Improvements Checklist

- [ ] Treat `DefaultPluginPrerequisiteProbe` endpoint URLs as trusted-only or add allowlisting if plugins become user-supplied
- [ ] Optional: integration test for `PluginConfigurationRepositoryImpl` + migration V14

### Security Review

`HttpURLConnection` to `PluginPrerequisiteType.ENDPOINT` values performs outbound requests from the desktop process. Acceptable when manifests are shipped/trusted; re-evaluate if arbitrary third-party plugin URLs are allowed (SSRF-style abuse). Credential checks use env and configuration maps—no secrets logged in code paths reviewed.

### Performance Considerations

Endpoint checks use short connect/read timeouts; prerequisite evaluation runs on IO dispatcher.

### Files Modified During Review

None.

### Gate Status

Gate: CONCERNS → docs/qa/gates/11.3-plugin-configuration-and-dependency-validation.yml

### Recommended Status

Done — story status set 2026-03-24 following QA gate PASS (re-review).

### Test Execution Note

Tests were not executed in the QA environment; rely on `./gradlew :core:test :desktop-app:test` locally for confirmation.

### Review Date: 2026-03-24 (re-review)

### Reviewed By: Quinn (Test Architect)

### Scope

Second pass on the same implementation (no code changes reviewed between passes). Re-evaluated the prior **CONCERNS** gate: outbound `ENDPOINT` prerequisite checks are driven only by **built-in** `PluginCatalogFixtures` / shipped manifests in this MVP, not by arbitrary user URLs. Under that **trusted-catalog** assumption, the finding is a **future hardening** item (SEC-001) rather than a release blocker.

### Outcome

- **Traceability:** Unchanged—all five ACs remain mapped to implementation and tests as in the first review.
- **Gate update:** Quality gate raised from **CONCERNS** to **PASS**; see current `docs/qa/gates/11.3-plugin-configuration-and-dependency-validation.yml` (supersedes the 16:00Z decision).
- **Residual risk:** If the product later loads plugin definitions from **untrusted** sources, re-open SEC-001 (allowlisting or disabling network probes).

### Gate Status (re-review)

Gate: PASS → docs/qa/gates/11.3-plugin-configuration-and-dependency-validation.yml
