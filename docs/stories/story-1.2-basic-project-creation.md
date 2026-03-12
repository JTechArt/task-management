# Story 1.2: Basic Project Creation with Single Repository Configuration

**Epic:** Epic 1 - Foundation and First Task Launch Flow

**As a** developer,  
**I want** to create a project with its primary repository and workspace settings,  
**so that** AiTask can prepare task work for a real codebase.

## Acceptance Criteria

1. A user can create, edit, view, and archive a project with name, description, workspace path, and branch naming template.
2. A user can attach one primary repository to the project with provider, clone URL, and repository name or label.
3. A user can define at least one preferred IDE for the repository from the supported IDE list.
4. Project data persists between application sessions.
5. Validation prevents saving incomplete or clearly invalid project or repository configurations and gives actionable feedback.

## Architecture References

- [Component Architecture: Project Management](../architecture.md#1-project-management-component)
- [Database Schema: projects, repositories](../architecture.md#database-schema)

## UX References

- [Flow 3: Project and Repository Setup](../front-end-spec.md#flow-3-project-and-repository-setup)
- [Screen Layouts: Project Detail](../front-end-spec.md#key-screen-layouts)
- [Visual Mockup: Projects List / Creation](../mockups/projects.html)
- [Visual Mockup: Project Detail](../mockups/project-detail.html)
