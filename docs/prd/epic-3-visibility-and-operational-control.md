# Epic 3: Visibility and Operational Control

## Epic Goal

Strengthen AiTask as an everyday operating console by giving developers and team leads clear visibility into task progress, system status, recent actions, and workspace lifecycle controls. This epic improves operational confidence and usability after the core workspace-launch automation is in place, making the product easier to manage over time and across multiple projects.

## Requirements Mapping

- **FR21–FR22:** Dashboard, activity history
- **FR25:** Health and connection status
- **FR28:** Workspace retention and cleanup
- **NFR5, NFR6, NFR7:** Responsiveness, error handling, offline access

## Architecture References

- [Component Architecture: Activity Tracking](../../architecture.md#8-activity-tracking-component)
- [Component Architecture: Workspace Management – RetentionPolicy](../../architecture.md#6-workspace-management-component)
- [Database Schema: activity_log](../../architecture.md#7-activity-log)
- [Quality Attributes: Usability, Reliability](../../architecture.md#usability)

## UX References

- [Main Dashboard layout](../../front-end-spec.md#1-main-dashboard)
- [Integrations & Health screen](../../front-end-spec.md#5-integrations--health)
- [Component: Status Indicator](../../front-end-spec.md#core-components)
- [Performance Goals: Startup, Interaction Response](../../front-end-spec.md#performance-goals)

---

## Story 3.1: Dashboard Overview for Active Work

**As a** developer,  
**I want** a dashboard that summarizes my projects and tasks,  
**so that** I can quickly understand current work without opening each item individually.

### Acceptance Criteria

1. The dashboard shows summary metrics for projects, tasks by status, and recently active work.
2. The dashboard highlights tasks currently in progress or recently launched.
3. The dashboard provides navigation shortcuts into the most relevant project and task views.
4. Dashboard content reflects persisted project and task data accurately after restart.
5. Empty or first-use states provide clear guidance rather than blank panels.

---

## Story 3.2: Activity History and Recent Events

**As a** developer,  
**I want** to review recent application activity,  
**so that** I can understand what happened during task setup, launch, and management workflows.

### Acceptance Criteria

1. The application records and displays recent activity entries for key events including task creation, workspace preparation, IDE launch, Git preparation, and rule application.
2. Activity entries include enough context to identify the relevant project, task, action, and result.
3. Users can view activity in chronological order with the most recent events first.
4. Users can filter activity by project, task, or event type.
5. Failed actions are visually distinguishable from successful actions in the activity history.

---

## Story 3.3: Health Monitoring and Connectivity Status

**As a** developer,  
**I want** visibility into system and integration health,  
**so that** I can diagnose why AiTask may not be able to complete workflow automation.

### Acceptance Criteria

1. The application displays current health status for the database, configured repositories, and enabled external integrations.
2. Health checks distinguish between healthy, degraded, and failed states where applicable.
3. The user can manually refresh health status from the UI.
4. Failure states include actionable context rather than generic error labels.
5. Health status does not block normal browsing of locally available project and task data when an external service is unavailable.

---

## Story 3.4: Workspace Retention and Cleanup Controls

**As a** developer,  
**I want** control over what happens to task workspaces after work changes state,  
**so that** I can balance disk usage with safety for unfinished or reference work.

### Acceptance Criteria

1. Users can configure workspace behavior for completed and deleted tasks, including retain, archive, or clean up options.
2. The application warns users before destructive cleanup actions that remove local workspace data.
3. Completed tasks clearly show the current workspace state or retention outcome.
4. Cleanup or archival actions are recorded in activity history.
5. Failed cleanup actions do not remove task metadata and provide actionable feedback.

---

## Story 3.5: Enhanced Management Views and Filtering

**As a** developer,  
**I want** stronger filtering and management views for projects, tasks, and rules,  
**so that** I can operate efficiently as the amount of tracked work grows.

### Acceptance Criteria

1. Project, task, and rule views support search and filtering based on relevant metadata.
2. The task view supports filtering by status, task type, and project.
3. The project view supports filtering by tags, teams, or other configured metadata when available.
4. Rule management views support locating rules by scope or linked project or repository.
5. Filter and search interactions return results quickly enough to support normal desktop productivity use.
