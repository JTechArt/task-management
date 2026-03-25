# Story 7.3: LLM-Assisted Git Operations (Commits, PRs, Comments)

**Epic:** Epic 7 - Local AI/ML Integration

**As a** developer with Git and optional external service integrations,  
**I want** the LLM to suggest commit messages, PR descriptions, or comments,  
**so that** I can maintain consistent, descriptive Git metadata with less manual writing.

## Status

Done

## Acceptance Criteria

1. With Git integration enabled, the system can suggest commit messages based on staged changes or task context.
2. When integrated with external services (e.g., GitHub PR API), the system can generate PR descriptions and comment drafts.
3. Suggestions are clearly labeled as AI-generated and require user approval before use.
4. The feature gracefully degrades when LLM is unavailable or integration is not configured.
5. Activity or history records indicate when LLM suggestions were used.

## Requirements Mapping

- AI-3: LLM-assisted operations (commit message suggestions, PR description generation, comment drafting)

## Dependencies

- Story 7.1: Local LLM Configuration
- Epic 2: Git integration
- Epic 4: External integrations (OAuth for PR APIs)

## Architecture References

- [Component Architecture: Git Integration](../architecture.md)
- [Component Architecture: Integration](../architecture.md)

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Mockup: AI Studio](../mockups/ai-studio.html)

## Tasks / Subtasks

