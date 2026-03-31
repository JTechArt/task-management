# Story 12.3: Daily, Per-Channel, and Per-Topic Summaries with References

**Epic:** Epic 12 - AI Slack Channel Analyzer

**As a** team lead,  
**I want** AI-generated Slack summaries grouped by day, channel, and discussion topic,  
**so that** I can quickly understand what happened and trace the summary back to source conversations.

## Status

Done

## Acceptance Criteria

1. The system generates summaries for each analyzed day and channel.
2. Distinct topics discussed within the same channel are summarized separately when the model detects meaningful topic separation.
3. Each summary includes references to the source Slack channel and, when available, the relevant message or thread link.
4. Summaries are labeled or categorized for later filtering and automation use.
5. Summary generation failures for one channel or topic do not hide successful results from other analyzed content.

## Requirements Mapping

- FR36: Summaries grouped by day, channel, and topic
- FR38: Labeled and categorized summary outputs

## Dependencies

- Story 12.2: Incremental Slack Analysis Execution
- Epic 7: Local AI/ML Integration or equivalent analyzer runtime

## Architecture References

- [Architecture Overview](../architecture.md)

## UX References

- [Front-end Spec: Slack Analyzer](../front-end-spec.md#16-slack-analyzer)
- [Mockup: Slack Analyzer](../mockups/slack-analyzer.html)

## Dev Notes

- Summary records should preserve source metadata needed for later traceability and automation handoff.
- Topic grouping should degrade safely when the model cannot confidently separate discussions.

## Dev Agent Record

### Agent Model Used

GPT-5.2 (Cursor agent)

### Completion Notes

- Extended Slack `conversations.history` handling to retain message text and thread metadata for analysis.
- Grouped new messages by UTC calendar day and channel, then generated per-day topic summaries via the default LLM profile (OpenAI-compatible and Ollama-style endpoints supported).
- Each summary record includes category, labels, Slack archive URLs for the channel and anchor message, optional thread link, and timestamps for traceability.
- When no LLM is configured, the model call fails, or JSON parsing fails, the service falls back to a single digest topic per day (flagged) so other channels still complete successfully.
- Run activity metadata stores `summariesJson` (truncated when large) and `summaryDiagnostics` for automation and auditing.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/SlackChannelMessage.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/SlackConversationSummaryItem.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/SlackSummaryGenerationService.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/SlackChannelAnalysisService.kt` (updated `Processed` outcome)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/plugin/slack_web_api_client.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/slack/DefaultSlackChannelAnalysisService.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/slack/DefaultSlackSummaryGenerationService.kt` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/slack/DefaultSlackChannelAnalysisServiceTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/slack/DefaultSlackSummaryGenerationServiceTest.kt` (new)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt`
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/SlackAnalyzerViewModelTest.kt`
- `docs/stories/story-12.3-slack-summaries-with-references.md` (Dev Agent Record only)

### Change Log

- 2026-03-31: Story 12.3 — AI Slack summaries with per-day grouping, topic separation via LLM, references, categories, isolated failures, activity metadata.
- 2026-03-31: Status set to Done; QA gate and NFR assessment recorded under `docs/qa/`.

### Debug Log References

- `./gradlew :core:compileKotlin :core:compileTestKotlin :desktop-app:compileKotlin :desktop-app:compileTestKotlin`
- `./gradlew test`

## QA Results

### Review Date: 2026-03-31

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation aligns with the story: messages are grouped by UTC day and channel, `DefaultSlackSummaryGenerationService` requests structured JSON topics with `category`, `topicLabel`, anchor timestamps, and builds Slack archive URLs. `SlackConversationSummaryItem` carries labels and categories for filtering. `DefaultSlackChannelAnalysisService` integrates summarization after successful checkpoint save, records `summariesJson` and `summaryDiagnostics` on `SLACK_ANALYSIS_RUN` activities, and continues processing when one channel’s Slack fetch fails. `./gradlew test` completed successfully.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓ Kotlin patterns, explicit types, focused services.
- Project Structure: ✓ Domain models and services under `core`, DI wiring in `DependencyContainer`.
- Testing Strategy: ⚠ Core paths covered; see Improvements Checklist.
- All ACs Met: ✓ Verified by code review plus green test run.

### Requirements Traceability (Given-When-Then)

| AC | Evidence |
|----|----------|
| 1 — Summaries per day and channel | `groupMessagesByUtcDay` + `summarizeChannelDays` loop per day; `Processed` outcomes include `summaries`. |
| 2 — Distinct topics when model separates | LLM prompt asks for multiple `topics`; `parseTopicsFromModel` maps each to `SlackConversationSummaryItem`. |
| 3 — References to channel and message/thread | `channelUrl`, `messageUrl`, optional `threadUrl` and timestamps on model. |
| 4 — Labeled / categorized for filtering | `topicLabel`, `category` (normalized set), `degradedSingleTopicFallback` flag. |
| 5 — Failures do not hide other results | Per-day catch with fallback; channel loop uses `Failed` vs `Processed`; test `continues when a channel fails`. |

### Improvements Checklist

- [ ] Add unit test: mocked LLM returns two topics → two summary items (strengthens AC2 regression safety).
- [ ] Optional: unit test for Ollama-style endpoint branch if regression risk warrants it.
- [ ] Optional: sanitize or code-map diagnostic strings stored in activity metadata (MNT-001).

### Security Review

No hardcoded secrets observed. LLM requests use configured endpoint and optional Bearer token. Minor note: exception messages may appear in `summaryDiagnostics` (see gate `MNT-001`).

### Performance Considerations

Prompt and stored JSON truncation are reasonable for a desktop batch analyzer.

### Files Modified During Review

None.

### Gate Status

Gate: CONCERNS → docs/qa/gates/12.3-slack-summaries-with-references.yml  
NFR assessment: docs/qa/assessments/12.3-nfr-20260331.md

### Recommended Status

✓ Done — Story closed 2026-03-31. Gate CONCERNS (TEST-001, MNT-001) remain as optional follow-ups.
