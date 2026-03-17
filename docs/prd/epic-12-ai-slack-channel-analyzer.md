# Epic 12: AI Slack Channel Analyzer

## Epic Goal

Deliver an AI-powered Slack analysis plugin that scans configured channels, groups discussions by channel, day, and topic, and turns that activity into traceable summaries. The feature should provide operational history, preserve references back to Slack, and create structured outputs that can later feed automated task creation.

## Requirements Mapping

- **FR34-FR39:** Slack analysis source configuration, incremental scans, structured summaries, run history, categorization, and retention
- **NFR12-NFR13:** Responsive job execution and predictable retention processing

## Dependencies

- **Depends on:** Epic 4 (Slack integration foundation), Epic 11 (plugin management and add-on framework)
- **Integrates with:** Epic 3 (history and operational visibility), Epic 10 (future task automation handoffs)

## Architecture References

- [Architecture Overview](../architecture.md)
- Slack integration and credential handling architecture
- Background job scheduling and processing architecture
- Plugin extension architecture

## UX References

- Plugin configuration and settings
- Analysis run history dashboard
- Summary list and archive views

---

## Story 12.1: Slack Analysis Source and Schedule Configuration

**As a** team-oriented developer,  
**I want** to configure which Slack channels are analyzed and when analysis runs,  
**so that** the system summarizes the conversations that matter without requiring manual setup every time.

### Acceptance Criteria

1. A user can select one or more Slack channels as analysis sources.
2. A user can configure analysis execution as scheduled daily runs, manual runs, or both.
3. Scheduled runs support a configurable execution time such as daily at 10:00 AM.
4. The plugin validates Slack connectivity and channel accessibility before saving the configuration.
5. Disabled or inaccessible channels are clearly identified in configuration status.

---

## Story 12.2: Incremental Slack Analysis Execution

**As a** team-oriented developer,  
**I want** Slack analysis runs to process only new conversation activity,  
**so that** repeated runs complete efficiently and avoid generating duplicate summaries.

### Acceptance Criteria

1. Each analysis run tracks the last successfully scanned time or message checkpoint per channel.
2. Manual and scheduled runs skip channels with no new content since the previous successful scan.
3. The run engine can continue processing remaining channels when one channel fails.
4. A currently running analysis job is visible to the user.
5. Failures preserve diagnostic detail without losing the last valid checkpoint for unaffected channels.

---

## Story 12.3: Daily, Per-Channel, and Per-Topic Summaries with References

**As a** team lead,  
**I want** AI-generated Slack summaries grouped by day, channel, and discussion topic,  
**so that** I can quickly understand what happened and trace the summary back to source conversations.

### Acceptance Criteria

1. The system generates summaries for each analyzed day and channel.
2. Distinct topics discussed within the same channel are summarized separately when the model detects meaningful topic separation.
3. Each summary includes references to the source Slack channel and, when available, the relevant message or thread link.
4. Summaries are labeled or categorized for later filtering and automation use.
5. Summary generation failures for one channel or topic do not hide successful results from other analyzed content.

---

## Story 12.4: Analysis Run History and Operational Dashboard

**As a** team lead,  
**I want** to review Slack analysis run history and current processing status,  
**so that** I can understand coverage, failures, and whether the analyzer is operating correctly.

### Acceptance Criteria

1. The UI shows analysis run history with run date and time, duration or completion state, and triggering mode.
2. Each run displays how many channels succeeded, failed, or were skipped.
3. The UI shows whether a run is currently in progress and which channels are still being processed.
4. Users can inspect failure details for unsuccessful channels.
5. Analysis run history is filterable by date, status, and trigger type.

---

## Story 12.5: Summary Retention, Archive, and Automation Handoffs

**As a** team lead,  
**I want** Slack summaries retained and surfaced according to clear lifecycle rules,  
**so that** recent insights stay accessible while older data is archived and structured for future automation.

### Acceptance Criteria

1. The main summary views show only the most recent 7 days of summaries by default.
2. Summaries older than 7 days move to an archive state without being immediately deleted.
3. Archived summaries are retained for up to 30 days total before cleanup.
4. Summary records preserve labels, references, and metadata needed for future task-creation or project-assignment automations.
5. Retention and archival actions execute automatically and are visible in operational history.
