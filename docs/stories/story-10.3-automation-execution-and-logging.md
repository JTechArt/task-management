# Story 10.3: Approve, Modify, and Execute Generated Approach

**Epic:** Epic 10 - AI-Powered Task Automation

**As a** developer,  
**I want** to approve, modify, or reject the generated approach before it runs,  
**so that** I stay in control of what the automation executes.

## Status

Done

## Acceptance Criteria

1. Before execution, the generated approach is shown in a review UI (steps, tools, prompts).
2. A user can approve the approach as-is and start execution.
3. A user can modify the approach (edit steps, remove steps, change tools) before execution.
4. A user can reject the approach and return to manual workflow.
5. Execution only proceeds after explicit user approval.

## Requirements Mapping

- AUTO-4: System should support execution of generated approaches with user approval

## Dependencies

- Story 10.1: Agent Builder and Workflow Configuration
- Story 10.2: AI-Generated Task-Solving Approach

## Architecture References

- [Component Architecture: Integration](../architecture.md)
- [Security Architecture: Credential Management](../architecture.md#credential-management)

## UX References

- [Front-end Spec: Automation Center](../front-end-spec.md#14-automation-center)
- [Mockup: Automation Center](../mockups/automation-center.html)

## Tasks / Subtasks

- [x] Task 1: Implement approach review UI
  - [x] Display generated steps, tools, prompts in review view
  - [x] Approve / Modify / Reject actions
- [x] Task 2: Implement modification capability
  - [x] Edit steps in-place
  - [x] Remove or reorder steps
  - [x] Change tool selection per step
- [x] Task 3: Wire execution trigger to approved approach
  - [x] ExecuteAutomationUseCase invoked only after approval
  - [x] Pass modified approach when user changed it
- [x] Task 4: Add unit tests for approval flow and modification logic

## Dev Agent Record

### Agent Model Used

Cursor agent (Claude)

### Debug Log References

`./gradlew :core:compileKotlin :desktop-app:compileKotlin :core:test :desktop-app:test` — passed

### Completion Notes List

- Extended `RunAgentRequest` with optional `approvedApproach`; `ExecuteAutomationUseCase` typealias points at `RunAgentUseCase`. `RunAgentUseCase` appends the user-approved plan (summary, steps, tools, prompts) to the LLM prompt when present.
- `TasksViewModel.runAgent` blocks when a draft exists for the task and review state is `PENDING_REVIEW` (sets error; does not call the use case). After `APPROVED`, passes current `taskApproachDraft` (including edits) into `RunAgentRequest`.
- Task detail UI: per-step move up/down, remove, and `FilterChip` toggles for Local LLM / Codex / Claude; agent run disabled with helper text while approach is pending review.
- Tests: `RunAgentUseCaseTest` for approved approach in prompt; `TasksViewModelTest` for pending gate, approved payload, and `removeTaskApproachStep`.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/RunAgentUseCase.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt` (modified)
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/RunAgentUseCaseTest.kt` (modified)
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModelTest.kt` (modified)

### Change Log

- 2026-03-31: Story 10.3 — approval gate for agent run, pass approved/edited approach into `RunAgentUseCase`, step reorder/remove/tool chips, tests.
- 2026-03-31: Status set to Done; QA gate PASS and artifacts committed with implementation.

## QA Results

### Review Date: 2026-03-31

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation matches the story: the task detail “Solving approach” card shows summary, per-step title/detail, tool `FilterChip`s, and prompt fields; edits are allowed while review is pending and locked after approval. `TasksViewModel.runAgent` blocks when a draft exists for the task and `taskApproachReviewState` is `PENDING_REVIEW`, and passes `approvedApproach` only when state is `APPROVED` for that task. `RunAgentUseCase` appends the approved plan to the rendered prompt with tools and prompts formatted. `rejectTaskApproach` clears the draft so the user can continue manually without the generated plan.

### Refactoring Performed

None (review only).

### Compliance Check

- Coding Standards: ✓ Kotlin style and explicit types consistent with surrounding code.
- Project Structure: ✓ Changes confined to core use case and desktop task UI/ViewModel as listed in the File List.
- Testing Strategy: ✓ `RunAgentUseCaseTest` covers approved approach in prompt; `TasksViewModelTest` covers pending gate, approved payload, and step removal.
- All ACs Met: ✓ AC1–AC5 verified against code and tests.

### Improvements Checklist

- [x] Verified approval gate and prompt injection with unit tests
- [ ] Optional: document or adjust behavior if product requires that agent runs after approval must only occur via explicit “Run agent” (not `TASK_UPDATED` auto-trigger); see gate recommendations

### Security Review

No new secret storage or logging of credentials. User-edited approach text is included in the LLM prompt as intended for automation; treat like other task context.

### Performance Considerations

No concerns; draft editing is local state until run.

### Files Modified During Review

None.

### Gate Status

Gate: PASS → docs/qa/gates/10.3-approve-modify-and-execute-generated-approach.yml

NFR assessment: docs/qa/assessments/10.3-nfr-20260331.md

### Recommended Status

Done — confirmed 2026-03-31 after QA gate PASS.
