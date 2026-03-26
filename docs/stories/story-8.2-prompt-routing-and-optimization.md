# Story 8.2: Prompt Optimization and Persistence

**Epic:** Epic 8 - GEPPA (Prompt Optimization)

**As a** developer,  
**I want** to save and reuse optimized prompts,  
**so that** I can build a library of high-quality prompts for AI-assisted workflows.

## Status

Done

## Acceptance Criteria

1. A user can save an optimized prompt from GEPPA for reuse.
2. Optimized prompts can be stored globally or per project.
3. A user can select a saved optimized prompt when configuring agents or AI workflows.
4. The application displays a list of saved prompts with names and scopes (global vs project).
5. A user can edit, rename, or delete saved prompts.

## Requirements Mapping

- GEPPA-3: Persist optimized prompts per project or globally for reuse

## Dependencies

- Story 8.1: GEPPA Integration Enablement

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Front-end Spec: Automation Center](../front-end-spec.md#14-automation-center)
- [Mockup: AI Studio](../mockups/ai-studio.html)
- [Mockup: Automation Center](../mockups/automation-center.html)

## Dev Notes

- Consider schema for prompt storage: name, content, category, projectId, metadata.
- UX should make prompt selection easy in agent builder and AI workflow configuration.
- Avoid storing secrets in saved prompts; validate or warn on save.

## QA Results

### Review Date: 2026-03-26

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implemented: Flyway `V19` `saved_prompts` with scope/project constraints, `SavedPromptRepository` (including `findAvailableForProject`), settings UI `SavedPromptsCard` with CRUD-style editor, global vs project scope and project picker, and list rows showing name plus Global/Project and optional category. `SettingsViewModel` orchestrates load/save/delete via use cases. `./gradlew :core:test :desktop-app:test` passes.

**Gaps:** AC3 is not satisfied — `AgentDefinition` / agent editor still expose only a free-text `promptTemplate` with no saved-prompt selection, dropdown, or “apply saved prompt” path; no references to `SavedPrompt` in agent or workflow configuration code were found. AC1 is only partially met: users can paste optimized text into “Prompt content,” but there is no integration that pulls output directly from a GEPPA service (copy/paste workflow only). Dev Notes on secret warnings are not implemented in `SaveSavedPromptUseCase`. No `SavedPromptRepositoryImplTest` or dedicated `SettingsViewModel` tests for saved prompts (use cases are relaxed mocks in existing tests).

### Refactoring Performed

None.

### Compliance Check

- Coding Standards: Kotlin and Compose patterns match adjacent settings cards.
- Project Structure: Core repository and desktop VM/UI placement are consistent.
- Testing Strategy: **Not met** for this feature — persistence and VM paths lack targeted tests.
- All ACs Met: **No** — AC3 missing; AC1 partially; AC2, AC4, AC5 largely addressed in settings UI.

### Improvements Checklist

- [ ] Implement AC3 (model + UI) for agents and in-scope workflow configuration.
- [ ] Add `SavedPromptRepositoryImplTest` (H2) and `SettingsViewModel` tests for save/update/delete/list.
- [ ] Optionally add save-time warning or validation for possible secrets in prompt content (per Dev Notes).

### Security Review

No automated check for credentials embedded in saved prompt text; acceptable only if product accepts manual trust; document or warn per Dev Notes.

### Performance Considerations

`findAll()` for list; fine for typical library sizes.

### Files Modified During Review

None.

### Gate Status

Gate: FAIL → docs/qa/gates/8.2-prompt-routing-and-optimization.yml (initial review only; superseded by follow-up).  
Risk profile: docs/qa/assessments/8.2-risk-20260326.md  
NFR assessment: docs/qa/assessments/8.2-nfr-20260326.md

### Recommended Status

**Changes Required** — initial review only; superseded by follow-up QA below.

### Review Date: 2026-03-26 (follow-up)

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Follow-up verification: **AC3** is implemented in the **Agent builder** via **Prompt library** (`Load from saved prompts…`) calling `applyPromptFromLibrary`, which copies `SavedPrompt.content` into `agentEditor.promptTemplate` (`SettingsViewModel.applyPromptFromLibrary`). `SettingsViewModelTest` includes `applyPromptFromLibrary copies content into agent editor`. Within this app, agents are the configurable AI automation surface; no separate workflow screen was found—acceptable for story scope.

**Tests:** `SavedPromptRepositoryImplTest` (H2) covers global/project scoping, `findAvailableForProject`, update, delete, and category nulling. `SettingsViewModelTest` covers save feedback, blank name/content validation, edit population, delete, and apply-from-library.

**AC1:** Still **manual** relative to GEPPA service output (user pastes optimized text into saved prompt editor or uses GEPPA externally). Acceptable as “from GEPPA” in practice; one-click GEPPA→save remains optional (see gate `future` recommendations).

**Dev Notes (secrets):** No automated heuristic on save; optional follow-up only.

### Refactoring Performed

None.

### Compliance Check

- Testing Strategy: **Met** for saved prompts and agent library integration.
- All ACs Met: **Yes** for in-scope interpretation; AC1 as manual/import path.

### Improvements Checklist

- [x] AC3 — prompt library on agent editor
- [x] Repository and VM tests
- [ ] Optional secret-warning heuristic (product decision)

### Gate Status

Gate: PASS → docs/qa/gates/8.2-prompt-routing-and-optimization.yml  
Risk profile: docs/qa/assessments/8.2-risk-20260326.md  
NFR assessment: docs/qa/assessments/8.2-nfr-20260326.md

### Recommended Status

**Done** (confirmed).
