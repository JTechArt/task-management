package com.aitask.core.domain.service

import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.TaskApproachStep

data class TaskApproachGenerationLlmOutput(
    val summary: String,
    val steps: List<TaskApproachStep>,
    val configurationName: String,
    val modelIdentifier: String,
    val endpointUrl: String
)

interface TaskApproachGenerationService {
    suspend fun generate(
        userPrompt: String,
        configuration: LlmConfiguration,
        apiKey: String?
    ): Result<TaskApproachGenerationLlmOutput>
}
