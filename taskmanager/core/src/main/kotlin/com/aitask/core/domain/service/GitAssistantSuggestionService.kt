package com.aitask.core.domain.service

import com.aitask.core.domain.model.GitAssistantSuggestionRequest
import com.aitask.core.domain.model.GitAssistantSuggestionResult
import com.aitask.core.domain.model.LlmConfiguration

interface GitAssistantSuggestionService {
    suspend fun generate(
        request: GitAssistantSuggestionRequest,
        configuration: LlmConfiguration,
        apiKey: String?
    ): Result<GitAssistantSuggestionResult>
}
