package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.TaskContentGenerationRequest
import com.aitask.core.domain.model.TaskContentGenerationResult
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.service.TaskContentGenerationService

class GenerateTaskContentUseCase(
    private val projectRepository: ProjectRepository,
    private val llmConfigurationRepository: LlmConfigurationRepository,
    private val taskContentGenerationService: TaskContentGenerationService
) {
    suspend operator fun invoke(request: TaskContentGenerationRequest): Result<TaskContentGenerationResult> {
        val project = projectRepository.findById(request.projectId)
            ?: return Result.failure(IllegalStateException("Project not found"))
        val configuration = llmConfigurationRepository.findDefault()
            ?: return Result.failure(IllegalStateException("No default local LLM profile configured"))
        val apiKey = if (configuration.hasApiKey) llmConfigurationRepository.findApiKey(configuration.id) else null
        return taskContentGenerationService.generate(
            request = request.copy(projectName = project.name),
            configuration = configuration,
            apiKey = apiKey
        )
    }
}
