# Epic 8: GEPPA (Prompt Optimization)

## Epic Goal

Integrate GEPPA or equivalent prompt-optimization tooling into AiTask so that prompts used for AI-assisted features (agents, task generation, commit messages, etc.) can be improved for quality and consistency. This epic enables users to optimize and persist prompts per project or globally for reuse across AI workflows.

## Requirements Mapping (from docs/features-v2.md)

- **GEPPA-1:** System must support GEPPA (or equivalent) integration for prompt optimization
- **GEPPA-2:** Prompts used for AI-assisted features should be optimizable via GEPPA
- **GEPPA-3:** System should persist optimized prompts per project or globally for reuse

## Dependencies

- **Depends on:** Epic 7 (Local AI/ML Integration), Epic 11 (plugin framework)
- **Enables:** Epic 10 (AI-Powered Task Automation)

## Architecture References

- [Architecture Overview](../architecture.md)
- [Plugin and AI Integration Patterns](../architecture.md)

## UX References

- Settings / AI configuration screens
- Agent builder and task automation workflows

---

## Story 8.1: GEPPA Integration Configuration

**As a** developer,  
**I want** to enable and configure GEPPA integration in AiTask,  
**so that** prompts sent to LLMs can be optimized before use.

### Acceptance Criteria

1. A user can enable or disable GEPPA integration in AiTask settings.
2. A user can configure the GEPPA endpoint or tool path when enabled.
3. When GEPPA is enabled, prompts sent to LLMs (local or cloud) can be routed through GEPPA for optimization before use.
4. The application provides clear feedback when GEPPA is unavailable or returns errors.
5. GEPPA configuration is stored securely and persists between sessions.

---

## Story 8.2: Prompt Optimization and Persistence

**As a** developer,  
**I want** to save and reuse optimized prompts,  
**so that** I can build a library of high-quality prompts for AI-assisted workflows.

### Acceptance Criteria

1. A user can save an optimized prompt from GEPPA for reuse.
2. Optimized prompts can be stored globally or per project.
3. A user can select a saved optimized prompt when configuring agents or AI workflows.
4. The application displays a list of saved prompts with names and scopes (global vs project).
5. A user can edit, rename, or delete saved prompts.

---

## Story 8.3: GEPPA Integration with AI Features

**As a** developer,  
**I want** AI-assisted features to use GEPPA-optimized prompts when configured,  
**so that** task generation, commit messages, and agent prompts benefit from optimization.

### Acceptance Criteria

1. Task description generation uses GEPPA-optimized prompts when GEPPA is enabled.
2. Commit message and PR description suggestions use optimized prompts when available.
3. Agent and workflow prompts can be associated with saved GEPPA-optimized prompts.
4. The application falls back gracefully when GEPPA is unavailable or optimization fails.
5. Optimization activity is logged for traceability without exposing prompt content in logs.
