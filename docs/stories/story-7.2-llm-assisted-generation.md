# Story 7.2: LLM-Assisted Text Generation and Service Integrations

**Epic:** Epic 7 - Local AI/ML Integration

**As a** developer,  
**I want** AiTask to use my configured LLM for task descriptions, summaries, and commit/PR suggestions,  
**so that** I get AI-assisted text generation without exposing data to cloud APIs when I prefer local inference.

## Status

Draft

## Acceptance Criteria

1. AiTask can generate task descriptions or summaries using the configured local model.
2. With service integrations enabled (Git, Slack, etc.), the LLM can suggest commit messages, PR descriptions, or comment drafting.
3. LLM-assisted operations are clearly indicated in the UI (e.g., "Generate with AI" actions).
4. Failed or slow LLM calls provide clear feedback and do not block core application flows.
5. Generated text can be accepted, edited, or rejected by the user before use.

## Requirements Mapping (from features-v2.md)

- AI-2, AI-3: Local models for task descriptions, summaries; LLM-assisted Git/Slack operations.

## Architecture References

- [Integration Architecture](../architecture.md)
- [Git Integration](../architecture.md)
