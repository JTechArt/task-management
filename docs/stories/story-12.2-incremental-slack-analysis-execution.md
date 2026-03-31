# Story 12.2: Incremental Slack Analysis Execution

**Epic:** Epic 12 - AI Slack Channel Analyzer

**As a** team-oriented developer,  
**I want** Slack analysis runs to process only new conversation activity,  
**so that** repeated runs complete efficiently and avoid generating duplicate summaries.

## Status

Done

## Tasks / Subtasks

- [x] Per-channel checkpoint persistence (`channel_checkpoints_json`) merged with plugin fields outside schema; split/load/save via `internalFields` on `PluginConfigurationEditorState`.
- [x] `SlackWebApiClient.conversations.history` + `DefaultSlackChannelAnalysisService` incremental scan, skip when no newer `ts`, continue on channel error, checkpoint rollback if save fails.
- [x] `SlackAnalyzerView` / `SlackAnalyzerViewModel`: run analysis (async), progress text, linear progress, last summary/error; `ActivityType.SLACK_ANALYSIS_RUN` for diagnostics.
- [x] Unit tests: checkpoint merge/split, analysis service (skip, multi-channel failure, activity log, save-failure rollback, ViewModel runAnalysis).

## Acceptance Criteria

1. Each analysis run tracks the last successfully scanned time or message checkpoint per channel.
2. Manual and scheduled runs skip channels with no new content since the previous successful scan.
3. The run engine can continue processing remaining channels when one channel fails.
4. A currently running analysis job is visible to the user.
5. Failures preserve diagnostic detail without losing the last valid checkpoint for unaffected channels.

## Requirements Mapping

- FR35: Incremental scans and skip behavior
- FR37: Visible run status and failure reporting
- NFR12: Responsive asynchronous execution

## Dependencies

- Story 12.1: Slack Analysis Source and Schedule Configuration
- Story 11.4: Plugin Status, Health, and Operational Visibility

## Architecture References

- [Architecture Overview](../architecture.md)

## UX References

