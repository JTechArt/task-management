# Story 2.1: Multi-Repository Project Configuration

**Epic:** Epic 2 - Multi-Repository Git Automation and Rule Application

**As a** developer,  
**I want** to configure multiple repositories within a project,  
**so that** AiTask can support real-world codebases that span several services or modules.

## Acceptance Criteria

1. A user can add, edit, archive, and remove multiple repositories within a project.
2. Each repository stores provider, clone URL, label, local role or purpose, and one or more preferred IDE options.
3. The application clearly distinguishes the primary repository from additional repositories when configured.
4. Repository configuration changes persist between sessions.
5. Validation prevents duplicate or invalid repository definitions within the same project.

## Architecture References

- [Component Architecture: Git Integration](../architecture.md#3-git-integration-component)
- [Integration Architecture: Git Provider Integration](../architecture.md#1-git-provider-integration)

## UX References

- [Flow 3: Project and Repository Setup](../front-end-spec.md#flow-3-project-and-repository-setup)

