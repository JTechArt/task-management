# Story 7.1: Local LLM (Llama) Configuration

**Epic:** Epic 7 - Local AI/ML Integration

**As a** developer who prefers local inference,  
**I want** to configure a local Llama (or compatible) endpoint in AiTask settings,  
**so that** I can use local AI without relying solely on cloud services.

## Status

Done

## Acceptance Criteria

1. A user can configure a local LLM (e.g., Llama) endpoint in AiTask settings.
2. The configuration includes endpoint URL, model identifier, and optional API key or auth if required.
3. The application validates connectivity to the configured endpoint before saving.
4. LLM configuration persists across application sessions.
5. Multiple LLM configurations can be stored; one is designated as the default for AI-assisted features.

## Requirements Mapping (from features-v2.md)

- AI-1: The system MUST support configuration of a local LLM (e.g., Llama) for use within AiTask.

## Architecture References

- [Integration Architecture: External APIs](../architecture.md)
- [Settings / Configuration](../architecture.md)

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Mockup: AI Studio](../mockups/ai-studio.html)

## Tasks / Subtasks

- [x] Add persisted LLM configuration domain, repository, entity, and migration support
- [x] Add endpoint validation for local LLM connectivity before save
- [x] Wire LLM profile management into the desktop settings experience
- [x] Add view-model and repository tests covering save, validation failure, default selection, and SQL persistence round-trip
- [x] Run `:core:test` and `:desktop-app:test`

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew :core:test :desktop-app:test`

### Completion Notes List

- Added a new local LLM profile model with encrypted API key storage support and persisted default selection.
- Implemented a connection validator that probes local or OpenAI-compatible endpoints before save.
- Replaced the placeholder settings flow with a functional LLM profile editor, profile list, default selector, and connection test action.
- Added an H2-backed repository integration test to prove SQL persistence and default selection survive a fresh repository instance.
- Kept the existing export/import and backup/restore controls intact on the same Settings screen.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/LlmConfiguration.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/repository/LlmConfigurationRepository.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/LlmConnectionValidator.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/LlmConfigurationEntity.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/InMemoryLlmConfigurationRepository.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/LlmConfigurationRepositoryImpl.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/llm/DefaultLlmConnectionValidator.kt` (new)
- `taskmanager/core/src/main/resources/db/migration/V15__add_llm_configurations.sql` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/data/repository/InMemoryLlmConfigurationRepositoryTest.kt` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/data/repository/LlmConfigurationRepositoryImplTest.kt` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/llm/DefaultLlmConnectionValidatorTest.kt` (new)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/PlaceholderViews.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/SettingsViewModel.kt` (rewritten)
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/SettingsViewModelTest.kt` (new)

### Change Log

| Date | Description |
|------|-------------|
| 2026-03-25 | Implemented persisted local LLM profile management, endpoint validation, and Settings UI integration; verified with `:core:test` and `:desktop-app:test`. |
| 2026-03-25 | Added SQL repository integration coverage and local validation coverage to address QA concern on persistence traceability. |

## QA Results

### Review Date: 2026-03-25

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation aligns with the five acceptance criteria: settings UI exposes name, endpoint, model, optional API key, default flag, test and save actions; `SettingsViewModel` validates remotely before persist; `DefaultLlmConnectionValidator` probes OpenAI-compatible and Ollama-style paths with optional Bearer auth; SQL migration and `LlmConfigurationRepositoryImpl` provide encrypted-at-rest API keys and single-default semantics. Structure follows existing use-case and DI patterns.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓ Kotlin style and explicit types are consistent with surrounding modules.
- Project Structure: ✓ Domain, data, infrastructure, and desktop layers are separated appropriately.
- Testing Strategy: ⚠ Unit coverage is solid for validator, in-memory repository, and view-model success/failure/default flows; see gaps below.
- All ACs Met: ⚠ Behavior in source appears to satisfy all five ACs; automated tests do not fully evidence AC4 against the SQL-backed repository.

### Requirements Traceability (summary)

| AC | Coverage | Notes |
|----|----------|-------|
| 1 | Partial | Exercised via view-model + UI code; no Compose/UI test. |
| 2 | Full | `SettingsViewModelTest` save path includes URL, model, API key. |
| 3 | Full | Save and validator tests cover pre-save validation and failure. |
| 4 | Partial | Migration + `LlmConfigurationRepositoryImpl` implemented; tests use `InMemoryLlmConfigurationRepository` only. |
| 5 | Full | `setDefaultLlmConfiguration` and repository tests. |

### Improvements Checklist

- [ ] Add an integration or repository test for `LlmConfigurationRepositoryImpl` (and migration V15) to close AC4 at the persistence layer.
- [ ] Add unit tests for local `validateLlmEditorLocally` branches (blank name, URL, model).
- [x] Confirmed `:core:test` and `:desktop-app:test` pass in review environment (2026-03-25).

### Security Review

API keys are encrypted via `EncryptionService` before storage. Endpoint URLs are restricted to `http`/`https` in `DefaultLlmConnectionValidator`. Outbound requests follow user-configured endpoints (desktop client context; not server-side SSRF). No secrets logged in reviewed paths.

### Performance Considerations

Connection probes use short connect/read timeouts (3s default) and sequential candidate URLs; appropriate for a settings-time validation.

### Files Modified During Review

None.

### Gate Status

Gate: CONCERNS → [docs/qa/gates/7.1-local-llm-configuration.yml](docs/qa/gates/7.1-local-llm-configuration.yml)

### Recommended Status

✗ Changes recommended — address medium test gap for SQL-backed persistence (or explicitly accept as debt with sign-off) before marking Done.

---

### Follow-up review date: 2026-03-25

### Reviewed By: Quinn (Test Architect)

### Code quality assessment

`LlmConfigurationRepositoryImplTest` uses a file-backed H2 database with `LlmConfigurationRepositoryImpl`, asserts encrypted storage of API keys, and reloads data through a second repository instance on the same JDBC URL — closing the prior AC4 traceability gap at the persistence layer. `SettingsViewModelTest` adds a guard-rail test for local validation before `testLlmConfigurationConnection()`.

### Refactoring performed

None (review-only).

### Compliance check

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ✓ Core validator, in-memory and SQL repository, and settings VM paths covered; `./gradlew :core:test :desktop-app:test` passes.
- All ACs Met: ✓ Automated tests now evidence AC4 against `LlmConfigurationRepositoryImpl` (H2); AC1 still has no Compose/UI test (acceptable residual).

### Requirements traceability (follow-up)

| AC | Coverage | Notes |
|----|----------|-------|
| 1 | Partial | VM + UI code; no UI automation. |
| 2 | Full | |
| 3 | Full | |
| 4 | Full | `LlmConfigurationRepositoryImplTest` — note schema created with Exposed, not Flyway V15 in test. |
| 5 | Full | |

### Improvements checklist (delta)

- [x] SQL-backed repository round-trip test added.
- [x] Local validation before connection test (blank name) covered.
- [ ] Optional: assert blank endpoint and blank model local validation messages.

### Gate status (follow-up)

Gate: PASS → [docs/qa/gates/7.1-local-llm-configuration.yml](docs/qa/gates/7.1-local-llm-configuration.yml)

### Recommended status (follow-up)

✓ **Ready for Done** from a QA perspective (story owner sets final status). Optional low-priority tests remain as noted above.
