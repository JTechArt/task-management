# User Stories Index

This directory contains individual user stories extracted from the 12 epics defined in the Product Requirements Document (PRD).

> Recommended implementation order: keep epic numbering stable for traceability, but deliver Epics 1-6, then Epic 11, then Epics 7-10, and finally Epic 12.

## Epic 1: Foundation and First Task Launch Flow

| Story | Title | File |
|-------|-------|------|
| 1.1 | Desktop App Foundation and Persistence Bootstrap | [story-1.1-desktop-app-foundation.md](./story-1.1-desktop-app-foundation.md) |
| 1.2 | Basic Project Creation with Single Repository Configuration | [story-1.2-basic-project-creation.md](./story-1.2-basic-project-creation.md) |
| 1.3 | Task Management for the Core Workflow | [story-1.3-task-management.md](./story-1.3-task-management.md) |
| 1.4 | Workspace Generation for a Selected Task | [story-1.4-workspace-generation.md](./story-1.4-workspace-generation.md) |
| 1.5 | Launch Task Workspace in Configured IDE | [story-1.5-ide-launch.md](./story-1.5-ide-launch.md) |

## Epic 2: Multi-Repository Git Automation and Rule Application

| Story | Title | File |
|-------|-------|------|
| 2.1 | Multi-Repository Project Configuration | [story-2.1-multi-repository-configuration.md](./story-2.1-multi-repository-configuration.md) |
| 2.2 | Task Workspace Repository Selection | [story-2.2-repository-selection.md](./story-2.2-repository-selection.md) |
| 2.3 | Task Branch Automation and Repository Validation | [story-2.3-branch-automation.md](./story-2.3-branch-automation.md) |
| 2.4 | Secure Git Credentials and Access Configuration | [story-2.4-git-credentials.md](./story-2.4-git-credentials.md) |
| 2.5 | Rule Set Management Across Projects and Repositories | [story-2.5-rule-set-management.md](./story-2.5-rule-set-management.md) |
| 2.6 | Rule-Aware Workspace Preparation | [story-2.6-rule-aware-workspace.md](./story-2.6-rule-aware-workspace.md) |

## Epic 3: Visibility and Operational Control

| Story | Title | File |
|-------|-------|------|
| 3.1 | Dashboard Overview for Active Work | [story-3.1-dashboard-overview.md](./story-3.1-dashboard-overview.md) |
| 3.2 | Activity History and Recent Events | [story-3.2-activity-history.md](./story-3.2-activity-history.md) |
| 3.3 | Health Monitoring and Connectivity Status | [story-3.3-health-monitoring.md](./story-3.3-health-monitoring.md) |
| 3.4 | Workspace Retention and Cleanup Controls | [story-3.4-workspace-retention.md](./story-3.4-workspace-retention.md) |
| 3.5 | Enhanced Management Views and Filtering | [story-3.5-enhanced-management-views.md](./story-3.5-enhanced-management-views.md) |

## Epic 4: External Integrations, Portability, and Distribution

| Story | Title | File |
|-------|-------|------|
| 4.1 | Slack Notification Configuration and Delivery | [story-4.1-slack-notifications.md](./story-4.1-slack-notifications.md) |
| 4.2 | OAuth-Based External Integration Access | [story-4.2-oauth-integration.md](./story-4.2-oauth-integration.md) |
| 4.3 | Import and Export of Projects, Tasks, and Rules | [story-4.3-import-export.md](./story-4.3-import-export.md) |
| 4.4 | Backup and Restore for Local Recovery | [story-4.4-backup-restore.md](./story-4.4-backup-restore.md) |
| 4.5 | Cross-Platform Packaging and Installer Readiness | [story-4.5-cross-platform-packaging.md](./story-4.5-cross-platform-packaging.md) |

## Epic 5: Pre-Run Scripts and Environment Validation

| Story | Title | File |
|-------|-------|------|
| 5.1 | Pre-Run Script Configuration | [story-5.1-pre-run-script-configuration.md](./story-5.1-pre-run-script-configuration.md) |
| 5.2 | Per-Module and Per-Repository Pre-Run Scripts | [story-5.2-per-module-pre-run-scripts.md](./story-5.2-per-module-pre-run-scripts.md) |
| 5.3 | Environment Check Templates and Validation Output | [story-5.3-environment-check-templates.md](./story-5.3-environment-check-templates.md) |

## Epic 6: BMAD Methodology Integration

| Story | Title | File |
|-------|-------|------|
| 6.1 | BMAD as Selectable Methodology | [story-6.1-bmad-methodology-selection.md](./story-6.1-bmad-methodology-selection.md) |
| 6.2 | BMAD Workspace Asset Injection | [story-6.2-bmad-workspace-injection.md](./story-6.2-bmad-workspace-injection.md) |
| 6.3 | BMAD Tool Pre-Selection and Project Templates | [story-6.3-bmad-tool-pre-selection.md](./story-6.3-bmad-tool-pre-selection.md) |
| 6.4 | Task-Level BMAD Override and Regeneration Controls | [story-6.4-bmad-task-level-override.md](./story-6.4-bmad-task-level-override.md) |

## Epic 7: Local AI/ML Integration

