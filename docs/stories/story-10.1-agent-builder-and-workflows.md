# Story 10.1: Agent Builder and Custom Workflow Configuration

**Epic:** Epic 10 - AI-Powered Task Automation

**As a** developer,  
**I want** to define custom agents and workflows for task automation,  
**so that** I can automate recurring or AI-assistable tasks in ways that match my project and methodology.

## Status

Done

## Acceptance Criteria

1. A user can create, edit, and delete custom agent definitions within the agent builder.
2. An agent definition includes a name, description, and association with one or more AI tools (local LLM, Codex, Claude).
3. A user can associate agents with projects or task types.
4. Agent definitions are persisted and available across application sessions.
5. The agent builder UI surfaces in project or task configuration where methodology or automation is configurable.

## Architecture References

- [Component Architecture: AI/ML Integration](../architecture.md) *(to be added)*
- [Component Architecture: Rule Management](../architecture.md#5-rule-management-component)

## UX References

- [Front-end Spec: Automation Center](../front-end-spec.md#14-automation-center)
- [Mockup: Automation Center](../mockups/automation-center.html)

## Requirements Mapping

- PRE-5.1: Agent builder for custom automation workflows
- PRE-5.2: Agent association with projects or task types

## Tasks / Subtasks

- [x] Task 1: Add agent definition domain models and persistence
  - [x] Create AgentDefinition, AutomationWorkflow domain models
  - [x] Add database migration for agent_definitions table
  - [x] Implement AgentDefinitionRepository
- [x] Task 2: Implement agent builder use cases
  - [x] CreateAgentDefinitionUseCase
  - [x] UpdateAgentDefinitionUseCase
  - [x] DeleteAgentDefinitionUseCase
  - [x] ListAgentDefinitionsUseCase
- [x] Task 3: Add agent builder UI
  - [x] AgentBuilderView component
  - [x] Agent definition form (name, description, tool associations)
  - [x] Project/task type association controls
- [x] Task 4: Add unit tests for agent definition use cases and repositories

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

- `./gradlew :core:test :desktop-app:test` — exit code 0

### Completion Notes List

- Extended domain with `AgentToolKind`, `AutomationWorkflow`, `AgentToolKindCodec`, and `associated_tools` column (V22 migration).
- Agent builder card in Settings: checkboxes for Local LLM, Codex, Claude; list rows show tool associations.
- Projects → project detail: **Automation** section lists agents available for the selected project (global + project-scoped).
- `RunAgentUseCase` requires Local LLM in `associatedTools` for the current executor; metadata logs associated tools.
- `ListAgentDefinitionsUseCase` typealias for `GetAgentDefinitionsUseCase`. Create/update use cases are `SaveAgentDefinitionUseCase` (single save for create/update).
- Added `AgentDefinitionUseCasesTest` and `RunAgentUseCase` test for non–Local LLM agents.
- **QA gate fix (REQ-001):** Optional `taskTypeFilter` on `AgentDefinition` (V23 migration), Settings dropdown (all types vs specific `TaskType`), `GetAgentDefinitionsUseCase(projectId, taskType)` filtering for task view, `RunAgentUseCase` rejects run when task type does not match filter. Gate `10.1-agent-builder-and-workflows.yml` set to **PASS**.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/AgentDefinition.kt`
- `taskmanager/core/src/main/resources/db/migration/V22__agent_definition_associated_tools.sql`
- `taskmanager/core/src/main/resources/db/migration/V23__agent_definition_task_type_filter.sql`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/repository/AgentDefinitionRepository.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/AgentDefinitionEntity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/AgentDefinitionRepositoryImpl.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/RunAgentUseCase.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/GetAgentDefinitionsUseCase.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/AgentDefinitionUseCasesTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/RunAgentUseCaseTest.kt`
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModelTest.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/SettingsViewModel.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/PlaceholderViews.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ProjectsViewModel.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectComponents.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectsView.kt`
- `docs/qa/gates/10.1-agent-builder-and-workflows.yml`

### Change Log

| Date | Change |
|------|--------|
| *(Initial story creation)* | Story created from features-v2.md |
| 2026-03-30 | Story 10.1: agent tool associations, AutomationWorkflow model, project automation section, tests |
| 2026-03-31 | Task type filter for agents (AC3 / QA REQ-001); gate 10.1 → PASS |
| 2026-03-31 | Status set to Done after QA gate PASS |

## QA Results

### Review Date: 2026-03-31

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation is coherent: domain validation on `AgentDefinition` / `AgentDefinitionRequest`, repository persistence with `associated_tools`, and Settings-based CRUD with tool checkboxes and project scope. Projects detail shows a read-only **Automation** section listing agents for the selected project. `./gradlew :core:test :desktop-app:test` completed successfully (exit code 0).

### Refactoring Performed

None during review.

### Compliance Check

- Coding Standards: ✓ (Kotlin patterns match surrounding modules; types explicit; no `any`)
- Project Structure: ✓ (core domain/data, desktop UI/viewmodels as expected)
- Testing Strategy: ⚠ (unit tests for use cases; no Compose/UI tests for agent builder)
- All ACs Met: ⚠ (see below)

### Acceptance Criteria Coverage

| AC | Assessment |
|----|------------|
| 1 | **Met** — Create/edit/delete via Settings **Agent builder** (`AgentDefinitionCard`, `saveAgentDefinition` / `deleteAgentDefinition`). |
| 2 | **Met** — Name, description, prompt template, LLM profile, and **AI tools** (`AgentToolKind`: Local LLM, Codex, Claude; persisted in `associated_tools`). |
| 3 | **Partial** — **Project** association via `AgentScope` and `projectId` is implemented. **Task types** (e.g. Bug vs Feature) are not modeled on `AgentDefinition` or in the UI; triggers (`MANUAL`, `TASK_OPENED`, `TASK_UPDATED`) cover lifecycle, not task taxonomy. |
| 4 | **Met** — `agent_definitions` table and repository; definitions loaded on startup in Settings. |
| 5 | **Met** — Agent builder in **Settings** (automation configuration); **Projects** detail includes **Automation** next to methodology-related content; **Tasks** expose agent **runner** (execution) for task context. |

### Improvements Checklist

- [ ] Clarify or implement **task-type** association for AC3 (domain + UI), or revise AC3 with PO/SM.
- [ ] Optional: add UI or integration tests for the agent builder card.

### Security Review

No hardcoded secrets in reviewed paths. Tool associations are stored as comma-separated enum names; local DB tampering could yield ignored tokens (defaults to Local LLM) — acceptable for local desktop scope.

### Performance Considerations

None blocking.

### Files Modified During Review

None (QA artifacts only: gate and assessments under `docs/qa/`).

### Gate Status

Gate: CONCERNS → docs/qa/gates/10.1-agent-builder-and-workflows.yml

Risk profile: docs/qa/assessments/10.1-risk-20260331.md

NFR assessment: docs/qa/assessments/10.1-nfr-20260331.md

### Recommended Status

✗ Changes Required — Resolve AC3 task-type scope (implementation or product sign-off on partial coverage), then re-review or mark waived in gate.

### Review Date: 2026-03-31 — Follow-up (post REQ-001)

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Follow-up review after implementation of optional `taskTypeFilter` on `AgentDefinition` (migration `V23__agent_definition_task_type_filter.sql`), Settings **Task type filter** dropdown (`PlaceholderViews` / `SettingsViewModel`), repository filtering in `findAvailableForProject(projectId, taskType)`, `TasksViewModel` passing current task type into `GetAgentDefinitionsUseCase`, and `RunAgentUseCase` rejecting runs when the agent filter does not match the task. Project Automation cards display filter labels. `./gradlew :core:test :desktop-app:test` completed successfully (exit code 0).

### Refactoring Performed

None during review.

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ⚠ (unit coverage includes `RunAgentUseCase` mismatch case; Compose UI tests for agent builder remain optional)
- All ACs Met: ✓

### Acceptance Criteria Coverage (updated)

| AC | Assessment |
|----|------------|
| 1 | **Met** — Unchanged. |
| 2 | **Met** — Unchanged. |
| 3 | **Met** — **Projects:** scope + `projectId`. **Task types:** optional `taskTypeFilter` (`TaskType`), persisted, configurable in Settings, enforced when listing agents for a task and when running an agent. |
| 4 | **Met** — Unchanged. |
| 5 | **Met** — Unchanged. |

### Improvements Checklist

- [x] Task-type association for AC3 (implemented).
- [ ] Optional: Compose UI tests for Settings agent builder card (low priority).

### Security Review

No new concerns; `task_type_filter` stores enum name server-side equivalent (local DB).

### Gate Status (follow-up)

Gate: PASS → docs/qa/gates/10.1-agent-builder-and-workflows.yml

NFR assessment (prior): docs/qa/assessments/10.1-nfr-20260331.md

Risk profile (prior): docs/qa/assessments/10.1-risk-20260331.md — BUS-001 mitigated by `taskTypeFilter` implementation.

### Recommended Status

✓ Ready for Done — All acceptance criteria satisfied; residual item is optional UI tests only.

### Review Date: 2026-03-31

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Independent re-verification: `./gradlew :core:test :desktop-app:test` completed successfully (exit code 0). Spot-check of `AgentDefinition` / `AgentDefinitionRequest` (`associatedTools`, `taskTypeFilter`), `RunAgentUseCase` task-type guard, `SettingsViewModel` save/delete, and `AgentDefinitionCard` in `PlaceholderViews.kt` confirms behavior aligned with prior PASS assessment. No regressions identified.

### Refactoring Performed

None.

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ⚠ — Strong unit coverage; Compose UI tests for agent builder still optional (see gate TEST-001).
- All ACs Met: ✓

### Improvements Checklist

- [ ] Optional: Compose UI tests for Settings agent builder (`TEST-001` in gate).

### Security Review

No new findings; local enum-backed persistence and desktop scope unchanged.

### Performance Considerations

None blocking.

### Files Modified During Review

None (QA documentation only).

### Gate Status

Gate: PASS → docs/qa/gates/10.1-agent-builder-and-workflows.yml

Risk profile: docs/qa/assessments/10.1-risk-20260331.md

NFR assessment: docs/qa/assessments/10.1-nfr-20260331.md

### Recommended Status

✓ Ready for Done — Story owner may set **Status** to Done when ready; optional UI tests remain a future enhancement.
