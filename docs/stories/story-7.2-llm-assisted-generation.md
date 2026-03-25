# Story 7.2: Local LLM for Task Descriptions and Summaries

**Epic:** Epic 7 - Local AI/ML Integration

**As a** developer,  
**I want** AiTask to generate task descriptions or summaries using my configured local model,  
**so that** I can quickly create or enrich task content without switching tools.

## Status

Done

## Acceptance Criteria

1. AiTask can generate task descriptions or summaries using the configured local model.
2. LLM-assisted generation actions are clearly indicated in the UI.
3. Failed or slow LLM calls provide clear feedback and do not block core application flows.
4. Generated text can be accepted, edited, or rejected by the user before use.
5. Generated content remains editable before it is saved to the task.

## Requirements Mapping

- Local AI generation for task descriptions and summaries

## Architecture References

- [Architecture Overview](../architecture.md)

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Mockup: AI Studio](../mockups/ai-studio.html)
- [Mockup: Tasks](../mockups/tasks.html)

## Tasks / Subtasks

- [x] Add core task-content generation model, service, and use case
- [x] Wire generation into create-task and task-detail UI flows
- [x] Add success and failure tests for the view-model and service
- [x] Run `:core:test` and `:desktop-app:test`

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew :core:test :desktop-app:test`

### Completion Notes List

- Added local LLM generation support for task descriptions and summaries with selectable description and summary prompts.
- Kept generated content editable before save and added explicit accept/discard flows in both create and detail experiences.
- Added HTTP timeouts to the generation client so slow or hung local endpoints do not leave the UI waiting indefinitely.
- Added a failure-path view-model test so generation errors surface cleanly and clear the loading state.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/TaskContentGeneration.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/TaskContentGenerationService.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/GenerateTaskContentUseCase.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/llm/DefaultTaskContentGenerationService.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/repository/LlmConfigurationRepository.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/InMemoryLlmConfigurationRepository.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/LlmConfigurationRepositoryImpl.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/CreateTaskDialog.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt` (modified)
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/llm/DefaultTaskContentGenerationServiceTest.kt` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/GenerateTaskContentUseCaseTest.kt` (new)
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModelTest.kt` (modified)

### Change Log

| Date | Description |
|------|-------------|
| 2026-03-25 | Implemented local LLM-assisted task content generation and updated the task UX to support generate, use, discard, and edit flows. |
| 2026-03-25 | Added HTTP timeouts and a failure-path view-model test to address QA concerns. |

## QA Results

### Review Date: 2026-03-25

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

The feature is wired end-to-end: `GenerateTaskContentUseCase` resolves project and default LLM profile (with optional API key), `DefaultTaskContentGenerationService` posts to Ollama- or OpenAI-compatible endpoints with sensible prompts for description vs summary, and `TasksViewModel.generateTaskContentSuggestion` updates isolated UI state so the rest of the tasks flow keeps working. `CreateTaskDialog` and `TaskComponents` show “Generate description/summary”, a highlighted “Generated suggestion” card, helper copy to edit before save, and **Use suggestion** / **Discard** — satisfying accept/edit/reject and edit-before-persist for descriptions.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓ Clear layered boundaries and typed models.
- Project Structure: ✓ Core use case + infrastructure + desktop VM/UI.
- Testing Strategy: ⚠ Core has happy-path generation and use case tests; desktop VM tests cover success and clear only — not failure or timeout behavior.
- All ACs Met: ✓ Behavior in source satisfies ACs; see reliability/testing notes for AC3 hardening.

### Requirements Traceability (summary)

| AC | Coverage | Notes |
|----|----------|-------|
| 1 | Full | Description + summary modes; create + task detail. |
| 2 | Full | Labels, “Generating…”, suggestion card, helper text. |
| 3 | Partial | Failures surface as `taskContentGenerationError`; dismiss not gated on generation. **Slow/hung calls:** `DefaultTaskContentGenerationService` `HttpClient` has no explicit request/connect timeout (user may wait a long time in “Generating…”). |
| 4 | Full | Use suggestion / discard; text field remains editable. |
| 5 | Full | Suggestion applied to draft only; persist via Create / Save description. |

### Improvements Checklist

- [ ] Add Ktor `HttpTimeout` (or equivalent) on `DefaultTaskContentGenerationService` for connect/request bounds aligned with AC3.
- [ ] Add `TasksViewModelTest` for `generateTaskContentSuggestion` **failure** (assert error string, `isGeneratingTaskContent` false).
- [ ] Optional: `GenerateTaskContentUseCaseTest` for missing project / missing default LLM; OpenAI-path test in `DefaultTaskContentGenerationServiceTest`.
- [ ] Optional: Extend story file with Tasks / Dev Agent Record / File List for handoff hygiene.
- [x] Confirmed `:core:test` and `:desktop-app:test` pass (2026-03-25).

### Security Review

Prompts include user-supplied title/description and project name sent to the user-configured endpoint (expected). API keys loaded via `findApiKey` only for generation; not logged in reviewed paths.

### Performance Considerations

Sequential candidate endpoint attempts may add latency; primary gap is missing explicit client timeouts (see AC3).

### Files Modified During Review

None.

### Gate Status

Gate: CONCERNS → [docs/qa/gates/7.2-llm-assisted-generation.yml](docs/qa/gates/7.2-llm-assisted-generation.yml)

### Recommended Status

✗ Changes recommended — add HTTP timeouts and a failure-path VM test (or document waiver); then re-review for PASS.

---

### Follow-up review date: 2026-03-25

### Reviewed By: Quinn (Test Architect)

### Code quality assessment

Prior QA items are addressed: `DefaultTaskContentGenerationService` installs Ktor `HttpTimeout` (3s connect, 20s request/socket). `TasksViewModelTest` includes `generateTaskContentSuggestion surfaces failure and clears loading state`, asserting `taskContentGenerationError`, cleared suggestion, and `isGeneratingTaskContent == false`. Story file now includes Tasks, Dev Agent Record, and File List.

### Refactoring performed

None (review-only).

### Compliance check

- Testing Strategy: ✓ Success, failure, and clear paths covered at VM; core service/use case tests present.
- All ACs Met: ✓ Including AC3 (timeouts + tested failure feedback).

### Requirements traceability (follow-up)

| AC | Coverage |
|----|----------|
| 1–5 | Full (AC3 backed by timeouts + failure test) |

### Improvements checklist (delta)

- [x] HttpTimeout on generation `HttpClient`
- [x] Failure-path `TasksViewModelTest`
- [x] Story handoff sections
- [ ] Optional: use case negative tests; OpenAI-path `DefaultTaskContentGenerationServiceTest`

### Gate status (follow-up)

Gate: PASS → [docs/qa/gates/7.2-llm-assisted-generation.yml](docs/qa/gates/7.2-llm-assisted-generation.yml)

### Recommended status (follow-up)

✓ **Ready for Done** from a QA perspective (story owner sets final status).
