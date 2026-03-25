# Story 7.4: Agent Builder and LLM-Powered Agents

**Epic:** Epic 7 - Local AI/ML Integration

**As a** developer,  
**I want** to build and extend agents that leverage local or remote LLMs,  
**so that** I can automate workflows that require AI reasoning within AiTask.

## Status

Done

## Acceptance Criteria

1. The system supports building and extending agents that leverage local or remote LLMs.
2. Agents can be defined with prompt templates, model selection, and invocation triggers.
3. Agent definitions are persisted and associated with projects or globally.
4. Agents can be invoked from the task UI or as part of automation flows.
5. Agent execution outcomes are logged for traceability.

## Requirements Mapping

- AI-4: System must support building and extending agents that leverage local or remote LLMs

## Dependencies

- Story 7.1: Local LLM Configuration

## Architecture References

- [Component Architecture: Integration](../architecture.md)

## UX References

- [Front-end Spec: Automation Center](../front-end-spec.md#14-automation-center)
- [Mockup: Automation Center](../mockups/automation-center.html)

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew :core:test :desktop-app:test`
- `./gradlew :desktop-app:test --tests com.aitask.desktop.ui.viewmodel.TasksViewModelTest.selectTask\\ auto-runs\\ task\\ opened\\ agents --stacktrace`

### Completion Notes List

- Added persisted agent definitions with global or project scope, linked LLM profile selection, prompt templates, and invocation triggers.
- Wired an agent builder card into Settings so agents can be created, edited, enabled, and deleted from the UI.
- Added a task-level agent runner so saved agents can be invoked from the task detail screen and their execution output is surfaced in-app.
- Logged agent execution outcomes to activity history for traceability.
- Wired task-opened and task-updated trigger handling into the task view model so configured agents can auto-run from task lifecycle events.
- Removed direct repository access from the task selection flow so the view model stays on the injected testable repository path.
- Added a task-agent failure-path test alongside the auto-run trigger test.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Activity.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/AgentDefinition.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/repository/AgentDefinitionRepository.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/AgentExecutionService.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/DeleteAgentDefinitionUseCase.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/GetAgentDefinitionsUseCase.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/RunAgentUseCase.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/SaveAgentDefinitionUseCase.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/AgentDefinitionEntity.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/AgentDefinitionRepositoryImpl.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/llm/DefaultAgentExecutionService.kt` (new)
- `taskmanager/core/src/main/resources/db/migration/V16__add_agent_definitions.sql` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/data/repository/AgentDefinitionRepositoryImplTest.kt` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/RunAgentUseCaseTest.kt` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/llm/DefaultAgentExecutionServiceTest.kt` (new)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/PlaceholderViews.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/SettingsViewModel.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt` (modified)
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/SettingsViewModelTest.kt` (modified)
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModelTest.kt` (modified)

### Change Log

| Date | Description |
|------|-------------|
| 2026-03-25 | Implemented persisted LLM-powered agent definitions, agent execution, and task/settings UI wiring. |
| 2026-03-25 | Verified `:core:test` and `:desktop-app:test` after adding repository, use case, and UI coverage for agent builder flows. |
| 2026-03-25 | Closed QA concerns by wiring task lifecycle trigger execution into `TasksViewModel`, removing the direct repository access path, and adding `runAgent` failure coverage. |

## QA Results

### Review Date: 2026-03-25

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

The implementation adds `AgentDefinition` with global/project scope, optional per-agent `llmConfigurationId` (fallback to default profile), `AgentTrigger` (manual / task opened / task updated), prompt templates with `{{taskTitle}}` placeholders, Flyway V16 + `AgentDefinitionRepositoryImpl`, Settings **Agent builder** UI, and task-detail agent run with `RunAgentUseCase` + `DefaultAgentExecutionService` (Ollama/OpenAI-compatible, `HttpTimeout`). `RunAgentUseCase` logs `ActivityType.AGENT_EXECUTED` on both success and failure with metadata (no raw LLM output body). Core tests cover repository (H2), execution service (HTTP), and run use case including activity verification.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓ Domain, data, infrastructure, desktop layers coherent.
- Testing Strategy: ⚠ Strong core coverage; VM tests cover `runAgent` success but not failure; Settings covers blank agent save.
- All ACs Met: ⚠ See traceability; automation trigger gap below.

### Requirements Traceability (summary)

| AC | Coverage | Notes |
|----|----------|-------|
| 1 | Full | Agents use configured `LlmConfiguration` (local or remote-compatible endpoint). |
| 2 | Full | Prompt templates, LLM selection, triggers in model + Settings. |
| 3 | Full | Persistence + global/project scope; `AgentDefinitionRepositoryImplTest`. |
| 4 | Partial | **Manual** invocation from task UI is implemented. **`AgentTrigger.TASK_OPENED` / `TASK_UPDATED` are stored and editable but no code path auto-invokes agents** on task open/update (automation flows not implemented). |
| 5 | Full | `AGENT_EXECUTED` activity with success/failed status and metadata. |

### Improvements Checklist

- [ ] Implement or schedule automation: when `trigger` is TASK_OPENED / TASK_UPDATED, invoke `RunAgentUseCase` from the appropriate task lifecycle hooks (or document as future story).
- [ ] Add `TasksViewModelTest` for `runAgent` **failure** (error message, `isRunningAgent` false).
- [x] `:core:test` and `:desktop-app:test` pass (2026-03-25).

### Security Review

Prompt rendering injects task/project fields into the template; LLM endpoint and API key follow existing LLM configuration patterns. Activity stores output length, not full text.

### Performance Considerations

HttpTimeout on agent execution client matches other LLM services.

### Files Modified During Review

None.

### Gate Status

Gate: CONCERNS → [docs/qa/gates/7.4-agent-builder-llm.yml](docs/qa/gates/7.4-agent-builder-llm.yml)

### Recommended Status

✗ Changes recommended — wire non-manual triggers or document waiver; add optional `runAgent` failure VM test; then re-review for PASS.

---

### Follow-up review date: 2026-03-26

### Reviewed By: Quinn (Test Architect)

### Code quality assessment

Prior gaps are closed: `selectTask` calls `loadAvailableAgents` with `AgentTrigger.TASK_OPENED` and the loaded task; `handleTaskUpdated` (invoked from successful task mutations) calls `loadAvailableAgents` with `TASK_UPDATED`. `loadAvailableAgents` invokes `runTriggeredAgent`, which selects the first enabled agent whose `trigger` matches and calls `runAgent`. `TasksViewModelTest` includes `runAgent surfaces failure and clears loading state` and `selectTask auto-runs task opened agents` with `coVerify` on `runAgentUseCase.invoke`.

### Refactoring performed

None (review-only).

### Compliance check

- Testing Strategy: ✓ Failure path + TASK_OPENED automation covered at VM layer.
- All ACs Met: ✓ Including AC4 (manual + automated trigger paths).

### Requirements traceability (follow-up)

| AC | Coverage |
|----|----------|
| 1–5 | Full |

### Improvements checklist (delta)

- [x] Non-manual trigger wiring
- [x] `runAgent` failure VM test
- [ ] Optional: test `TASK_UPDATED` auto-run; document multi-agent same-trigger behavior

### Gate status (follow-up)

Gate: PASS → [docs/qa/gates/7.4-agent-builder-llm.yml](docs/qa/gates/7.4-agent-builder-llm.yml)

### Recommended status (follow-up)

✓ **Ready for Done** from a QA perspective (story owner sets final status).
