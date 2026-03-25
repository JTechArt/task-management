# Story 7.3: LLM-Assisted Git Operations (Commits, PRs, Comments)

**Epic:** Epic 7 - Local AI/ML Integration

**As a** developer with Git and optional external service integrations,  
**I want** the LLM to suggest commit messages, PR descriptions, or comments,  
**so that** I can maintain consistent, descriptive Git metadata with less manual writing.

## Status

Approved

## Acceptance Criteria

1. With Git integration enabled, the system can suggest commit messages based on staged changes or task context.
2. When integrated with external services (e.g., GitHub PR API), the system can generate PR descriptions and comment drafts.
3. Suggestions are clearly labeled as AI-generated and require user approval before use.
4. The feature gracefully degrades when LLM is unavailable or integration is not configured.
5. Activity or history records indicate when LLM suggestions were used.

## Requirements Mapping

- AI-3: LLM-assisted operations (commit message suggestions, PR description generation, comment drafting)

## Dependencies

- Story 7.1: Local LLM Configuration
- Epic 2: Git integration
- Epic 4: External integrations (OAuth for PR APIs)

## Architecture References

- [Component Architecture: Git Integration](../architecture.md)
- [Component Architecture: Integration](../architecture.md)

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Mockup: AI Studio](../mockups/ai-studio.html)
