package com.aitask.core.domain.service

import com.aitask.core.domain.model.AgentExecutionResult
import com.aitask.core.domain.model.LlmConfiguration

interface AgentExecutionService {
    suspend fun execute(
        prompt: String,
        configuration: LlmConfiguration,
        apiKey: String?
    ): Result<AgentExecutionResult>
}
