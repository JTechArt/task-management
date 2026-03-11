# Story 2.6: Rule-Aware Workspace Preparation

**Epic:** Epic 2 - Multi-Repository Git Automation and Rule Application

**As a** developer,  
**I want** AiTask to apply the correct rules when preparing my task workspace,  
**so that** my IDE and AI tools start with the right project context automatically.

## Acceptance Criteria

1. During task launch preparation, the application determines the effective rules for the selected project, repositories, IDE, and AI-tool context.
2. The application applies those effective rules to the workspace in a way compatible with the selected IDE or AI tooling.
3. The launch flow shows which rule sets were applied or skipped.
4. If rule application partially fails, the application reports the issue without masking repository or workspace preparation status.
5. Successful rule application is recorded in task activity history.

## Architecture References

- [Component Architecture: Rule Management](../architecture.md#5-rule-management-component)
- [Component Architecture: Workspace Management](../architecture.md#6-workspace-management-component)

## UX References

- [Task Launch Flow (Stepper)](../front-end-spec.md#3-task-launch-flow-modal-or-stepper)
- [Rule Management screen](../front-end-spec.md#4-rule-management)

