package com.aitask.core.domain.service

/**
 * Calls the Anthropic Messages API (HTTPS) for Claude API integration mode.
 */
interface ClaudeAnthropicApiService {
    suspend fun validateApiKey(apiKey: String, baseUrl: String): Result<Unit>
    suspend fun sendTaskContextPrompt(apiKey: String, baseUrl: String, userPrompt: String): Result<String>
}
