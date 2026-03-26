package com.aitask.core.domain.service

interface GeppaPromptOptimizationService {
    /**
     * Optimizes [prompt] via GEPPA if GEPPA is enabled and reachable.
     * Returns the optimized prompt on success, or [prompt] unchanged when GEPPA is
     * disabled, unavailable, or the call fails (graceful fallback — AC4).
     *
     * [context] is a short identifier used for activity logging (e.g. "task_generation",
     * "git_suggestion", "agent_execution").  Prompt content is never written to logs (AC5).
     */
    suspend fun optimizeIfEnabled(prompt: String, context: String): String
}
