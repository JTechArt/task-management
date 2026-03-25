package com.aitask.core.domain.service

import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.TaskContentGenerationRequest
import com.aitask.core.domain.model.TaskContentGenerationResult

interface TaskContentGenerationService {
    suspend fun generate(
        request: TaskContentGenerationRequest,
        configuration: LlmConfiguration,
        apiKey: String?
    ): Result<TaskContentGenerationResult>
}
