# Story 5.3: Environment Check Templates

**Epic:** Epic 5 - Pre-Run Scripts and Environment Validation

**As a** developer,  
**I want** templates for common environment checks (runtime versions, env vars),  
**so that** I can quickly validate Node.js, Java, Python, or required variables without writing custom scripts.

## Status

Draft

## Acceptance Criteria

1. The system supports environment checks within pre-run scripts for Node.js, Java, and Python versions.
2. The system supports checking required environment variables.
3. The application provides templates for common checks (runtime versions, env vars, dependency presence).
4. Users can add template-based checks to their project with minimal configuration (e.g., target version, variable name).
5. Template checks produce clear pass/fail output with actionable messages when validation fails.

## Requirements Mapping

- PRE-3: Environment checks (Node, Java, Python, env vars)
- PRE-5: Templates for common checks

## Architecture References

- [Component Architecture: Workspace Management](../architecture.md#6-workspace-management-component)

## Tasks / Subtasks

- [ ] Task 1: Define environment check template model and registry
  - [ ] Create EnvironmentCheckTemplate (id, name, script generator, parameters)
  - [ ] Support: node_version, java_version, python_version, env_var, dependency_present
- [ ] Task 2: Implement template-to-script generation
  - [ ] Generate executable script from template + user params
  - [ ] Cross-platform: shell for macOS/Linux, batch for Windows
- [ ] Task 3: Add template selection UI in pre-run config
  - [ ] Template picker with parameter inputs
  - [ ] Preview generated script before save
- [ ] Task 4: Add unit tests for template generation and check logic
