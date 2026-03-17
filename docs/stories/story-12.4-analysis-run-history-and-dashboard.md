# Story 12.4: Analysis Run History and Operational Dashboard

**Epic:** Epic 12 - AI Slack Channel Analyzer

**As a** team lead,  
**I want** to review Slack analysis run history and current processing status,  
**so that** I can understand coverage, failures, and whether the analyzer is operating correctly.

## Status

Draft

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
