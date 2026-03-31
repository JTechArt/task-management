# Story 12.3: Daily, Per-Channel, and Per-Topic Summaries with References

**Epic:** Epic 12 - AI Slack Channel Analyzer

**As a** team lead,  
**I want** AI-generated Slack summaries grouped by day, channel, and discussion topic,  
**so that** I can quickly understand what happened and trace the summary back to source conversations.

## Status

Approved

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
