# Story 9.2: Claude Integration

**Epic:** Epic 9 - AI Tools Integration (Codex, Claude)

**As a** developer,  
**I want** to configure and invoke Claude (CLI or API) from AiTask with task context,  
**so that** I can use Claude for AI-assisted workflows within my task workflow.

## Status

Draft

## Acceptance Criteria

1. A user can configure Claude (CLI path or API endpoint and credentials) in AiTask settings.
2. The application validates Claude availability when configured.
3. When "Run with Claude" is invoked from a task, AiTask passes project name, task description, and repository path as context.
4. The application launches Claude with the task workspace as the working directory (for CLI) or passes context to API.
5. Invocation success or failure is reported to the user with clear feedback.

## Requirements Mapping

- AITOOL-2 (Must Have): Claude integration for AI-assisted workflows
- AITOOL-3 (Should Have): Task context passed to AI tools

## Dependencies

- Epic 1: Task context and workspace structure
- Epic 7: Credential/API key storage patterns (OAuth or secure config)

## Dev Notes

- Claude may be invoked via CLI or API; design for both modes.
- API mode requires secure storage of API keys; align with Epic 4 OAuth/credentials patterns.
- Context injection format should be documented for extensibility.
