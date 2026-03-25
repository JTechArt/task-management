# Story 7.5: MCP Server for IDE and Tool Integration

**Epic:** Epic 7 - Local AI/ML Integration

**As a** developer using Cursor, Claude, or other MCP-compatible clients,  
**I want** AiTask to expose an MCP (Model Context Protocol) server,  
**so that** AiTask context (tasks, projects, repositories) can be used by those tools.

## Status

Approved

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
