# Story 8.2: Prompt Routing and Optimization

**Epic:** Epic 8 - GEPPA (Prompt Optimization)

**As a** developer,  
**I want** prompts sent to LLMs to be routed through GEPPA for optimization before use,  
**so that** AI-assisted features produce more consistent and higher-quality outputs.

## Status

Draft

## Acceptance Criteria

1. When GEPPA is enabled, prompts used for AI-assisted features (agents, task generation, commits) can be routed through GEPPA.
2. The application supports opt-in or opt-out per feature (e.g., commit messages vs. task descriptions) for GEPPA routing.
3. Optimized prompts are used in place of raw prompts when routing is active.
4. The application handles GEPPA timeout or failure gracefully (fallback to raw prompt or clear error).
5. Routing behavior is configurable at project or global level.

## Requirements Mapping

- GEPPA-2 (Should Have): Optimizable prompts for AI-assisted features

## Dependencies

- Story 8.1: GEPPA Integration Enablement
- Epic 7: Local AI/ML Integration

## Dev Notes

- Integrate with LLM invocation points from Epic 7 and future AI automation.
- Consider async/sync behavior; GEPPA may add latency.
- Log when prompts are optimized for traceability.
