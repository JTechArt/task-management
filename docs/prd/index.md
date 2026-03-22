# AiTask Product Requirements Document

## Overview

AiTask is a cross-platform desktop application that streamlines developer workflows by automating task workspace creation, repository preparation, branch setup, and IDE launch with AI-assisted development rules.

## Goals

- Reduce per-task development environment setup time by automating workspace creation, repository preparation, branch setup, and IDE launch.
- Centralize project, repository, task, rule, and IDE configuration in a single desktop application.
- Support multi-repository development workflows without forcing teams into a single repository structure or Git provider.
- Make AI-assisted development repeatable by applying project and IDE-specific rules automatically.
- Improve visibility into active work through task lifecycle tracking, dashboard metrics, and project-level analytics.
- Provide secure, cross-platform desktop operations for Git credentials, OAuth-based integrations, and data portability.

## Epics

| Epic | Title | Stories |
|------|-------|---------|
| 1 | [Foundation and First Task Launch Flow](./epic-1-foundation-and-first-task-launch-flow.md) | 1.1 – 1.5 |
| 2 | [Multi-Repository Git Automation and Rule Application](./epic-2-multi-repository-git-automation-and-rule-application.md) | 2.1 – 2.6 |
| 3 | [Visibility and Operational Control](./epic-3-visibility-and-operational-control.md) | 3.1 – 3.5 |
| 4 | [External Integrations, Portability, and Distribution](./epic-4-external-integrations-portability-and-distribution.md) | 4.1 – 4.5 |
| 5 | [Pre-Run Scripts and Environment Validation](./epic-5-pre-run-scripts.md) | 5.1 – 5.3 |
| 6 | [BMAD Methodology Integration](./epic-6-bmad-methodology-integration.md) | 6.1 – 6.4 |
| 7 | [Local AI/ML Integration](./epic-7-local-ai-ml-integration.md) | 7.1 – 7.5 |
| 8 | [GEPPA (Prompt Optimization)](./epic-8-geppa-prompt-optimization.md) | 8.1 – 8.3 |
| 9 | [AI Tools Integration (Codex, Claude)](./epic-9-ai-tools-integration.md) | 9.1 – 9.3 |
| 10 | [AI-Powered Task Automation](./epic-10-ai-powered-task-automation.md) | 10.1 – 10.4 |
| 11 | [Plugin Management and Add-on Framework](./epic-11-plugin-management-and-addon-framework.md) | 11.1 – 11.4 |
| 12 | [AI Slack Channel Analyzer](./epic-12-ai-slack-channel-analyzer.md) | 12.1 – 12.5 |

> Recommended delivery order: keep epic numbering as-is for traceability, but implement Epics 1-6, then Epic 11, then Epics 7-10, and finally Epic 12.

## Reference Documents

- **Architecture:** [docs/architecture.md](../architecture.md)
- **Front-End Spec:** [docs/front-end-spec.md](../front-end-spec.md)
- **Full PRD:** [docs/prd.md](../prd.md)
