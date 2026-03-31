# Story 12.4: Analysis Run History and Operational Dashboard

**Epic:** Epic 12 - AI Slack Channel Analyzer

**As a** team lead,  
**I want** to review Slack analysis run history and current processing status,  
**so that** I can understand coverage, failures, and whether the analyzer is operating correctly.

## Status

Done

## Acceptance Criteria

1. The UI shows analysis run history with run date and time, duration or completion state, and triggering mode.
2. Each run displays how many channels succeeded, failed, or were skipped.
3. The UI shows whether a run is currently in progress and which channels are still being processed.
4. Users can inspect failure details for unsuccessful channels.
5. Analysis run history is filterable by date, status, and trigger type.

## Requirements Mapping

- FR37: Analysis run history and current status
- NFR12: Responsive, observable background job execution

## Dependencies

- Story 12.2: Incremental Slack Analysis Execution
- Epic 3: Visibility and Operational Control

## Architecture References

- [Architecture Overview](../architecture.md)

## UX References

- [Front-end Spec: Slack Analyzer](../front-end-spec.md#16-slack-analyzer)
- [Front-end Spec: Plugin Catalog & Operational Health](../front-end-spec.md#15-plugin-catalog--operational-health)
- [Mockup: Slack Analyzer](../mockups/slack-analyzer.html)

## Dev Notes

- Reuse existing activity history and health-monitoring patterns instead of inventing a separate operational model.
- Current-run visibility should expose channel-level progress when possible.

## Tasks / Subtasks

- [x] Persist structured run metrics on `SLACK_ANALYSIS_RUN` activities (counts, duration, trigger) and emit channel-level progress during execution.
- [x] Load and filter run history in `SlackAnalyzerViewModel` via `ActivityRepository.findFiltered`.
- [x] Add Slack Analyzer UI: run history list, filters (date range, outcome, trigger), in-progress channel queue, expandable failure details.
- [x] Unit tests for service metadata, channel progress, and view model run history (including filter scenarios per QA TEST-001).

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Completion Notes List

- Run history reuses `ActivityType.SLACK_ANALYSIS_RUN` with added metadata: `processedCount`, `skippedCount`, `failedCount`, `durationMs`, existing `trigger` and `failures`.
- `SlackAnalysisChannelProgress` reports the active channel and remaining queue while a run is in progress.
- Slack Analyzer screen uses a `LazyColumn` with a dedicated “Run history & status” section and filter card aligned with activity history patterns.
- QA follow-up: added `SlackAnalyzerViewModelTest` cases for `ActivityStatus`, `SlackAnalysisTrigger`, and local date range filters on run history (`docs/qa/gates/12.4-analysis-run-history-and-dashboard.yml` TEST-001).

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/SlackChannelAnalysisService.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/slack/DefaultSlackChannelAnalysisService.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/slack/DefaultSlackChannelAnalysisServiceTest.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/SlackAnalyzerViewModel.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/slack/SlackAnalyzerView.kt`
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/SlackAnalyzerViewModelTest.kt`
- `docs/stories/story-12.4-analysis-run-history-and-dashboard.md`

### Change Log

- 2026-03-31: Story 12.4 — run history dashboard, activity metadata, channel progress callback, tests.
- 2026-03-31: QA gate TEST-001 — ViewModel unit tests for run-history filters (status, trigger, date range) and `clearRunHistoryFilters`.
- 2026-03-31: Story marked Done after QA re-review (gate PASS).

## QA Results

### Review Date: 2026-03-31

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation aligns with the story: `SLACK_ANALYSIS_RUN` activities carry structured metadata (`processedCount`, `skippedCount`, `failedCount`, `durationMs`, `trigger`, `failures`), `DefaultSlackChannelAnalysisService` emits `SlackAnalysisChannelProgress` during multi-channel runs, and `SlackAnalyzerView` presents a filter card, run rows with time/duration/trigger, expandable failure details, and in-progress state for analyses started from this screen. `./gradlew` tests for `DefaultSlackChannelAnalysisServiceTest` and `SlackAnalyzerViewModelTest` pass.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓ Kotlin patterns and explicit types match project conventions.
- Project Structure: ✓ Changes live under core Slack service and desktop Slack analyzer UI as expected.
- Testing Strategy: ⚠ Core service is well covered; ViewModel run-history filters are not unit-tested (see gate).
- All ACs Met: ✓ Functional behavior verified by code review and passing targeted tests; see operational note for AC3 vs scheduled runs below.

### Requirements Traceability (Given-When-Then summary)

| AC | Coverage |
| --- | --- |
| 1 | **Full** — `SlackAnalysisRunHistoryRow` shows formatted `createdAt`, duration, trigger, status; `DefaultSlackChannelAnalysisServiceTest` asserts metadata. |
| 2 | **Full** — Row displays succeeded/skipped/failed counts from metadata. |
| 3 | **Partial (integration)** — Manual “Run analysis now” shows `analysisRunInProgress`, progress indicator, and `SlackAnalysisChannelProgress` text; scheduled/headless runs do not populate this ViewModel without further work. |
| 4 | **Full** — Expandable `failures` string from metadata. |
| 5 | **Full** — `loadRunHistory` uses `findFiltered` plus trigger filter; UI exposes date, outcome, trigger. **Unit gap:** filter combinations not asserted in tests. |

### Improvements Checklist

- [ ] Add `SlackAnalyzerViewModelTest` cases for status, trigger, and date filters (addresses TEST-001 in gate).
- [ ] Product confirmation: whether scheduled runs require live in-progress visibility in this dashboard (addresses OPS-001).

### Security Review

No new credential storage or logging of tokens in the reviewed changes. Failure metadata may contain API diagnostics already produced by the Slack client; consistent with prior analyzer stories.

### Performance Considerations

Run history limited to 200 rows; acceptable for desktop activity history.

### Files Modified During Review

None (QA artifacts only: assessments and gate under `docs/qa/`).

### Gate Status

Gate: CONCERNS → docs/qa/gates/12.4-analysis-run-history-and-dashboard.yml

Risk profile: docs/qa/assessments/12.4-risk-20260331.md

NFR assessment: docs/qa/assessments/12.4-nfr-20260331.md

### Recommended Status

✗ Changes Required — Optional but recommended: add ViewModel filter tests before marking Done; confirm PO stance on scheduled-run live progress. Story owner decides final status.

### Review Date: 2026-03-31 (re-review)

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Re-verified after dev follow-up: `SlackAnalyzerViewModelTest` now includes `run history filters by ActivityStatus` (with `clearRunHistoryFilters`), `run history filters by SlackAnalysisTrigger metadata`, and `run history filters by local date range`, using `InMemoryActivityRepository` and fixed instants in the system default zone. `./gradlew :desktop-app:test --tests SlackAnalyzerViewModelTest` and `:core:test --tests DefaultSlackChannelAnalysisServiceTest` pass.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ✓ ViewModel filter behavior is now unit-tested (prior TEST-001 closed).
- All ACs Met: ✓ Trace row AC5 updated to full unit + integration coverage for filters.

### Requirements Traceability (delta)

| AC | Update |
| --- | --- |
| 5 | **Full** — Filter tests assert repository-backed `findFiltered` plus client trigger filter and date window. |

### Improvements Checklist

- [x] Add `SlackAnalyzerViewModelTest` cases for status, trigger, and date filters (TEST-001 closed).
- [ ] Product confirmation: whether scheduled runs require live in-progress visibility in this dashboard (OPS-001 — optional future story).

### Security Review

Unchanged from prior review.

### Performance Considerations

Unchanged from prior review.

### Files Modified During Review

QA gate and assessments under `docs/qa/` updated for re-review.

### Gate Status

Gate: PASS → docs/qa/gates/12.4-analysis-run-history-and-dashboard.yml

Risk profile: docs/qa/assessments/12.4-risk-20260331.md

NFR assessment: docs/qa/assessments/12.4-nfr-20260331.md

### Recommended Status

✓ Ready for Done — PO may still schedule follow-up for scheduled-run live visibility if needed; not blocking for 12.4.
