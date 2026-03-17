# Story 8.2: Prompt Optimization and Persistence

**Epic:** Epic 8 - GEPPA (Prompt Optimization)

**As a** developer,  
**I want** to save and reuse optimized prompts,  
**so that** I can build a library of high-quality prompts for AI-assisted workflows.

## Status

Draft

## Acceptance Criteria

1. A user can save an optimized prompt from GEPPA for reuse.
2. Optimized prompts can be stored globally or per project.
3. A user can select a saved optimized prompt when configuring agents or AI workflows.
4. The application displays a list of saved prompts with names and scopes (global vs project).
5. A user can edit, rename, or delete saved prompts.

## Requirements Mapping

- GEPPA-3: Persist optimized prompts per project or globally for reuse

## Dependencies

- Story 8.1: GEPPA Integration Enablement

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Front-end Spec: Automation Center](../front-end-spec.md#14-automation-center)
- [Mockup: AI Studio](../mockups/ai-studio.html)
- [Mockup: Automation Center](../mockups/automation-center.html)

## Dev Notes

- Consider schema for prompt storage: name, content, category, projectId, metadata.
- UX should make prompt selection easy in agent builder and AI workflow configuration.
- Avoid storing secrets in saved prompts; validate or warn on save.