- [Front-end Spec: Slack Analyzer](../front-end-spec.md#16-slack-analyzer)
- [Mockup: Slack Analyzer](../mockups/slack-analyzer.html)

## Dev Notes

- Checkpoint persistence should be channel-specific and resilient to partial failures.
- Background execution must surface progress without blocking desktop UI responsiveness.

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

- `./gradlew :core:compileKotlin :desktop-app:compileKotlin` — success
- `./gradlew test` — success (after QA gate fixes)

### Completion Notes List

- Checkpoints stored as JSON in `channel_checkpoints_json` (not plugin schema fields); UI editors keep internal keys across save via `splitPluginConfigurationFields` / `mergePluginConfigurationFields`.
- `DefaultSlackChannelAnalysisService` uses Slack `conversations.history` with `oldest` on first page; filters `ts` strictly after last checkpoint; paginates with `cursor`; `SlackAnalysisTrigger.MANUAL` and `SCHEDULED` share the same engine (scheduler wiring to call `SCHEDULED` is out of scope for this story).
- Manual runs: Slack Analyzer screen “Run analysis now” with progress and non-blocking coroutine execution.
- Failures: per-channel `Failed` outcome; checkpoint only advances after successful save; other channels’ checkpoints unchanged.
- **QA gate follow-up:** `SaveFirstFailPluginManagement` test proves checkpoint JSON stays at prior `ts` when `saveConfiguration` fails; `FakeSlackChannelAnalysisService` covers `SlackAnalyzerViewModel.runAnalysis` completion; `SLACK_ANALYSIS_RUN` uses `ActivityStatus.FAILED` when any channel outcome is `Failed`.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Activity.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/plugin/plugin_configuration_snapshot_support.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/plugin/slack_channel_analyzer_plugin_ids.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/SlackChannelAnalysisService.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/plugin/slack_web_api_client.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/slack/DefaultSlackChannelAnalysisService.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/plugin/PluginConfigurationSnapshotSupportTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/slack/DefaultSlackChannelAnalysisServiceTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/plugin/DefaultPluginPrerequisiteProbeSlackTest.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/slack/SlackAnalyzerView.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/PluginManagementViewModel.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/SlackAnalyzerViewModel.kt`
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/SlackAnalyzerViewModelTest.kt`
- `docs/stories/story-12.2-incremental-slack-analysis-execution.md`
- `docs/qa/gates/12.2-incremental-slack-analysis-execution.yml`
- `docs/qa/assessments/12.2-nfr-20260331.md`

### Change Log

- 2026-03-31: Implemented incremental Slack analysis execution (checkpoints, history API, UI run + progress, diagnostics activity).
- 2026-03-31: QA gate fixes — tests for save-failure rollback + ViewModel runAnalysis; FAILED activity on partial channel failure; gate/NFR updated to PASS.
- 2026-03-31: Story marked **Done** after QA PASS.

## QA Results

### Review Date: 2026-03-31

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

`DefaultSlackChannelAnalysisService` implements per-channel checkpoints in `channel_checkpoints_json`, incremental `conversations.history` with `oldest` on the first page and `isStrictlyAfter` filtering, skip when no new content, `continue` on channel API error, and in-memory rollback when `saveConfiguration` fails before recording `Processed`. `Mutex` prevents overlapping runs. UI exposes `analysisRunInProgress`, `LinearProgressIndicator`, progress text, and last summary/error. `ActivityType.SLACK_ANALYSIS_RUN` records run summaries with failure diagnostics in metadata. `PluginConfigurationSnapshotSupportTest` and `split`/`merge` keep internal checkpoint JSON out of schema fields.

**Tests:** `DefaultSlackChannelAnalysisServiceTest` passes (skip on second run, continue after one channel error, activity logged). Targeted `:core:test` for `DefaultSlackChannelAnalysisServiceTest` executed successfully.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ⚠ At first review, gaps TEST-001/TEST-002/OPS-001 were open; **re-review** closed them (see below).
- All ACs Met: ✓

### Requirements Traceability

| AC | Evidence |
| --- | --- |
| 1 Per-channel checkpoint | `parseCheckpoints` / `saveCheckpoints` / `FIELD_CHANNEL_CHECKPOINTS_JSON` |
| 2 Skip when no new content | `SkippedNoNewContent` when filtered timestamps empty |
| 3 Continue on channel failure | `Failed` + `continue`; multi-channel test |
| 4 Visible running job | `analysisRunInProgress`, progress message, linear indicator |
| 5 Diagnostics + checkpoints | Failed outcomes + rollback on save failure; activity metadata |

### Improvements Checklist

- [x] TEST-001: `SaveFirstFailPluginManagement` + `save checkpoint failure rolls back…` test
- [x] TEST-002: `FakeSlackChannelAnalysisService` + `runAnalysis updates progress then summary…`
- [x] OPS-001: `recordRunActivity` uses `ActivityStatus.FAILED` when any channel outcome is `Failed`

### Security Review

No new hardcoded secrets; tokens read from existing plugin snapshot.

### Performance Considerations

Serial runs via mutex; acceptable for desktop MVP.

### Files Modified During Review

None.

### Gate Status

Superseded by **re-review** entry below (gate lifted to PASS).

### Recommended Status

Superseded by **re-review** entry below.

### Review Date: 2026-03-31 (re-review)

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Independent verification: `DefaultSlackChannelAnalysisServiceTest` includes `save checkpoint failure rolls back in memory and does not persist new ts` using `SaveFirstFailPluginManagement` (first `saveConfiguration` fails; persisted `channel_checkpoints_json` keeps prior `50.0`, not `100.0`). Multi-channel test asserts `ActivityStatus.FAILED` when a channel fails. `SlackAnalyzerViewModelTest` injects `FakeSlackChannelAnalysisService` and asserts `runAnalysis` clears `analysisRunInProgress` and sets summary. `recordRunActivity` uses `FAILED` when any `SlackChannelRunOutcome.Failed` is present. Ran `:core:test` (`DefaultSlackChannelAnalysisServiceTest`) and `:desktop-app:test` (`SlackAnalyzerViewModelTest`); both succeeded.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ✓
- All ACs Met: ✓

### Gate Status

Gate: PASS → docs/qa/gates/12.2-incremental-slack-analysis-execution.yml  
NFR assessment: docs/qa/assessments/12.2-nfr-20260331.md

### Recommended Status

**Done** (2026-03-31) — story status set after QA gate PASS.
