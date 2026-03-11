# Story 3.4: Workspace Retention and Cleanup Controls

**Epic:** Epic 3 - Visibility and Operational Control

**As a** developer,  
**I want** control over what happens to task workspaces after work changes state,  
**so that** I can balance disk usage with safety for unfinished or reference work.

## Acceptance Criteria

1. Users can configure workspace behavior for completed and deleted tasks, including retain, archive, or clean up options.
2. The application warns users before destructive cleanup actions that remove local workspace data.
3. Completed tasks clearly show the current workspace state or retention outcome.
4. Cleanup or archival actions are recorded in activity history.
5. Failed cleanup actions do not remove task metadata and provide actionable feedback.

## Architecture References

- [Component Architecture: Workspace Management – RetentionPolicy](../architecture.md#6-workspace-management-component)

## UX References

- [Confirmation Dialog component (destructive actions)](../front-end-spec.md#core-components)

