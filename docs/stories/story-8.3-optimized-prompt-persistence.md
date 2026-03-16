# Story 8.3: Optimized Prompt Persistence

**Epic:** Epic 8 - GEPPA (Prompt Optimization)

**As a** developer,  
**I want** optimized prompts to be saved and reused across projects or tasks,  
**so that** I build a library of effective prompts without re-optimizing each time.

## Status

Draft

## Acceptance Criteria

1. A user can save optimized prompts with a name and optional category/tag.
2. Saved prompts can be associated with a project, task type, or kept global.
3. A user can browse, search, and select saved prompts when configuring AI-assisted features.
4. Prompts can be updated or deprecated; the application preserves version history or last-used state as appropriate.
5. Export/import of prompt library is supported (aligns with Epic 4 import/export where applicable).

## Requirements Mapping

- GEPPA-3 (Should Have): Persist optimized prompts per project or globally

## Dependencies

- Story 8.2: Prompt Routing and Optimization
- Epic 4: Import/Export (optional integration)

## Dev Notes

- Consider schema for prompt storage: name, content, category, projectId, metadata.
- UX should make prompt selection easy in agent builder, commit flow, etc.
- Security: avoid storing secrets in prompts; validate on save.
