# Story 7.2: Local LLM for Task Descriptions and Summaries

**Epic:** Epic 7 - Local AI/ML Integration

**As a** developer,  
**I want** AiTask to generate task descriptions or summaries using my configured local model,  
**so that** I can quickly create or enrich task content without switching tools.

## Status

Draft

## Acceptance Criteria

1. AiTask can generate task descriptions or summaries using the configured local model.
2. LLM-assisted generation actions are clearly indicated in the UI.
3. Failed or slow LLM calls provide clear feedback and do not block core application flows.
4. Generated text can be accepted, edited, or rejected by the user before use.
5. Generated content remains editable before it is saved to the task.

## Requirements Mapping

- Local AI generation for task descriptions and summaries

## Architecture References

- [Architecture Overview](../architecture.md)

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Mockup: AI Studio](../mockups/ai-studio.html)
- [Mockup: Tasks](../mockups/tasks.html)
