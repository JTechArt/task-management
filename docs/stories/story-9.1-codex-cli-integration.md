# Story 9.1: Codex CLI Integration

**Epic:** Epic 9 - AI Tools Integration (Codex, Claude)

**As a** developer,  
**I want** to configure and invoke Codex CLI from AiTask with task context,  
**so that** I can run AI-assisted terminal workflows without leaving the task context.

## Status

Done

## Tasks / Subtasks

- [x] Persist Codex settings (path, optional API key, enable flag) and Flyway migration
- [x] Resolve and validate Codex CLI (PATH or custom path); Settings Test/Save validation when enabled
- [x] Launch Codex from task with pre-run scripts, TASK_CONTEXT.md, env vars, workspace cwd, terminal UX
- [x] Task detail "Run with Codex" + success/error feedback; activity `CODEX_LAUNCHED`
- [x] Unit tests and `./gradlew :core:test :desktop-app:test`

## Acceptance Criteria

1. A user can configure Codex CLI path and credentials (if required) in AiTask settings.
2. The application validates Codex CLI availability when configured.
3. When "Run with Codex" is invoked from a task, AiTask passes project name, task description, and repository path as context.
4. The application launches Codex with the task workspace as the working directory.
5. Invocation success or failure is reported to the user with clear feedback.

## Requirements Mapping

- AITOOL-1 (Must Have): Codex CLI integration for AI-assisted terminal workflows
- AITOOL-3 (Should Have): Task context passed to AI tools

## Dependencies

- Epic 1: IDE Launch flow (similar invocation pattern)
- Epic 5: Pre-Run Scripts (optional: run pre-run before Codex)

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Mockup: AI Studio](../mockups/ai-studio.html)
- [Mockup: Tasks](../mockups/tasks.html)

## Dev Notes

- Codex CLI typically runs in terminal; design for subprocess launch with context injection.
- Consider environment variable or config file for passing task context to Codex.
- Support both configured path and system PATH lookup.

## Dev Agent Record

### Agent Model Used

Cursor agent (GPT-5.1)

### Completion Notes

- Added `codex_configurations` (V20 migration), repository with encrypted optional API key, `CodexCliService` / `DesktopCodexCliService` (PATH resolution, `--version`/`--help` validation, macOS Terminal / Windows cmd / Linux gnome-terminal or fallback).
- `LaunchCodexUseCase` mirrors IDE pre-run flow, writes `TASK_CONTEXT.md` via `TaskContextFileWriter` (includes **Repository path**), sets `AITASK_PROJECT_NAME`, `AITASK_REPOSITORY_PATH`, `AITASK_TASK_CONTEXT_FILE`, optional `OPENAI_API_KEY`.
- Settings: Codex CLI card; Tasks: "Run with Codex" when integration enabled; `TasksViewModel.refreshCodexIntegration()` on load.
- QA follow-up (TEST-001): `LaunchCodexUseCaseTest` (core); `SettingsViewModelTest` for `testCodexCli` and `saveCodexConfiguration` when Codex enabled; `TasksViewModelTest` for `runCodex` success and failure. MockK matchers use `import io.mockk.*` where `any`/`match` are required.

### Debug Log References

- `./gradlew :core:compileKotlin :desktop-app:compileKotlin` — success
- `./gradlew :core:test :desktop-app:test` — success (after QA test additions, 2026-03-30)

### File List

