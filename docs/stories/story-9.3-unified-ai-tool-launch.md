# Story 9.3: Unified AI Tool Launch Experience

**Epic:** Epic 9 - AI Tools Integration (Codex, Claude)

**As a** developer,  
**I want** a unified way to launch configured AI tools from the task UI,  
**so that** I can quickly choose which AI assistant to use without navigating multiple settings.

## Status

Done

## Tasks / Subtasks

- [x] Group Codex and Claude launch actions under one **AI tools** section on the task detail workspace panel
- [x] Use a consistent layout: shared heading and helper text; side-by-side buttons when both tools are enabled; single full-width button when one is enabled
- [x] When neither tool is configured, show explicit hint to Settings → AI Studio (instead of silent empty UI)
- [x] Keep existing `TasksViewModel` wiring (`isCodexIntegrationEnabled` / `isClaudeIntegrationEnabled`, `runCodex` / `runClaude`); pre-run behavior unchanged (use cases)
- [x] Run `./gradlew test` (taskmanager)

## Acceptance Criteria

1. The task detail screen includes actions (e.g., "Run with Codex", "Run with Claude") when the respective tools are configured.
2. AI tool actions are grouped or presented in a consistent pattern (e.g., toolbar, dropdown, or action menu).
3. Disabled or unconfigured tools are hidden or clearly indicated as unavailable.
4. The user can invoke multiple AI tools from the same task without re-navigating.
5. Launch actions respect pre-run script execution when configured (Epic 5).

## Requirements Mapping

- AITOOL-4 (Should Have): Unified launch for configured AI tools from task UI

## Dependencies

- Story 9.1: Codex CLI Integration
- Story 9.2: Claude Integration
- Epic 5: Pre-Run Scripts (optional gating)

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Mockup: AI Studio](../mockups/ai-studio.html)
- [Mockup: Tasks](../mockups/tasks.html)

## Dev Notes

- Reuse patterns from IDE launch (Story 1.5) for consistency.
- Consider extensibility for future AI tools (e.g., GitHub Copilot CLI).
- Ensure UI scales when more tools are added.

## Testing

- `./gradlew test` from `taskmanager/` — all modules pass after UI refactor.

## Dev Agent Record

### Agent Model Used

Cursor agent (GPT-5.1)

### Completion Notes

- Introduced `UnifiedAiToolsSection` in `TaskComponents.kt`: single **AI tools** heading, helper text referencing pre-run scripts and multi-tool use on the same screen, paired buttons when both integrations are enabled, and a configuration hint when neither Codex nor Claude is enabled.
- `TaskDetailView` delegates workspace AI actions to `UnifiedAiToolsSection`; `TasksView` / `TasksViewModel` callbacks unchanged.

### Debug Log References

- `./gradlew test` (taskmanager) — success

### File List

- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt`

### Change Log

- 2026-03-30: Story 9.3 — unified AI tool launch section on task detail.
- 2026-03-30: Story marked Done; QA gate PASS (`docs/qa/gates/9.3-unified-ai-tool-launch.yml`).

## QA Results

### Review Date: 2026-03-30

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

`UnifiedAiToolsSection` centralizes AI launch UI: **AI tools** heading, helper text describing pre-run scripts and multi-tool use on the same screen, `Row` with weighted buttons when both `onRunCodex` and `onRunClaude` are non-null, full-width single tool when only one is enabled, and explicit copy directing users to **Settings → AI Studio** when neither integration is enabled (callbacks null). `TaskDetailView` embeds this section below IDE launch; `TasksView` still gates callbacks with `isCodexIntegrationEnabled` / `isClaudeIntegrationEnabled`. Duplicate standalone Codex/Claude blocks are consolidated into this composable. `./gradlew test` from `taskmanager/` completed successfully during this review.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓ Matches existing Compose/Material patterns in `TaskComponents.kt`.
- Project Structure: ✓ Change scoped to desktop task UI.
- Testing Strategy: ✓ No new business logic; existing VM/use case tests cover launches. Optional Compose UI tests for layout states (nice-to-have).
- All ACs Met: ✓ AC1–4 reflected in composable branches; AC5 satisfied by unchanged use cases (pre-run before Codex/Claude launch).

### Improvements Checklist

- [ ] Optional: Compose UI or screenshot tests for three states (both tools, one tool, neither configured).

### Security Review

No new attack surface; no credential handling added in this story.

### Performance Considerations

Negligible; standard Column/Row layout.

### Files Modified During Review

None.

### Gate Status

Gate: PASS → docs/qa/gates/9.3-unified-ai-tool-launch.yml  
Risk profile: docs/qa/assessments/9.3-risk-20260330.md  
NFR assessment: docs/qa/assessments/9.3-nfr-20260330.md

### Recommended Status

✓ **Ready for Done** — PO may set story Status to Done after confirmation.
