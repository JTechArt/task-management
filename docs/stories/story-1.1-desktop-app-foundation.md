# Story 1.1: Desktop App Foundation and Persistence Bootstrap

**Epic:** Epic 1 - Foundation and First Task Launch Flow

**As a** developer,  
**I want** the desktop application to start reliably with its local services configured,  
**so that** I can use AiTask as an installable product rather than a prototype.

## Acceptance Criteria

1. The application launches into a stable desktop shell with navigation placeholders for dashboard, projects, tasks, rules, and settings.
2. The application initializes database connectivity and applies schema migrations automatically on startup or through a supported startup flow.
3. The application displays a clear success or failure state when startup dependencies such as the database are unavailable.
4. The application provides a simple canary experience, such as a home screen or status view, confirming that the desktop app, persistence layer, and base navigation are working.
5. The application logs startup and persistence initialization events without exposing secrets.

## Architecture References

- [Component Architecture: Project Management](../architecture.md#1-project-management-component)
- [Component Architecture: Task Management](../architecture.md#2-task-management-component)
- [Database Schema: projects, repositories, tasks](../architecture.md#database-schema)
- [Project Structure: taskmanager/](../architecture.md#project-structure)

## UX References

- [Flow 1: Task Launch (Primary MVP Flow)](../front-end-spec.md#flow-1-task-launch-primary-mvp-flow)
- [Screen Layouts: Dashboard, Project Detail, Task Launch Flow](../front-end-spec.md#key-screen-layouts)

