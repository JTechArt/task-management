# Story 11.4: Plugin Status, Health, and Operational Visibility

**Epic:** Epic 11 - Plugin Management and Add-on Framework

**As a** developer,  
**I want** operational visibility into installed plugins,  
**so that** I can understand whether an add-on is ready, degraded, or failing.

## Status

Approved

## Acceptance Criteria

1. Plugin management surfaces current plugin status including enabled, disabled, misconfigured, degraded, or unavailable states.
2. Health information identifies the failing dependency, service, or validation step when a plugin is not operational.
3. A user can manually re-run plugin validation or health checks.
4. Plugin issues are isolated and do not block unrelated AiTask workflows.
5. Recent plugin events and failures are visible in operational history.

## Requirements Mapping

- FR30: Plugin status visibility
- FR32: Re-runnable validation and prerequisite reporting
- FR33: Plugin issue isolation

## Dependencies

- Story 11.2: Plugin Catalog, Install, Attach, and Remove
- Story 11.3: Plugin Configuration and Dependency Validation
- Epic 3: Visibility and Operational Control

## Architecture References

- [Architecture Overview](../architecture.md)

## UX References

- [Front-end Spec: Plugin Catalog & Operational Health](../front-end-spec.md#15-plugin-catalog--operational-health)
- [Mockup: Plugins](../mockups/plugins.html)

## Dev Notes

- Reuse existing activity history and health-monitoring patterns where possible.
- Distinguish configuration failures from runtime degradation in the status model.
