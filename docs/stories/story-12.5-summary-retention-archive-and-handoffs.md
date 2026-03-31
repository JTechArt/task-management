# Story 12.5: Summary Retention, Archive, and Automation Handoffs

**Epic:** Epic 12 - AI Slack Channel Analyzer

**As a** team lead,  
**I want** Slack summaries retained and surfaced according to clear lifecycle rules,  
**so that** recent insights stay accessible while older data is archived and structured for future automation.

## Status

Approved

## Acceptance Criteria

1. The main summary views show only the most recent 7 days of summaries by default.
2. Summaries older than 7 days move to an archive state without being immediately deleted.
3. Archived summaries are retained for up to 30 days total before cleanup.
4. Summary records preserve labels, references, and metadata needed for future task-creation or project-assignment automations.
5. Retention and archival actions execute automatically and are visible in operational history.

## Requirements Mapping

- FR38: Preserve automation-ready metadata
- FR39: Summary retention, archive, and cleanup behavior
- NFR13: Predictable archival and retention processing

## Dependencies

- Story 12.3: Daily, Per-Channel, and Per-Topic Summaries with References
- Story 12.4: Analysis Run History and Operational Dashboard
- Epic 10: AI-Powered Task Automation for future handoff consumption

## Architecture References

- [Architecture Overview](../architecture.md)

## UX References

- [Front-end Spec: Slack Analyzer](../front-end-spec.md#16-slack-analyzer)
- [Mockup: Slack Analyzer](../mockups/slack-analyzer.html)

## Dev Notes

- Archive state should remain queryable without polluting the default recent-summary view.
- Preserve automation metadata even when summaries move out of the active 7-day window.
