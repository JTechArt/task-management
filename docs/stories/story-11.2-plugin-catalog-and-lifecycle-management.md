# Story 11.2: Plugin Catalog, Install, Attach, and Remove

**Epic:** Epic 11 - Plugin Management and Add-on Framework

**As a** developer,  
**I want** to manage optional add-ons from within AiTask,  
**so that** I can enable only the capabilities relevant to my workflow.

## Status

Draft

## Acceptance Criteria

1. A user can view installed plugins and their current attachment or enablement state.
2. A user can install, attach, detach, disable, or remove supported plugins through a plugin management experience.
3. The UI clearly distinguishes core features from optional plugins.
4. Plugin management actions confirm success or failure with clear status feedback.
5. Plugin lifecycle actions are recorded in application history.

## Requirements Mapping

- FR29: Plugin installation and attachment model
- FR30: Plugin management views and status visibility
- FR33: Optional plugins cannot break core usage flows

## Dependencies

- Story 11.1: Plugin Framework and Lifecycle Contracts
- Epic 3: Visibility and Operational Control

## Architecture References

- [Architecture Overview](../architecture.md)

## UX References

- [Front-end Spec: Plugin Catalog & Operational Health](../front-end-spec.md#15-plugin-catalog--operational-health)
- [Mockup: Plugins](../mockups/plugins.html)

## Dev Notes

- Treat install, attach, enable, disable, detach, and remove as distinct lifecycle actions with clear user feedback.
- Plugin lifecycle actions should emit history events for later operational troubleshooting.
