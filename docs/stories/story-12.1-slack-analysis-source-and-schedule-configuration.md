# Story 12.1: Slack Analysis Source and Schedule Configuration

**Epic:** Epic 12 - AI Slack Channel Analyzer

**As a** team-oriented developer,  
**I want** to configure which Slack channels are analyzed and when analysis runs,  
**so that** the system summarizes the conversations that matter without requiring manual setup every time.

## Status

Done

## Acceptance Criteria

1. A user can select one or more Slack channels as analysis sources.
2. A user can configure analysis execution as scheduled daily runs, manual runs, or both.
3. Scheduled runs support a configurable execution time such as daily at 10:00 AM.
4. The plugin validates Slack connectivity and channel accessibility before saving the configuration.
5. Disabled or inaccessible channels are clearly identified in configuration status.

## Requirements Mapping

- FR34: Slack analysis source and schedule configuration
- FR37: Configuration status must support operational visibility

## Dependencies

- Epic 4: External Integrations, Portability, and Distribution
- Epic 11: Plugin Management and Add-on Framework

## Architecture References

- [Architecture Overview](../architecture.md)

## UX References

- [Front-end Spec: Slack Analyzer](../front-end-spec.md#16-slack-analyzer)
- [Mockup: Slack Analyzer](../mockups/slack-analyzer.html)
- [Mockup: Plugins](../mockups/plugins.html)

## Dev Notes

- Build this as a plugin-hosted configuration surface rather than a core feature screen.
- Reuse Slack connectivity validation patterns from the integration foundation where practical.

## Tasks / Subtasks

- [x] Add `plugin.slack-channel-analyzer` catalog definition (channels, schedule mode, daily time, Slack API prerequisites).
- [x] Implement Slack connectivity and channel accessibility checks in `DefaultPluginPrerequisiteProbe` (HTTPS to Slack Web API only).
- [x] Support conditional validation rules (`whenFieldId` / `whenFieldMatches`) for daily run time when schedule mode requires it.
- [x] Add Slack Analyzer primary navigation destination and `SlackAnalyzerView` using plugin configuration form content.
- [x] Unit tests: catalog size, `SlackAnalyzerViewModel` load, existing plugin management tests updated.
- [x] QA follow-up (gate TEST-001): `SlackWebApiClient` + tests for Slack prerequisite probes and JSON ok parsing.

## Dev Agent Record

### Agent Model Used

Composer (Cursor agent)

### Debug Log References

- `./gradlew :core:compileKotlin :desktop-app:compileKotlin` — success
- `./gradlew test` — success (full suite after QA fixes; includes new Slack probe tests)

### Completion Notes List

- Introduced `plugin.slack-channel-analyzer` with fields: bot token, comma-separated channel IDs, `schedule_mode` (`manual_only` | `daily` | `manual_and_daily`), and `daily_run_time` (HH:mm) when daily scheduling applies.
- Save and validate paths run full plugin validation including `auth.test` and per-channel `conversations.info`; inaccessible channels are listed in the prerequisite result message.
- Extracted `PluginConfigurationFormContent` for reuse between the plugin dialog and the Slack Analyzer screen.
- **QA gate (TEST-001):** Introduced `SlackWebApiClient` / `DefaultSlackWebApiClient` so `DefaultPluginPrerequisiteProbe` Slack checks are injectable; added `DefaultPluginPrerequisiteProbeSlackTest` and `SlackJsonResponseOkTest`. Re-run `review-story` / refresh `docs/qa/gates/12.1-*.yml` after verification (dev does not edit gate files).

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/plugin/PluginConfiguration.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/plugin/PluginCatalog.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/plugin/DefaultPluginPrerequisiteProbe.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/plugin/slack_web_api_client.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/plugin/InMemoryPluginManagementService.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/plugin/DefaultPluginPrerequisiteProbeSlackTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/plugin/SlackJsonResponseOkTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/plugin/InMemoryPluginManagementServiceTest.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/TaskManagerApp.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/Navigation.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/plugins/PluginManagementView.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/slack/SlackAnalyzerView.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/SlackAnalyzerViewModel.kt`
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/PluginManagementViewModelTest.kt`
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/SlackAnalyzerViewModelTest.kt`
- `docs/stories/story-12.1-slack-analysis-source-and-schedule-configuration.md`

### Change Log

- 2026-03-31: Implemented Epic 12 story 12.1 — Slack analyzer plugin config, Slack API prerequisites, Slack Analyzer navigation and view, tests.
- 2026-03-31: QA follow-up — `SlackWebApiClient` extraction, unit tests for SLACK_API / SLACK_CHANNELS probe behavior and `slackJsonResponseOk` (addresses gate TEST-001).
- 2026-03-31: Story marked **Done** after QA gate PASS.

## QA Results

### Review Date: 2026-03-31

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation aligns with the plugin-hosted configuration approach: `plugin.slack-channel-analyzer` in `PluginCatalogFixtures` defines channels (comma-separated IDs), `schedule_mode`, conditional `daily_run_time`, and Slack prerequisites. `InMemoryPluginManagementService.evaluateConfiguration` runs field rules, HH:mm validation for daily modes, then prerequisite evaluation before save. `DefaultPluginPrerequisiteProbe` uses `https://slack.com/api/auth.test` and `conversations.info` only. UI reuses `PluginConfigurationFormContent`, which surfaces prerequisite rows including the inaccessible-channel message. `./gradlew test` completed successfully in this review environment.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓ Kotlin patterns and explicit types match project conventions.
- Project Structure: ✓ Core plugin domain/infrastructure and desktop UI/viewmodel separation preserved.
- Testing Strategy: ⚠ At first review, Slack probes were not unit-tested; **re-review 2026-03-31** added `DefaultPluginPrerequisiteProbeSlackTest` / `SlackJsonResponseOkTest` (see below).
- All ACs Met: ✓ Functionally satisfied (1–5).

### Requirements Traceability (Given-When-Then mapping)

| AC | Evidence |
| --- | --- |
| 1 Multiple channels | `analysis_channels` field; probe splits and checks each ID |
| 2 Manual / scheduled / both | `schedule_mode` options `manual_only`, `daily`, `manual_and_daily` |
| 3 Daily time | `daily_run_time` with conditional rule and HH:mm regex |
| 4 Validate before save | `saveConfiguration` → `validateConfiguration` → prerequisites |
| 5 Inaccessible channels visible | Prerequisite message `Disabled or inaccessible channels: …` and `PrerequisiteRow` in form |

### Improvements Checklist

- [x] Add automated tests for `DefaultPluginPrerequisiteProbe` Slack evaluation (TEST-001 — closed in re-review)
- [x] Confirmed acceptance criteria covered by code review + passing Gradle tests

### Security Review

No hardcoded tokens. Outbound calls target fixed Slack API URLs over HTTPS. Advise ensuring no verbose logging of HTTP bodies if diagnostics are added later (SEC-001 in gate).

### Performance Considerations

HTTP connect/read timeouts are set on Slack checks; acceptable for interactive validation.

### Files Modified During Review

None.

### Gate Status

Gate: CONCERNS → docs/qa/gates/12.1-slack-analysis-source-and-schedule-configuration.yml  
Risk profile: Not generated for this review (low scope; see `risk_summary` in gate).  
NFR assessment: docs/qa/assessments/12.1-nfr-20260331.md

### Recommended Status

Superseded by **re-review** entry below (TEST-001 closed).

### Review Date: 2026-03-31 (re-review)

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Follow-up addresses prior **TEST-001**: `SlackWebApiClient` / `DefaultSlackWebApiClient` extract real HTTP; `DefaultPluginPrerequisiteProbe` delegates `authTest` and per-channel `conversationInfoOk` to the injectable client. `DefaultPluginPrerequisiteProbeSlackTest` exercises blank token, accept/reject auth, empty channel list, all reachable channels, and inaccessible channel IDs in the failure message. `SlackJsonResponseOkTest` locks JSON parsing variants. `./gradlew test` passed after verification.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓
- Project Structure: ✓
- Testing Strategy: ✓ Slack probe paths covered with test doubles; JSON helper covered.
- All ACs Met: ✓

### Improvements Checklist (re-review)

- [x] Automated tests for `DefaultPluginPrerequisiteProbe` Slack evaluation (`DefaultPluginPrerequisiteProbeSlackTest`, `SlackJsonResponseOkTest`)

### Gate Status

Gate: PASS → docs/qa/gates/12.1-slack-analysis-source-and-schedule-configuration.yml  
NFR assessment: docs/qa/assessments/12.1-nfr-20260331.md (maintainability updated to PASS)

### Recommended Status

**Done** (2026-03-31) — aligned with QA gate PASS and story status.
