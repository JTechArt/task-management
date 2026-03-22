# Epic 9: AI Tools Integration (Codex, Claude)

## Epic Goal

Integrate with AI coding assistants such as Codex and Claude so users can invoke these tools from within AiTask's context. Task context (project, task description, repository) is passed to the AI tools, enabling contextual assistance during development workflows. Support includes terminal-based and CLI workflows.

## Requirements Mapping (from docs/features-v2.md)

- **AITOOL-1:** System must support integration with Codex CLI for AI-assisted terminal workflows
- **AITOOL-2:** System must support integration with Claude (e.g., Claude CLI or API) for AI-assisted workflows
- **AITOOL-3:** AI tools should receive task context when invoked from AiTask
- **AITOOL-4:** System should provide a unified way to launch or invoke configured AI tools from the task UI

## Dependencies

- **Depends on:** Core task and project management (Epic 1), Epic 11 (plugin framework)
- **Enables:** Epic 10 (AI-Powered Task Automation)

## Architecture References

- [Architecture Overview](../architecture.md)
- [Plugin and Tool Launch Integration Patterns](../architecture.md)

## UX References

- Task detail screen
- Settings / AI tools configuration

---

## Story 9.1: Codex CLI Integration

**As a** developer,  
**I want** to configure and invoke Codex CLI from AiTask with task context,  
**so that** I can use Codex for AI-assisted terminal workflows within my current task.

### Acceptance Criteria

1. A user can configure the Codex CLI path and credentials in AiTask settings.
2. A user can invoke Codex from the task detail screen when Codex is configured.
3. When Codex is invoked, it receives project name, task description, and repository path as context.
4. The application validates Codex availability before presenting the invoke action.
5. Failed invocations display actionable error messages to the user.

---

## Story 9.2: Claude Integration

**As a** developer,  
**I want** to configure and invoke Claude (CLI or API) from AiTask with task context,  
**so that** I can use Claude for AI-assisted workflows within my current task.

### Acceptance Criteria

1. A user can configure Claude (CLI path or API key) in AiTask settings.
2. A user can invoke Claude from the task detail screen when Claude is configured.
3. When Claude is invoked, it receives project name, task description, and repository path as context.
4. The application validates Claude availability before presenting the invoke action.
5. Failed invocations display actionable error messages to the user.

---

## Story 9.3: Unified AI Tools Launch and Context Passing

**As a** developer,  
**I want** a unified way to launch configured AI tools with full task context,  
**so that** I can quickly switch between Codex, Claude, or other tools from a single task view.

### Acceptance Criteria

1. The task detail screen shows "Run with Codex", "Run with Claude", or equivalent actions for each configured tool.
2. Actions are only shown when the respective tool is properly configured.
3. All AI tool invocations pass consistent context: project name, task description, repository path, workspace path.
4. The application records AI tool invocation in task activity history.
5. A user can configure which AI tools appear by default for new projects or tasks.
