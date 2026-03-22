# Story 9.1: Codex CLI Integration

**Epic:** Epic 9 - AI Tools Integration (Codex, Claude)

**As a** developer,  
**I want** to configure and invoke Codex CLI from AiTask with task context,  
**so that** I can run AI-assisted terminal workflows without leaving the task context.

## Status

Draft

## Acceptance Criteria

1. A user can configure Codex CLI path and credentials (if required) in AiTask settings.
2. The application validates Codex CLI availability when configured.
3. When "Run with Codex" is invoked from a task, AiTask passes project name, task description, and repository path as context.
4. The application launches Codex with the task workspace as the working directory.
5. Invocation success or failure is reported to the user with clear feedback.

## Requirements Mapping

- AITOOL-1 (Must Have): Codex CLI integration for AI-assisted terminal workflows
- AITOOL-3 (Should Have): Task context passed to AI tools

## Dependencies

- Epic 1: IDE Launch flow (similar invocation pattern)
- Epic 5: Pre-Run Scripts (optional: run pre-run before Codex)

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Mockup: AI Studio](../mockups/ai-studio.html)
- [Mockup: Tasks](../mockups/tasks.html)

## Dev Notes

- Codex CLI typically runs in terminal; design for subprocess launch with context injection.
- Consider environment variable or config file for passing task context to Codex.
- Support both configured path and system PATH lookup.
