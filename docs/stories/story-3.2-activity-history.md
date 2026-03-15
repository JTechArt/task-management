# Story 3.2: Activity History and Recent Events

**Epic:** Epic 3 - Visibility and Operational Control

**As a** developer,  
**I want** to review recent application activity,  
**so that** I can understand what happened during task setup, launch, and management workflows.

## Acceptance Criteria

1. The application records and displays recent activity entries for key events including task creation, workspace preparation, IDE launch, Git preparation, and rule application.
2. Activity entries include enough context to identify the relevant project, task, action, and result.
3. Users can view activity in chronological order with the most recent events first.
4. Users can filter activity by project, task, or event type.
5. Failed actions are visually distinguishable from successful actions in the activity history.

## Architecture References

- [Component Architecture: Activity Tracking](../architecture.md#8-activity-tracking-component)
- [Database Schema: activity_log](../architecture.md#7-activity-log)

## UX References

- [Main Dashboard layout](../front-end-spec.md#1-main-dashboard)
- [Visual Mockup: Dashboard](../mockups/dashboard.html)