- `taskmanager/core/src/main/resources/db/migration/V20__add_codex_configurations.sql`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/CodexConfigurationEntity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/CodexConfiguration.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/repository/CodexConfigurationRepository.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/CodexConfigurationRepositoryImpl.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/CodexCliService.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/codex/DesktopCodexCliService.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/ide/TaskContextFileWriter.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/ide/DesktopIDEService.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/IDE.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Activity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/LaunchIDEUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/LaunchCodexUseCase.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/SettingsViewModel.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/PlaceholderViews.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/data/repository/CodexConfigurationRepositoryImplTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/LaunchCodexUseCaseTest.kt`
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/SettingsViewModelTest.kt`
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModelTest.kt`

### Change Log

- 2026-03-30: Implemented Codex CLI settings, validation, task launch with context and feedback (story 9.1).
- 2026-03-30: Closed QA TEST-001 gaps — unit tests for `LaunchCodexUseCase`, Codex paths in `SettingsViewModelTest`, `runCodex` in `TasksViewModelTest`; full `:core:test` and `:desktop-app:test` green.
- 2026-03-30: Story marked Done; QA gate PASS (`docs/qa/gates/9.1-codex-cli-integration.yml`).

## QA Results

### Review Date: 2026-03-30

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation follows existing IDE/pre-run patterns: `LaunchCodexUseCase` composes workspace validation, optional pre-run scripts, `TaskContextFileWriter` for `TASK_CONTEXT.md`, `CodexCliService` for resolve/validate/launch, and activity recording with `ActivityType.CODEX_LAUNCHED`. Settings and task UI mirror other integrations. `./gradlew :core:test :desktop-app:test` (from `taskmanager/`) completed successfully during this review.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓ Kotlin style and DI patterns align with surrounding modules.
- Project Structure: ✓ Core domain, infrastructure, desktop UI, and migration placement are consistent with the repo.
- Testing Strategy: ✗ New flows are under-tested relative to project expectations (see Improvements).
- All ACs Met: ✓ Behavior in source satisfies AC1–AC5; automated tests do not fully exercise every AC.

### Improvements Checklist

- [ ] Add unit tests for `LaunchCodexUseCase` (success, disabled Codex, missing workspace, pre-run failure, CLI resolve/validate/launch failure) with test doubles.
- [ ] Add `SettingsViewModelTest` coverage for `testCodexCli` and `saveCodexConfiguration` when Codex is enabled (mock `CodexCliService` resolve/validate).
- [ ] Add `TasksViewModelTest` coverage for `runCodex` success and failure messaging.
- [ ] Optional: fake `CodexCliService` for asserting env vars and `TASK_CONTEXT.md` content in core tests.

### Security Review

Optional API key is persisted encrypted (`CodexConfigurationRepositoryImpl`); CLI path is validated as a concrete file before use; task workspace drives cwd and context file location—no path traversal from user-supplied strings beyond configured executable path. No secrets logged in reviewed paths.

### Performance Considerations

CLI resolution and `--version`/`--help` probes use bounded waits; no concerns for typical desktop use.

### Files Modified During Review

None.

### Gate Status

Gate: CONCERNS → docs/qa/gates/9.1-codex-cli-integration.yml  
Risk profile: docs/qa/assessments/9.1-risk-20260330.md  
NFR assessment: docs/qa/assessments/9.1-nfr-20260330.md

### Recommended Status

✗ Changes Required — Close test gaps above (or split a follow-up story) before **Done**; PO may accept **Ready for Done** with CONCERNS if debt is tracked.

### Follow-up Review Date: 2026-03-30

### Reviewed By: Quinn (Test Architect)

### Summary

Second pass: searched the codebase for `LaunchCodexUseCase` tests, `runCodex` / `testCodexCli` coverage in ViewModel tests—**no new tests** since the first review; `LaunchCodexUseCase` appears only under `main`. Re-ran `./gradlew :core:test :desktop-app:test` from `taskmanager/` — completed successfully (exit 0).

### Refactoring Performed

None.

### Compliance Check (delta)

- Testing Strategy: ✗ Unchanged — gaps listed in the first review’s Improvements Checklist remain open.

### Gate Status

Gate: **CONCERNS** (unchanged) → docs/qa/gates/9.1-codex-cli-integration.yml

### Recommended Status

Same as first review: add targeted tests or explicitly accept debt; gate elevates to PASS when `TEST-001` is addressed.

### Review Date: 2026-03-30 (third pass — post TEST-001)

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

`LaunchCodexUseCaseTest` exercises disabled/null config, missing task/workspace/project, pre-run failure, CLI resolve/validate/launch failures, happy path with `TASK_CONTEXT.md` and env capture (including optional `OPENAI_API_KEY`). `SettingsViewModelTest` covers `testCodexCli` success/failure and `saveCodexConfiguration` validate-and-save vs resolve failure. `TasksViewModelTest` covers `runCodex` success and error UI state. `./gradlew :core:test :desktop-app:test` completed successfully during this review.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ✓ Prior gaps closed.
- All ACs Met: ✓ With unit-level traceability to repository, settings validation, use case context/env, and task feedback.

### Improvements Checklist

- [x] `LaunchCodexUseCase` unit tests (see `LaunchCodexUseCaseTest.kt`)
- [x] `SettingsViewModelTest` Codex test/save paths
- [x] `TasksViewModelTest` `runCodex` success/failure
- [ ] Optional: contract tests for `DesktopCodexCliService` launch scripts (low priority)

### Security Review

Unchanged: encrypted key at rest; tests use mocked secrets for `OPENAI_API_KEY` propagation only.

### Performance Considerations

Unchanged.

### Files Modified During Review

None.

### Gate Status

Gate: **PASS** → docs/qa/gates/9.1-codex-cli-integration.yml  
Risk profile: docs/qa/assessments/9.1-risk-20260330.md (superseded for TEST-001; residual low OPS note in gate)  
NFR assessment: docs/qa/assessments/9.1-nfr-20260330.md

### Recommended Status

✓ **Ready for Done** — PO may set story Status to Done after their checklist; prior CONCERNS for TEST-001 is cleared.
