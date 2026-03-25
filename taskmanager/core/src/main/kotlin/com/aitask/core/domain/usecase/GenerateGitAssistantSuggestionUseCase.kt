package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.GitAssistantSuggestionMode
import com.aitask.core.domain.model.GitAssistantSuggestionRequest
import com.aitask.core.domain.model.GitAssistantSuggestionResult
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.GitAssistantSuggestionService
import com.aitask.core.domain.service.GitService

data class GenerateGitAssistantSuggestionRequest(
    val taskId: java.util.UUID,
    val mode: GitAssistantSuggestionMode
)

class GenerateGitAssistantSuggestionUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val repositoryRepository: RepositoryRepository,
    private val llmConfigurationRepository: LlmConfigurationRepository,
    private val gitService: GitService,
    private val gitAssistantSuggestionService: GitAssistantSuggestionService
) {
    suspend operator fun invoke(request: GenerateGitAssistantSuggestionRequest): Result<GitAssistantSuggestionResult> {
        val task = taskRepository.findById(request.taskId)
            ?: return Result.failure(IllegalStateException("Task not found"))
        val project = projectRepository.findById(task.projectId)
            ?: return Result.failure(IllegalStateException("Project not found"))
        val configuration = llmConfigurationRepository.findDefault()
            ?: return Result.failure(IllegalStateException("No default local LLM profile configured"))
        val apiKey = if (configuration.hasApiKey) llmConfigurationRepository.findApiKey(configuration.id) else null
        val repositoryNames = repositoryRepository.findByProject(project.id).map { it.name }
        val stagedChangesSummary = task.workspacePath
            ?.takeIf { it.isNotBlank() }
            ?.let { gitService.getStagedChangesSummary(it).getOrNull() }

        return gitAssistantSuggestionService.generate(
            request = GitAssistantSuggestionRequest(
                projectId = project.id,
                projectName = project.name,
                taskId = task.id,
                taskTitle = task.title,
                taskDescription = task.description,
                branchName = task.branchName,
                repositoryNames = repositoryNames,
                stagedChangesSummary = stagedChangesSummary,
                mode = request.mode
            ),
            configuration = configuration,
            apiKey = apiKey
        )
    }
}
