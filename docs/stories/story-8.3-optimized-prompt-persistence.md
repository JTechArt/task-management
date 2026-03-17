# Story 8.3: GEPPA Integration with AI Features

**Epic:** Epic 8 - GEPPA (Prompt Optimization)

**As a** developer,  
**I want** AI-assisted features to use GEPPA-optimized prompts when configured,  
**so that** task generation, commit messages, and agent prompts benefit from optimization.

## Status

Draft

## Acceptance Criteria

1. Task description generation uses GEPPA-optimized prompts when GEPPA is enabled.
2. Commit message and PR description suggestions use optimized prompts when available.
3. Agent and workflow prompts can be associated with saved GEPPA-optimized prompts.
4. The application falls back gracefully when GEPPA is unavailable or optimization fails.
5. Optimization activity is logged for traceability without exposing prompt content in logs.

## Requirements Mapping

- GEPPA-2: Prompts used for AI-assisted features should be optimizable via GEPPA

## Dependencies

- Story 8.1: GEPPA Integration Enablement
- Story 8.2: Prompt Optimization and Persistence
- Epic 7: Local AI/ML Integration

## UX References

- [Front-end Spec: AI Studio](../front-end-spec.md#13-ai-studio)
- [Front-end Spec: Automation Center](../front-end-spec.md#14-automation-center)
- [Mockup: AI Studio](../mockups/ai-studio.html)
- [Mockup: Automation Center](../mockups/automation-center.html)

## Dev Notes

- Integrate with LLM invocation points from Epic 7 and future AI automation.
- Graceful fallback is required when optimization is unavailable or times out.
- Log optimization usage for traceability without storing sensitive prompt bodies.
