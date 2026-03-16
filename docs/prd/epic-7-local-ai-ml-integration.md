# Epic 7: Local AI/ML Integration

## Epic Goal

Enable local AI/ML usage via Llama and compatible models for text generation, descriptions, and assistive tasks without relying solely on cloud services. This gives users privacy, cost control, and offline capability for AI-assisted workflows.

## Requirements Mapping

- **AI-1–AI-5:** Local LLM configuration, task/PR/commit generation, agent building, MCP server
- **NFR:** Security (credential handling), performance

## Dependencies

- Foundation for Epic 8 (GEPPA), Epic 9 (AI Tools), Epic 10 (AI Automation)
- May integrate with Epic 4 (OAuth) for cloud fallback

## Architecture References

- [Component Architecture: Integration](../../architecture.md#7-integration-component)
- [Security Architecture: Credential Management](../../architecture.md#credential-management)

---

## Story 7.1: Local LLM Configuration and Endpoint Setup

**As a** developer,  
**I want** to configure a local Llama (or compatible) endpoint in AiTask settings,  
**so that** I can use local AI for task descriptions and other text generation without cloud dependency.

### Acceptance Criteria

1. A user can configure a local Llama or compatible LLM endpoint in AiTask settings (URL, port, model name).
2. The application validates endpoint connectivity before saving configuration.
3. Connection failures and timeouts surface clear error messages.
4. LLM configuration is stored securely and not exposed in logs or UI beyond masked identifiers.
5. The application supports at least one local LLM backend (e.g., Ollama, LM Studio, or direct API).

---

## Story 7.2: Local LLM for Task Descriptions and Summaries

**As a** developer,  
**I want** AiTask to generate task descriptions or summaries using my configured local model,  
**so that** I can quickly create or enrich task content without switching tools.

### Acceptance Criteria

1. AiTask can generate task descriptions or summaries using the configured local model.
2. Generation is invoked from the task creation or edit flow with clear user control.
3. Generated text is presented as a suggestion that the user can accept, modify, or discard.
4. Generation failures do not block task creation and provide actionable feedback.
5. The feature is disabled or hidden when no LLM is configured.

---

## Story 7.3: LLM-Assisted Git Operations (Commits, PRs, Comments)

**As a** developer with Git and optional external service integrations,  
**I want** the LLM to suggest commit messages, PR descriptions, or comments,  
**so that** I can maintain consistent, descriptive Git metadata with less manual writing.

### Acceptance Criteria

1. With Git integration enabled, the system can suggest commit messages based on staged changes or task context.
2. When integrated with external services (e.g., GitHub PR API), the system can generate PR descriptions and comment drafts.
3. Suggestions are clearly labeled as AI-generated and require user approval before use.
4. The feature gracefully degrades when LLM is unavailable or integration is not configured.
5. Activity or history records indicate when LLM suggestions were used.

---

## Story 7.4: Agent Builder and LLM-Powered Agents

**As a** developer,  
**I want** to build and extend agents that leverage local or remote LLMs,  
**so that** I can automate workflows that require AI reasoning within AiTask.

### Acceptance Criteria

1. The system supports building and extending agents that leverage local or remote LLMs.
2. Agents can be defined with prompt templates, model selection, and invocation triggers.
3. Agent definitions are persisted and associated with projects or globally.
4. Agents can be invoked from the task UI or as part of automation flows.
5. Agent execution outcomes are logged for traceability.

---

## Story 7.5: MCP Server for IDE and Tool Integration

**As a** developer using Cursor, Claude, or other MCP-compatible clients,  
**I want** AiTask to expose an MCP (Model Context Protocol) server,  
**so that** AiTask context (tasks, projects, repositories) can be used by those tools.

### Acceptance Criteria

1. An MCP server (or equivalent) is available to connect AiTask with Cursor, Claude, or other MCP-compatible clients.
2. The MCP server exposes relevant AiTask context (e.g., task descriptions, project info, repository paths).
3. Configuration for MCP server enablement and connection details is available in settings.
4. The server operates securely and does not expose credentials or sensitive data.
5. Documentation or in-app help describes how to connect external clients.
