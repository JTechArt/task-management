# AiTask Features v2 — Product Requirements

This document defines additional feature requirements for AiTask beyond the core feature set, with a focus on pre-run automation, methodology integration, AI/ML capabilities, and intelligent task automation.

---

## 1. Pre-Run Scripts

### Overview

Support configurable scripts that execute before launching a task in an IDE or running automated workflows. Similar to Cursor's pre-run hooks, this enables environment validation and workspace preparation.

### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| PRE-1 | The system MUST allow users to configure project- or task-level pre-run scripts. | Must Have |
| PRE-2 | Pre-run scripts MUST execute before the selected IDE is launched for a task. | Must Have |
| PRE-3 | The system MUST support environment checks within pre-run scripts (e.g., Node.js version, Java version, Python version, required environment variables). | Must Have |
| PRE-4 | Pre-run scripts MUST be configurable per repository or project module. | Should Have |
| PRE-5 | The system SHOULD provide templates for common checks (runtime versions, env vars, dependency presence). | Could Have |
| PRE-6 | Pre-run script failures SHOULD block IDE launch and surface clear error messages to the user. | Must Have |

### Acceptance Criteria

- **AC-PRE-1**: User can define one or more pre-run scripts per project/repository.
- **AC-PRE-2**: When "Open in IDE" is triggered, pre-run scripts execute first; on success, IDE launches; on failure, user sees the error and IDE does not launch.
- **AC-PRE-3**: Pre-run scripts support checking Node.js, Java, Python versions and required environment variables.

---

## 2. BMAD Methodology Integration

### Overview

Integrate the BMAD (BMAD-Method) approach into AiTask so that when a user selects BMAD for a project or task, the workspace is automatically configured with BMAD tooling and recommended tools are pre-selected.

### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| BMAD-1 | The system MUST support BMAD as a selectable methodology/framework for projects and tasks. | Must Have |
| BMAD-2 | When BMAD is selected, the system MUST inject BMAD setup (config, tasks, templates) into the task workspace. | Must Have |
| BMAD-3 | BMAD-selected projects MUST have BMAD-recommended tools automatically selected (e.g., agents, workflows). | Must Have |
| BMAD-4 | BMAD integration SHOULD be configurable at project level with override capability at task level. | Should Have |
| BMAD-5 | The system SHOULD surface BMAD tool selection in the project/task configuration UI. | Should Have |

### Acceptance Criteria

- **AC-BMAD-1**: User can select "BMAD" as the methodology for a project or task from a dropdown or configuration panel.
- **AC-BMAD-2**: Upon selection, BMAD setup files (e.g., `.bmad-core`, `AGENTS.md`) are copied or linked into the workspace.
- **AC-BMAD-3**: BMAD-recommended tools (agents, checklists, tasks) appear pre-selected in the project/task configuration.
- **AC-BMAD-4**: User can customize which BMAD tools are active per project or task.

---

## 3. Local AI/ML Integration

### Overview

Enable local AI/ML usage via Llama and compatible models for text generation, descriptions, and assistive tasks without relying solely on cloud services.

### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| AI-1 | The system MUST support configuration of a local LLM (e.g., Llama) for use within AiTask. | Must Have |
| AI-2 | The system SHOULD use local models to generate task descriptions, summaries, or contextual text. | Should Have |
| AI-3 | When integrated with external services (Git, Slack, etc.), the system SHOULD support LLM-assisted operations such as commit message suggestions, PR description generation, and comment drafting. | Should Have |
| AI-4 | The system MUST support building and extending agents that leverage local or remote LLMs. | Must Have |
| AI-5 | The system SHOULD expose an MCP (Model Context Protocol) server to enable integration with other AI tools and IDEs. | Should Have |

### Acceptance Criteria

- **AC-AI-1**: User can configure a local Llama (or compatible) endpoint in AiTask settings.
- **AC-AI-2**: AiTask can generate task descriptions or summaries using the configured local model.
- **AC-AI-3**: With service integrations enabled, LLM can suggest commit messages, PR descriptions, or comments.
- **AC-AI-4**: Agents built on top of the AI layer can be defined and invoked from within AiTask.
- **AC-AI-5**: An MCP server (or equivalent) is available to connect AiTask with Cursor, Claude, or other MCP-compatible clients.

---

## 4. GEPPA (Prompt Optimization)

### Overview

Integrate GEPPA or equivalent prompt-optimization tooling to improve the quality and consistency of prompts used in AI-assisted workflows.

### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| GEPPA-1 | The system MUST support GEPPA (or equivalent) integration for prompt optimization. | Must Have |
| GEPPA-2 | Prompts used for AI-assisted features (agents, task generation, commits, etc.) SHOULD be optimizable via GEPPA. | Should Have |
| GEPPA-3 | The system SHOULD persist optimized prompts per project or globally for reuse. | Should Have |

### Acceptance Criteria

- **AC-GEPPA-1**: User can enable GEPPA integration in AiTask settings.
- **AC-GEPPA-2**: Prompts sent to LLMs can be routed through GEPPA for optimization before use.
- **AC-GEPPA-3**: Optimized prompts can be saved and reused across projects or tasks.

---

## 5. AI Tools Integration (Codex, Claude)

### Overview

Integrate with AI coding assistants such as Codex and Claude, including support for terminal-based or CLI workflows, so users can invoke these tools from within AiTask's context.

### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| AITOOL-1 | The system MUST support integration with Codex CLI for AI-assisted terminal workflows. | Must Have |
| AITOOL-2 | The system MUST support integration with Claude (e.g., Claude CLI or API) for AI-assisted workflows. | Must Have |
| AITOOL-3 | AI tools SHOULD receive task context (project, task description, repository) when invoked from AiTask. | Should Have |
| AITOOL-4 | The system SHOULD provide a unified way to launch or invoke configured AI tools (Codex, Claude) from the task UI. | Should Have |

### Acceptance Criteria

- **AC-AITOOL-1**: User can configure Codex CLI path and credentials; AiTask can invoke Codex with task context.
- **AC-AITOOL-2**: User can configure Claude; AiTask can invoke Claude with task context.
- **AC-AITOOL-3**: When AI tool is invoked, it receives project name, task description, and repository path.
- **AC-AITOOL-4**: Task detail screen includes actions (e.g., "Run with Codex", "Run with Claude") when tools are configured.

---

## 6. AI-Powered Task Automation

### Overview

Enable an intelligent automation layer that combines AI tools, GEPPA-optimized prompts, and local/cloud LLMs to generate and execute task-solving approaches from natural language task descriptions.

### Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| AUTO-1 | The system MUST provide an in-app agent builder or agent configuration for creating custom automation workflows. | Must Have |
| AUTO-2 | The system MUST combine AI tools (Codex, Claude), GEPPA, and LLM (Llama or cloud) to automate task-solving workflows. | Must Have |
| AUTO-3 | Given a task description, the system MUST be able to generate a proposed approach (plan, steps, tools) for solving the task. | Must Have |
| AUTO-4 | The system SHOULD support execution of generated approaches with user approval (e.g., review plan before run). | Should Have |
| AUTO-5 | The system SHOULD log automation runs and outcomes for traceability. | Should Have |

### Acceptance Criteria

- **AC-AUTO-1**: User can define agents or workflows in the agent builder and associate them with projects or task types.
- **AC-AUTO-2**: User creates a task with a description; the system uses AI + GEPPA + LLM to produce a solving approach.
- **AC-AUTO-3**: The generated approach is presented to the user (steps, tools, prompts) before execution.
- **AC-AUTO-4**: User can approve, modify, or reject the generated approach.
- **AC-AUTO-5**: Automation runs are logged with input task, generated plan, and result status.

---

## 7. Priority Legend

| Priority | Meaning |
|----------|---------|
| Must Have | Core functionality; required for release. |
| Should Have | Important; planned for release if feasible. |
| Could Have | Desirable; may be deferred to later release. |

---

## 8. Dependencies Between Features

```
Pre-Run Scripts ──► IDE Integration (from core features)
BMAD Integration ──► Project/Task configuration, Rule Management
Local AI/ML ──► AI Automation, AI Tools Integration, GEPPA
GEPPA ──► AI Automation, Local AI/ML
AI Tools Integration ──► AI Automation
AI Automation ──► Local AI/ML, GEPPA, AI Tools Integration
```

---

## 9. Glossary

| Term | Definition |
|------|------------|
| BMAD | BMAD-Method: a structured methodology for product/development workflows with agents, tasks, and checklists. |
| GEPPA | Tool/framework for prompt optimization. |
| MCP | Model Context Protocol: a standard for connecting AI applications with tools and context providers. |
| Pre-run script | Script executed before launching an IDE or running a workflow, typically for environment validation. |
