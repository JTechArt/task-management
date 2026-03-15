# Story 3.3: Health Monitoring and Connectivity Status

**Epic:** Epic 3 - Visibility and Operational Control

**As a** developer,  
**I want** visibility into system and integration health,  
**so that** I can diagnose why AiTask may not be able to complete workflow automation.

## Acceptance Criteria

1. The application displays current health status for the database, configured repositories, and enabled external integrations.
2. Health checks distinguish between healthy, degraded, and failed states where applicable.
3. The user can manually refresh health status from the UI.
4. Failure states include actionable context rather than generic error labels.
5. Health status does not block normal browsing of locally available project and task data when an external service is unavailable.

## Architecture References

- [Quality Attributes: Reliability](../architecture.md#reliability)

## UX References

- [Integrations & Health screen](../front-end-spec.md#5-integrations--health)
- [Component: Status Indicator](../front-end-spec.md#core-components)
- [Visual Mockup: Integrations & Health](../mockups/integrations.html)
- [Visual Mockup: Dashboard](../mockups/dashboard.html)
