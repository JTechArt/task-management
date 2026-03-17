# Story 9.3: Unified AI Tool Launch Experience

**Epic:** Epic 9 - AI Tools Integration (Codex, Claude)

**As a** developer,  
**I want** a unified way to launch configured AI tools from the task UI,  
**so that** I can quickly choose which AI assistant to use without navigating multiple settings.

## Status

Draft

## Acceptance Criteria

1. The task detail screen includes actions (e.g., "Run with Codex", "Run with Claude") when the respective tools are configured.
2. AI tool actions are grouped or presented in a consistent pattern (e.g., toolbar, dropdown, or action menu).
3. Disabled or unconfigured tools are hidden or clearly indicated as unavailable.
4. The user can invoke multiple AI tools from the same task without re-navigating.
5. Launch actions respect pre-run script execution when configured (Epic 5).

## Requirements Mapping

- AITOOL-4 (Should Have): Unified launch for configured AI tools from task UI

## Dependencies

- Story 9.1: Codex CLI Integration
- Story 9.2: Claude Integration
- Epic 5: Pre-Run Scripts (optional gating)

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Mockup: AI Studio](../mockups/ai-studio.html)
- [Mockup: Tasks](../mockups/tasks.html)

## Dev Notes

- Reuse patterns from IDE launch (Story 1.5) for consistency.
- Consider extensibility for future AI tools (e.g., GitHub Copilot CLI).
- Ensure UI scales when more tools are added.
