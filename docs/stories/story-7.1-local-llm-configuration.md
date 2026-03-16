# Story 7.1: Local LLM (Llama) Configuration

**Epic:** Epic 7 - Local AI/ML Integration

**As a** developer who prefers local inference,  
**I want** to configure a local Llama (or compatible) endpoint in AiTask settings,  
**so that** I can use local AI without relying solely on cloud services.

## Status

Draft

## Acceptance Criteria

1. A user can configure a local LLM (e.g., Llama) endpoint in AiTask settings.
2. The configuration includes endpoint URL, model identifier, and optional API key or auth if required.
3. The application validates connectivity to the configured endpoint before saving.
4. LLM configuration persists across application sessions.
5. Multiple LLM configurations can be stored; one is designated as the default for AI-assisted features.

## Requirements Mapping (from features-v2.md)

- AI-1: The system MUST support configuration of a local LLM (e.g., Llama) for use within AiTask.

## Architecture References

- [Integration Architecture: External APIs](../architecture.md)
- [Settings / Configuration](../architecture.md)
