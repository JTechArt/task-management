# Story 12.2: Incremental Slack Analysis Execution

**Epic:** Epic 12 - AI Slack Channel Analyzer

**As a** team-oriented developer,  
**I want** Slack analysis runs to process only new conversation activity,  
**so that** repeated runs complete efficiently and avoid generating duplicate summaries.

## Status

Draft

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
