# Story 1.1: Desktop App Foundation and Persistence Bootstrap

## Status

Done

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
- [Visual Mockup: Dashboard](../mockups/dashboard.html)

## QA Results

### Review Date: 2025-03-12

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation delivers a stable desktop shell with navigation, database bootstrap, and a status view. Core architecture follows DI patterns (config loader, bootstrapper injected), and Compose UI is used consistently. Code quality is generally good with clear separation of concerns.

### Refactoring Performed

No refactoring was performed during this review. Recommendations are documented below for dev consideration.

### Compliance Check

- **Coding Standards**: ✓ Kotlin conventions followed; type declarations, verb-named functions, constants used.
- **Project Structure**: ✓ Files under `taskmanager/desktop-app/` and `taskmanager/core/` align with architecture.
- **Testing Strategy**: ⚠ Partial – unit tests exist for AppLauncher and EnvConfigLoader; no StatusView or navigation tests; database bootstrap path untested (would require testcontainers).
- **All ACs Met**: ✓ All five acceptance criteria are addressed; AC5 has a noted concern (see Security Review).

### Improvements Checklist

- [ ] Mask or remove Database User from StatusView System Information card (AC5 / security)
- [ ] Add unit tests for StatusView displaying connected vs disconnected states
- [ ] Add navigation smoke test or UI test for sidebar items
- [ ] Consider adding integration test for successful database bootstrap (testcontainers)
- [ ] Consider clarifying `redactSensitiveValues()` – it excludes password but includes user; name may be misleading

### Security Review

**AC5 compliance**: Logs correctly avoid exposing secrets – `redactSensitiveValues()` excludes password from log output.

**Finding**: StatusView displays `config.database.user` in the System Information card. AC5 states “without exposing secrets,” and the PRD says to avoid exposing credentials in “logs, exports, or UI surfaces.” Displaying the database username could expose connection identity in screenshots or shared screens. **Recommendation**: Mask the username (e.g. `••••••`) or remove the Database User row from the status UI, unless there is an explicit requirement for debugging visibility.

### Performance Considerations

Database connection uses HikariCP with sensible pool defaults. Bootstrap is synchronous on startup; acceptable for initial launch. No obvious performance concerns.

### Files Modified During Review

None. Dev to update File List if one exists.

### Gate Status

Gate: CONCERNS → docs/qa/gates/1.1-desktop-app-foundation.yml

### Recommended Status

⚠ **Changes Required** – Address SEC-001 (Database User in UI) and consider test coverage improvements before marking Done.
