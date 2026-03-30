# Story 10.4: Automation Run Logging and Traceability

**Epic:** Epic 10 - AI-Powered Task Automation

**As a** developer,  
**I want** automation runs to be logged with input task, generated plan, and result status,  
**so that** I can audit and improve my automated workflows over time.

## Status

Done

## Acceptance Criteria

1. Each automation run records the input task, generated plan (or approved modification), and result status.
2. Run history is accessible from the task, project, or a dedicated automation history view.
3. Failed runs record error information for debugging.
4. Logs do not expose secrets, tokens, or sensitive prompt content.
5. A user can filter or search run history by task, project, status, or date.

## Requirements Mapping

- AUTO-5: System should log automation runs and outcomes for traceability

## Dependencies

- Story 10.1: Agent Builder and Workflow Configuration
- Story 10.2: AI-Generated Task-Solving Approach
- Story 10.3: Approve, Modify, and Execute Generated Approach

## Architecture References

- [Component Architecture: Integration](../architecture.md)

## UX References

- [Front-end Spec: Automation Center](../front-end-spec.md#14-automation-center)
- [Mockup: Automation Center](../mockups/automation-center.html)

## Tasks / Subtasks

- [x] Task 1: Enrich automation run activity metadata (task, plan summary, step titles, result, errors)
- [x] Task 2: Safe logging (sanitize errors; no full prompts or LLM output in logs)
- [x] Task 3: Repository and Activity view filters (project, task, type, status, date range, search)
- [x] Task 4: Task detail and project entry points to run history + Activity navigation
- [x] Task 5: Unit tests (sanitizer, RunAgent metadata, ActivityViewModel mocks)
- [x] Task 6 (QA): Failure-path `RunAgentUseCaseTest` for FAILED activity + sanitized `errorMessage` (gate TEST-001)

## Dev Agent Record

### Agent Model Used

Cursor agent (Claude)

### Debug Log References

`./gradlew :core:compileKotlin :desktop-app:compileKotlin :core:test :desktop-app:test` — passed (re-run after QA fix)

### Completion Notes List

- Extended `RunAgentUseCase` activity rows for `AGENT_EXECUTED` with `taskTitle`, `inputTaskId`, plan trace (`hasUserApprovedPlan`, truncated `approachSummary`, `approachStepTitles`, `approachStepCount`), `resultStatus`, `outputLength` on success, sanitized `errorMessage` on failure; description line names automation run + status.
- Added `AutomationRunLogSanitizer` for error redaction and truncation (no Bearer/sk-/long-hex patterns; no prompt text stored).
- Extended `ActivityRepository.findFiltered` with `status`, `createdAfterInclusive`, `createdBeforeExclusive`; implemented in SQL and in-memory repos.
- `ActivityViewModel` / `ActivityView`: status + date range + search; expandable automation rows with metadata; `applyFilters` + `pendingFilters` for navigation from Tasks/Projects.
- `TasksViewModel` loads `taskAutomationRuns` on task select and after agent run; `TaskDetailView` shows compact history + “Open full history (Activity)”.
- `ProjectDetailView`: “View automation run history” → Activity with project + `AGENT_EXECUTED` filter. `TaskManagerApp` wires `pendingActivityFilters` for cross-nav.
- QA gate TEST-001: Added `invoke logs failed activity with sanitized errorMessage when execution fails` in `RunAgentUseCaseTest` (`ActivityStatus.FAILED`, no raw token in `errorMessage`, `[REDACTED]` present). Gate file updated to PASS.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/AutomationRunLogSanitizer.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/RunAgentUseCase.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/repository/ActivityRepository.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/ActivityRepositoryImpl.kt` (modified)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/InMemoryActivityRepository.kt` (modified)
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/service/AutomationRunLogSanitizerTest.kt` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/RunAgentUseCaseTest.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ActivityViewModel.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/activity/ActivityView.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectComponents.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectsView.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/TaskManagerApp.kt` (modified)
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/ActivityViewModelTest.kt` (modified)

### Change Log

- 2026-03-31: Story 10.4 — automation run trace metadata, sanitizer, Activity filters/search, task and project history entry points, tests.
- 2026-03-31: QA apply-fixes — `RunAgentUseCaseTest` failure-path logging; `docs/qa/gates/10.4-automation-run-logging.yml` → PASS.

## QA Results

### Review Date: 2026-03-31

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation aligns with the acceptance criteria: `RunAgentUseCase` enriches `AGENT_EXECUTED` activity metadata with task identity, plan trace (approved summary and step titles, truncated), result status, output length on success, and sanitized errors on failure. `ActivityRepository.findFiltered` supports project, task, type, status, and date range; `ActivityViewModel` applies filters and client-side search over descriptions and metadata values. Task detail and project views navigate to Activity with pending filters for automation runs. `./gradlew :core:test :desktop-app:test` completed successfully in this review.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓ Kotlin style and explicit types match project conventions.
- Project Structure: ✓ Core domain sanitizer and repositories; desktop view models and views as documented in File List.
- Testing Strategy: ⚠ Sanitizer and ViewModel paths covered; failure logging in `RunAgentUseCase` not asserted in use case tests (see Improvements).
- All ACs Met: ✓ By code inspection and passing tests.

### Improvements Checklist

- [ ] Add `RunAgentUseCaseTest` case for execution failure verifying `ActivityStatus.FAILED` and sanitized `errorMessage` in metadata (closes TEST-001 in gate).

### Security Review

Logging avoids storing full prompts or model output in activity metadata; approach text is truncated; errors pass through `AutomationRunLogSanitizer`. No issues blocking release for this story.

### Performance Considerations

Repository queries use indexed filters with a limit; search is applied to the loaded window—acceptable for current scale.

### Files Modified During Review

None.

### Gate Status

Gate: CONCERNS → docs/qa/gates/10.4-automation-run-logging.yml

### Recommended Status

Ready for Done — optional follow-up test recommended, not required to merge if team accepts the CONCERNS item as a fast-follow.

### Review Date: 2026-03-31 (follow-up)

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Re-reviewed after QA fix: `RunAgentUseCaseTest` includes `invoke logs failed activity with sanitized errorMessage when execution fails`, which verifies `AGENT_EXECUTED` with `ActivityStatus.FAILED`, `resultStatus` metadata, and `errorMessage` that redacts a Bearer fragment (`[REDACTED]`, raw token not present). This closes prior gate **TEST-001**. `./gradlew :core:test :desktop-app:test` passed in this review.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ✓ Failure path covered at use case level alongside sanitizer tests.
- All ACs Met: ✓

### Improvements Checklist

- [x] `RunAgentUseCaseTest` failure path for FAILED activity + sanitized `errorMessage` (gate TEST-001)

### Security Review

No change from prior review; failure-path test reinforces that bearer-like material does not reach stored `errorMessage` text.

### Gate Status

Gate: PASS → docs/qa/gates/10.4-automation-run-logging.yml

### Recommended Status

✓ Ready for Done