- [x] Add core git-assist generation model, service, and use case
- [x] Wire git-assist suggestions into task detail UI
- [x] Add activity logging for accepted AI suggestions
- [x] Add success and failure tests for the core service, use case, and view-model
- [x] Run `:core:test` and `:desktop-app:test`

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew :core:test :desktop-app:test`
- `./gradlew :core:test --tests com.aitask.core.infrastructure.git.JGitServiceTest`

### Completion Notes List

- Added a git-assist generation pipeline that drafts commit messages, PR descriptions, and comment text from task and repository context.
- Threaded staged Git context from the workspace into commit-message generation so suggestions are grounded in actual staged changes when available.
- Kept suggestions clearly labeled as AI-generated and required explicit user approval before copying or using them.
- Added activity logging when a suggestion is accepted so the dashboard history can surface AI-assisted usage.
- Added HTTP timeouts to the new git-assist LLM client to keep slow local endpoints from blocking the UI indefinitely.
- Tightened the acceptance test for accepted suggestions to verify the `LLM_SUGGESTION_USED` activity type and its metadata.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Activity.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/GitAssistantSuggestion.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/GitAssistantSuggestionService.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/GitService.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/GenerateGitAssistantSuggestionUseCase.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/git/JGitService.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/llm/DefaultGitAssistantSuggestionService.kt` (new)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt` (modified)
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/git/JGitServiceTest.kt` (modified)
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/llm/DefaultGitAssistantSuggestionServiceTest.kt` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/GenerateGitAssistantSuggestionUseCaseTest.kt` (new)
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModelTest.kt` (modified)

### Change Log

| Date | Description |
|------|-------------|
| 2026-03-25 | Implemented local LLM-assisted git metadata generation for commit messages, PR descriptions, and comment drafts. |
| 2026-03-25 | Added activity logging and success/failure test coverage, then verified `:core:test` and `:desktop-app:test`. |
| 2026-03-25 | Added staged Git context to commit-message generation and tightened the accepted-suggestion activity test, then re-verified `:core:test` and `:desktop-app:test`. |

## QA Results

### Review Date: 2026-03-25

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

`GenerateGitAssistantSuggestionUseCase` loads task, project, linked repository names, and default LLM credentials, then delegates to `DefaultGitAssistantSuggestionService` with `HttpTimeout` (3s / 20s) and Ollama/OpenAI-compatible posting. Task detail UI exposes a **Git assistant** section with three modes, “AI-generated …” labeling, copy-to-clipboard as the explicit “use” action, discard, and error text on failure. `markGitAssistantSuggestionUsed` persists `ActivityType.LLM_SUGGESTION_USED` with mode metadata and branch name (no raw suggestion body in the log). Tests cover service (HTTP), use case wiring, VM success/failure, activity `create` on accept, and `coVerify` on the repository.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ✓ Core + VM paths; activity recording verified via mock.
- All ACs Met: ⚠ See traceability; one scope nuance vs strict AC wording.

### Requirements Traceability (summary)

| AC | Coverage | Notes |
|----|----------|-------|
| 1 | Partial | Commit messages are suggested from **task context** (title, description, branch, repo names). **Staged `git diff` content is not read or sent to the LLM.** |
| 2 | Partial | PR description and comment **drafts** are generated locally; there is **no GitHub (or other) PR API** call to open PRs or post comments—users copy text out. |
| 3 | Full | AI labeling, copy vs discard, no auto-commit. |
| 4 | Full | Errors from missing default LLM / failures; failure VM test; timeouts on client. |
| 5 | Full | `LLM_SUGGESTION_USED` activity with mode metadata. |

### Improvements Checklist

- [ ] Optional follow-up: ingest `git diff --staged` (or similar) when workspace/git integration allows, to tighten AC1.
- [ ] Optional follow-up: integrate hosted PR APIs when OAuth/epic 4 flows require automated submit (AC2).
- [ ] Optional: assert `ActivityType.LLM_SUGGESTION_USED` in `markGitAssistantSuggestionUsed` test (currently `create(any())`).
- [x] `:core:test` and `:desktop-app:test` pass (2026-03-25).

### Security Review

Suggestions are generated from task metadata and repo names; API keys follow existing LLM path. Activity metadata stores suggestion length, not full text.

### Performance Considerations

HttpTimeout aligned with other LLM clients; sequential endpoint candidates.

### Files Modified During Review

None.

### Gate Status

Gate: CONCERNS → [docs/qa/gates/7.3-llm-assisted-git-operations.yml](docs/qa/gates/7.3-llm-assisted-git-operations.yml)

### Recommended Status

✗ Changes recommended — confirm product acceptance of task-context-only + clipboard drafts (or extend scope); then re-review or waive REQ gaps in gate.

---

### Follow-up review date: 2026-03-25

### Reviewed By: Quinn (Test Architect)

### Code quality assessment

Follow-up addresses prior gaps: `GenerateGitAssistantSuggestionUseCase` calls `gitService.getStagedChangesSummary(task.workspacePath)` when a workspace path exists and passes `stagedChangesSummary` into `GitAssistantSuggestionRequest`; `DefaultGitAssistantSuggestionService` includes staged context in the commit-message prompt. `JGitService` / `JGitServiceTest` cover staged summary behavior. `TasksViewModelTest.markGitAssistantSuggestionUsed` now captures the activity and asserts `ActivityType.LLM_SUGGESTION_USED`, entity ids, and metadata keys including `mode`, `taskTitle`, `suggestionLength`, and `branchName`.

### Refactoring performed

None (review-only).

### Compliance check

- Testing Strategy: ✓ Staged path asserted in use case and service tests; activity assertions tightened.
- All ACs Met: ✓ AC1 satisfied when workspace is prepared (staged summary); otherwise task-only context. AC2: local generation of PR/comment drafts (no OAuth PR API — acceptable as MVP per REQ-003).

### Requirements traceability (follow-up)

| AC | Coverage |
|----|----------|
| 1 | Full (staged summary via Git when `workspacePath` + repo available) |
| 2 | Full (draft generation; external API out of scope — see gate note) |
| 3–5 | Full |

### Improvements checklist (delta)

- [x] Staged git context in commit flow
- [x] Strong activity assertions in VM test
- [ ] Optional future: hosted PR API automation

### Gate status (follow-up)

Gate: PASS → [docs/qa/gates/7.3-llm-assisted-git-operations.yml](docs/qa/gates/7.3-llm-assisted-git-operations.yml)

### Recommended status (follow-up)

✓ **Ready for Done** from a QA perspective (story owner sets final status).
