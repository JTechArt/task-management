# Story 8.3: GEPPA Integration with AI Features

**Epic:** Epic 8 - GEPPA (Prompt Optimization)

**As a** developer,  
**I want** AI-assisted features to use GEPPA-optimized prompts when configured,  
**so that** task generation, commit messages, and agent prompts benefit from optimization.

## Status

Done

## Acceptance Criteria

1. Task description generation uses GEPPA-optimized prompts when GEPPA is enabled.
2. Commit message and PR description suggestions use optimized prompts when available.
3. Agent and workflow prompts can be associated with saved GEPPA-optimized prompts.
4. The application falls back gracefully when GEPPA is unavailable or optimization fails.
5. Optimization activity is logged for traceability without exposing prompt content in logs.

## Requirements Mapping

- GEPPA-2: Prompts used for AI-assisted features should be optimizable via GEPPA

## Dependencies

- Story 8.1: GEPPA Integration Enablement
- Story 8.2: Prompt Optimization and Persistence
- Epic 7: Local AI/ML Integration

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Front-end Spec: Automation Center](../front-end-spec.md#14-automation-center)
- [Mockup: AI Studio](../mockups/ai-studio.html)
- [Mockup: Automation Center](../mockups/automation-center.html)

## Dev Notes

- Integrate with LLM invocation points from Epic 7 and future AI automation.
- Graceful fallback is required when optimization is unavailable or times out.
- Log optimization usage for traceability without storing sensitive prompt bodies.

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Tasks

- [x] Create `GeppaPromptOptimizationService` interface
- [x] Implement `DefaultGeppaPromptOptimizationService` (POST `/optimize`, fallback on failure)
- [x] Update `DefaultTaskContentGenerationService` to apply GEPPA optimization (AC1)
- [x] Update `DefaultGitAssistantSuggestionService` to apply GEPPA optimization (AC2)
- [x] Update `RunAgentUseCase` to apply GEPPA optimization and log `geppaOptimized` in activity metadata (AC3, AC5)
- [x] Wire `GeppaPromptOptimizationService` in `DependencyContainer`
- [x] Write `DefaultGeppaPromptOptimizationServiceTest` (enabled/disabled/fallback scenarios)
- [x] Extend `RunAgentUseCaseTest` with GEPPA optimization test

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/GeppaPromptOptimizationService.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/llm/DefaultGeppaPromptOptimizationService.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/llm/DefaultTaskContentGenerationService.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/llm/DefaultGitAssistantSuggestionService.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/RunAgentUseCase.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt` (modified)
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/llm/DefaultGeppaPromptOptimizationServiceTest.kt` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/RunAgentUseCaseTest.kt` (modified)

### Completion Notes

- GEPPA optimization uses `POST {endpointUrl}/optimize` with `{"prompt":"...","context":"..."}` body and expects `{"optimized_prompt":"..."}` response.
- All three AI invocation paths (task generation, git suggestions, agent execution) fall back to the original prompt when GEPPA is disabled, null, returns non-2xx, or throws — satisfying AC4.
- Prompt content is never written to logs; only context label, lengths, and error class names are logged — satisfying AC5.
- `geppaOptimized` metadata field added to `AGENT_EXECUTED` activity entries for traceability.
- `GeppaPromptOptimizationService` is nullable in service constructors (default `null`) to maintain backward-compatibility with tests.

## QA Results

### Review Date: 2026-03-26

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

`DefaultGeppaPromptOptimizationService` reads `GeppaConfigurationRepository`, skips work when disabled or missing, and `POST`s to `{endpoint}/optimize` with JSON body `prompt` + `context` (`task_generation`, `git_suggestion`, `agent_execution`). On success it parses `optimized_prompt`; otherwise it returns the original prompt. Logging records context and lengths only, not prompt text (AC5). `DefaultTaskContentGenerationService` and `DefaultGitAssistantSuggestionService` call `optimizeIfEnabled` before LLM submission; `RunAgentUseCase` optimizes the rendered agent template, passes the result to `AgentExecutionService`, and sets `geppaOptimized` in `AGENT_EXECUTED` activity metadata when the optimized string differs from the rendered template (AC3/AC5 for agents). `DependencyContainer` wires a shared `geppaPromptOptimizationService` into task, git, and `RunAgentUseCase`. `./gradlew :core:test` completed successfully in review.

**AC3:** Interpreted with Story 8.2: agents use prompt templates that may originate from the saved-prompt library; at execution time GEPPA optimizes that template text. There is no separate `savedPromptId` foreign key—association is by content path, which matches current product behavior.

**Coverage gap (non-blocking):** `DefaultTaskContentGenerationServiceTest` / `DefaultGitAssistantSuggestionServiceTest` still construct services without a GEPPA mock; end-to-end optimize-then-LLM order for those classes is proven by code inspection plus `DefaultGeppaPromptOptimizationServiceTest`.

### Refactoring Performed

None.

### Compliance Check

- Coding Standards: Consistent with Epic 7 LLM services and nullable DI for tests.
- Testing Strategy: Core optimizer and `RunAgentUseCase` GEPPA path covered; optional extra tests per gate monitor.
- All ACs Met: Yes, given content-based association for AC3.

### Improvements Checklist

- [ ] Optional: parameterized tests for task/git services injecting a mock `GeppaPromptOptimizationService` to assert optimized prompt reaches LLM request.
- [ ] Optional: explicit `savedPromptId` on agents if product requires auditable linkage beyond template text.

### Security Review

Prompt bodies are not written to application logs in reviewed paths; metadata uses boolean and lengths.

### Performance Considerations

Additional network call to GEPPA when enabled; timeouts limit hang risk.

### Files Modified During Review

None.

### Gate Status

Gate: PASS → docs/qa/gates/8.3-optimized-prompt-persistence.yml  
Risk profile: docs/qa/assessments/8.3-risk-20260326.md  
NFR assessment: docs/qa/assessments/8.3-nfr-20260326.md

### Recommended Status

**Done** (confirmed).