| Story | Title | File |
|-------|-------|------|
| 7.1 | Local LLM Configuration and Health Checks | [story-7.1-local-llm-configuration.md](./story-7.1-local-llm-configuration.md) |
| 7.2 | Local LLM for Task Descriptions and Summaries | [story-7.2-llm-assisted-generation.md](./story-7.2-llm-assisted-generation.md) |
| 7.3 | LLM-Assisted Git Operations | [story-7.3-llm-assisted-git-operations.md](./story-7.3-llm-assisted-git-operations.md) |
| 7.4 | Local AI Support for Agent Builder | [story-7.4-agent-builder-llm.md](./story-7.4-agent-builder-llm.md) |
| 7.5 | MCP Server for IDE and Tool Integration | [story-7.5-mcp-server-integration.md](./story-7.5-mcp-server-integration.md) |

## Epic 8: GEPPA (Prompt Optimization)

| Story | Title | File |
|-------|-------|------|
| 8.1 | GEPPA Service Integration | [story-8.1-geppa-integration.md](./story-8.1-geppa-integration.md) |
| 8.2 | Prompt Optimization and Persistence | [story-8.2-prompt-routing-and-optimization.md](./story-8.2-prompt-routing-and-optimization.md) |
| 8.3 | GEPPA Integration with AI Features | [story-8.3-optimized-prompt-persistence.md](./story-8.3-optimized-prompt-persistence.md) |

## Epic 9: AI Tools Integration (Codex, Claude)

| Story | Title | File |
|-------|-------|------|
| 9.1 | Codex CLI Integration | [story-9.1-codex-cli-integration.md](./story-9.1-codex-cli-integration.md) |
| 9.2 | Claude Integration | [story-9.2-claude-integration.md](./story-9.2-claude-integration.md) |
| 9.3 | Unified AI Tool Launch Experience | [story-9.3-unified-ai-tool-launch.md](./story-9.3-unified-ai-tool-launch.md) |

## Epic 10: AI-Powered Task Automation

| Story | Title | File |
|-------|-------|------|
| 10.1 | Agent Builder and Custom Workflow Configuration | [story-10.1-agent-builder-and-workflows.md](./story-10.1-agent-builder-and-workflows.md) |
| 10.2 | AI-Generated Task Approach Suggestions | [story-10.2-ai-generated-task-approach.md](./story-10.2-ai-generated-task-approach.md) |
| 10.3 | Automation Execution for Approved Workflows | [story-10.3-automation-execution-and-logging.md](./story-10.3-automation-execution-and-logging.md) |
| 10.4 | Automation Run Logging and History | [story-10.4-automation-run-logging.md](./story-10.4-automation-run-logging.md) |

## Epic 11: Plugin Management and Add-on Framework

| Story | Title | File |
|-------|-------|------|
| 11.1 | Plugin Framework and Lifecycle Contracts | [story-11.1-plugin-framework-and-lifecycle-contracts.md](./story-11.1-plugin-framework-and-lifecycle-contracts.md) |
| 11.2 | Plugin Catalog, Install, Attach, and Remove | [story-11.2-plugin-catalog-and-lifecycle-management.md](./story-11.2-plugin-catalog-and-lifecycle-management.md) |
| 11.3 | Plugin Configuration and Dependency Validation | [story-11.3-plugin-configuration-and-dependency-validation.md](./story-11.3-plugin-configuration-and-dependency-validation.md) |
| 11.4 | Plugin Status, Health, and Operational Visibility | [story-11.4-plugin-status-health-and-visibility.md](./story-11.4-plugin-status-health-and-visibility.md) |

## Epic 12: AI Slack Channel Analyzer

| Story | Title | File |
|-------|-------|------|
| 12.1 | Slack Analysis Source and Schedule Configuration | [story-12.1-slack-analysis-source-and-schedule-configuration.md](./story-12.1-slack-analysis-source-and-schedule-configuration.md) |
| 12.2 | Incremental Slack Analysis Execution | [story-12.2-incremental-slack-analysis-execution.md](./story-12.2-incremental-slack-analysis-execution.md) |
| 12.3 | Daily, Per-Channel, and Per-Topic Summaries with References | [story-12.3-slack-summaries-with-references.md](./story-12.3-slack-summaries-with-references.md) |
| 12.4 | Analysis Run History and Operational Dashboard | [story-12.4-analysis-run-history-and-dashboard.md](./story-12.4-analysis-run-history-and-dashboard.md) |
| 12.5 | Summary Retention, Archive, and Automation Handoffs | [story-12.5-summary-retention-archive-and-handoffs.md](./story-12.5-summary-retention-archive-and-handoffs.md) |

## Summary

- **Total Stories:** 52
- **Epic 1:** 5 stories
- **Epic 2:** 6 stories
- **Epic 3:** 5 stories
- **Epic 4:** 5 stories
- **Epic 5:** 3 stories
- **Epic 6:** 4 stories
- **Epic 7:** 5 stories
- **Epic 8:** 3 stories
- **Epic 9:** 3 stories
- **Epic 10:** 4 stories
- **Epic 11:** 4 stories
- **Epic 12:** 5 stories

## Related Documentation

- [Epic Definitions](../prd/)
- [Product Requirements Document](../prd.md)
- [Architecture Documentation](../architecture.md)
- [Front-End Specification](../front-end-spec.md)
- [Mockup Gallery](../mockups/index.html)
