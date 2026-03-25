# Story 7.5: MCP Server for IDE and Tool Integration

**Epic:** Epic 7 - Local AI/ML Integration

**As a** developer using Cursor, Claude, or other MCP-compatible clients,  
**I want** AiTask to expose an MCP (Model Context Protocol) server,  
**so that** AiTask context (tasks, projects, repositories) can be used by those tools.

## Status

Done

## Acceptance Criteria

1. An MCP server (or equivalent) is available to connect AiTask with Cursor, Claude, or other MCP-compatible clients.
2. The MCP server exposes relevant AiTask context (e.g., task descriptions, project info, repository paths).
3. Configuration for MCP server enablement and connection details is available in settings.
4. The server operates securely and does not expose credentials or sensitive data.
5. Documentation or in-app help describes how to connect external clients.

## Requirements Mapping

- AI-5: System should expose an MCP server to enable integration with other AI tools and IDEs

## Dependencies

- Story 7.1: Local LLM Configuration

## Architecture References

- [Component Architecture: Integration](../architecture.md)

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Mockup: AI Studio](../mockups/ai-studio.html)

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew :core:test :desktop-app:test`

### Completion Notes List

- Added a persisted MCP bridge configuration with enablement and local port control.
- Implemented a localhost-only bridge service that exposes sanitized project, task, and repository context through HTTP and JSON-RPC-style endpoints.
- Wired the Settings screen with MCP enable/disable, port control, connection guidance, and status/feedback messaging.
- Started and stopped the bridge from desktop app launch and shutdown so the connection details stay current when the app runs.

### File List

- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/McpServerConfiguration.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/repository/McpServerConfigurationRepository.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/McpBridgeService.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/McpServerConfigurationEntity.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/McpServerConfigurationRepositoryImpl.kt` (new)
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/mcp/DefaultMcpBridgeService.kt` (new)
- `taskmanager/core/src/main/resources/db/migration/V17__add_mcp_server_configurations.sql` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/data/repository/McpServerConfigurationRepositoryImplTest.kt` (new)
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/mcp/DefaultMcpBridgeServiceTest.kt` (new)
- `taskmanager/core/build.gradle.kts` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/TaskManagerApp.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/PlaceholderViews.kt` (modified)
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/SettingsViewModel.kt` (modified)
- `taskmanager/desktop-app/src/test/kotlin/com/aitask/desktop/ui/viewmodel/SettingsViewModelTest.kt` (modified)

### Change Log

| Date | Description |
|------|-------------|
| 2026-03-25 | Implemented the MCP bridge settings, localhost bridge service, startup wiring, and sanitized context endpoints for story 7.5. |
| 2026-03-25 | Verified `:core:test` and `:desktop-app:test` after adding bridge persistence, service, UI, and test coverage. |

## QA Results

### Review Date: 2026-03-25

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation delivers a localhost-only MCP-style HTTP bridge (`DefaultMcpBridgeService`) with persisted enablement and port (`McpServerConfigurationRepositoryImpl`, V17 migration), Settings UI (`McpBridgeCard` in `PlaceholderViews.kt`, `SettingsViewModel`), and lifecycle wiring. Context payloads map domain models to explicit DTOs and omit repository credentials; `DefaultMcpBridgeServiceTest` asserts sensitive fields (clone URL, token) are not present in HTTP responses. Repository and ViewModel tests cover persistence and save/sync behavior.

### Refactoring Performed

None (review-only).

### Compliance Check

- Coding Standards: ✓ Kotlin patterns and DI match surrounding modules.
- Project Structure: ✓ Core domain/infrastructure vs desktop UI separation preserved.
- Testing Strategy: ✓ Unit/integration-style tests for repository, bridge (HTTP client), and ViewModel.
- All ACs Met: ✓ AC1–5 satisfied (bridge equivalent, context, settings, safe exposure, in-app connection guide and safety notes).

### Improvements Checklist

- [ ] Consider adding HTTP POST tests for JSON-RPC (`initialize`, `tools/call`, `resources/read`) on `/mcp` for regression safety (GET `/mcp/context` and manifest are covered).

### Security Review

Binding to `127.0.0.1` limits exposure to the local machine. Context serialization avoids clone URLs and tokens; test explicitly checks absence of credential material in JSON. Residual risk: any process on the host can call the bridge while enabled—acceptable for local-dev MCP patterns; documented in manifest safety notes.

### Performance Considerations

Embedded Netty server started on sync; mutex serializes start/stop. No issues identified for expected single-user desktop use.

### Files Modified During Review

None.

### Gate Status

Gate: PASS → docs/qa/gates/7.5-mcp-server-integration.yml

Risk profile: Not generated for this review (low-risk feature; optional follow-up).

NFR assessment: Security PASS (sanitization + loopback); reliability PASS (error paths on bad JSON-RPC payload); maintainability PASS (coverage adequate; optional POST tests in improvements checklist).

### Recommended Status

✓ Ready for Done (story owner may set Status to Done after confirmation).
