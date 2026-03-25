# Story 8.1: GEPPA Integration Enablement

**Epic:** Epic 8 - GEPPA (Prompt Optimization)

**As a** developer,  
**I want** to enable GEPPA integration in AiTask settings,  
**so that** my prompts can be optimized for consistency and quality before use in AI-assisted workflows.

## Status

Done

## Acceptance Criteria

1. A user can enable or disable GEPPA integration from AiTask settings.
2. When enabled, the user can configure GEPPA endpoint or path (local service or external API).
3. The application validates GEPPA availability when enabled and reports connection status.
4. GEPPA configuration is persisted and applied across application sessions.
5. The application surfaces clear feedback if GEPPA is unavailable or misconfigured.

## Requirements Mapping

- PRE-GEPPA-1, GEPPA-1 (Must Have): GEPPA integration support

## Dependencies

- Epic 7: Local AI/ML Integration (shared configuration patterns)

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Mockup: AI Studio](../mockups/ai-studio.html)

## Dev Notes

- GEPPA may be a local tool or external API; design configuration to support both.
- Consider compatibility with existing AI/LLM settings in the application.
- Validate connection on save or on-demand; avoid blocking startup.

## QA Results

### Review Date: 2026-03-25

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation follows existing patterns: Flyway `V18` table, Exposed `GeppaConfigurations`, repository, `DefaultGeppaConnectionValidator` with GET health probing and http/https validation, settings UI card with enable/disable, test connection, save, and user-visible error/success feedback. **Critical gap:** `SettingsViewModel` `init` calls `loadGeppaConfiguration()`, which uses Exposed, but `SettingsViewModelTest` does not inject a `GeppaConfigurationRepository` mock, so all six tests fail with `Please call Database.connect()`. `:core:test` passes; `:desktop-app:test` fails.

### Refactoring Performed

None (QA is advisory; no code changes in this review).

### Compliance Check

- Coding Standards: Not verified against `docs/architecture/coding-standards.md` (file not present at expected path); Kotlin style matches surrounding modules.
- Project Structure: Aligned with core `data` / `domain` / `infrastructure` layout and desktop settings UI.
- Testing Strategy: **Not met** — desktop tests red; no new tests for GEPPA types.
- All ACs Met: **Cannot confirm** until tests are fixed and extended; manual code review suggests settings-side ACs are largely addressed; UI text implies runtime prompt routing that is not implemented elsewhere in code (enablement-only).

### Improvements Checklist

- [ ] Mock `GeppaConfigurationRepository` (and `GeppaConnectionValidator` if required) in `SettingsViewModelTest` to restore green CI.
- [ ] Add `GeppaConfigurationRepositoryImplTest` (H2 + `SchemaUtils`) and unit tests for `DefaultGeppaConnectionValidator` / URL rules.
- [ ] Align “prompts are routed through GEPPA” copy with actual behavior or track a follow-up story for runtime integration.

### Security Review

Endpoint validation requires `http`/`https`; probes use short timeouts. User-controlled URL for outbound GET is consistent with desktop LLM settings; no credentials stored for GEPPA in reviewed code.

### Performance Considerations

3s connect/read timeouts on validation; acceptable for settings flows.

### Files Modified During Review

None.

### Gate Status

Gate: FAIL → docs/qa/gates/8.1-geppa-integration.yml  
Risk profile: docs/qa/assessments/8.1-risk-20260325.md  
NFR assessment: docs/qa/assessments/8.1-nfr-20260325.md

### Recommended Status

**Changes Required** — initial review only; superseded by follow-up QA below.

### Review Date: 2026-03-25 (follow-up)

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Prior FAIL items are resolved: `SettingsViewModelTest` injects mock `GeppaConfigurationRepository` and `GeppaConnectionValidator`; `coEvery { geppaConfigurationRepository.find() } returns null` satisfies `loadGeppaConfiguration()` in `init`. New coverage includes `GeppaConfigurationRepositoryImplTest` (H2 + `SchemaUtils`), `DefaultGeppaConnectionValidatorTest` (local `HttpServer` + negative cases), and four GEPPA save-path tests on the view model. `./gradlew :core:test :desktop-app:test` completes successfully.

### Refactoring Performed

None in this review.

### Compliance Check

- Coding Standards: Kotlin patterns consistent with Epic 7 settings modules.
- Project Structure: Matches core repository and desktop settings conventions.
- Testing Strategy: Met for story scope — repository, validator, and primary save/validation paths covered.
- All ACs Met: Yes — enable/disable, URL configuration, validation when enabled, SQLite persistence via Flyway/Exposed, and user-visible errors/feedback are implemented and traceable to tests. Runtime prompt routing is explicitly deferred (story 8.2); settings subtitle now states integration is “configured and available” rather than implying live routing.

### Improvements Checklist

- [x] Mock GEPPA dependencies in `SettingsViewModelTest`
- [x] Add repository and validator automated tests
- [x] Align UI copy with enablement-only scope

### Optional follow-up

- [ ] Unit test `SettingsViewModel.testGeppaConnection()` (blank endpoint, success, failure) for parity with save flows.

### Security Review

Unchanged: scheme and timeout constraints; desktop-appropriate trust model for user-entered service URL.

### Performance Considerations

Unchanged: short client timeouts for validation.

### Files Modified During Review

None.

### Gate Status

Gate: PASS → docs/qa/gates/8.1-geppa-integration.yml  
Risk profile: docs/qa/assessments/8.1-risk-20260325.md (superseded for residual risk — see gate)  
NFR assessment: docs/qa/assessments/8.1-nfr-20260325.md

### Recommended Status

**Done** (confirmed).
