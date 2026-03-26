package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.AgentDefinitionRepository
import com.aitask.core.domain.service.GeppaPromptOptimizationService
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.AgentExecutionService
import java.time.Instant
import java.util.UUID

data class RunAgentRequest(
    val taskId: UUID,
    val agentId: UUID
)

data class RunAgentResult(
    val agentId: UUID,
    val agentName: String,
    val generatedText: String,
    val configurationName: String,
    val modelIdentifier: String,
    val endpointUrl: String
)

class RunAgentUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val repositoryRepository: RepositoryRepository,
    private val agentDefinitionRepository: AgentDefinitionRepository,
    private val llmConfigurationRepository: LlmConfigurationRepository,
    private val agentExecutionService: AgentExecutionService,
    private val activityRepository: ActivityRepository,
    private val geppaPromptOptimizationService: GeppaPromptOptimizationService? = null
) {
    suspend operator fun invoke(request: RunAgentRequest): Result<RunAgentResult> {
        val task = taskRepository.findById(request.taskId)
            ?: return Result.failure(IllegalStateException("Task not found"))
        val agent = agentDefinitionRepository.findById(request.agentId)
            ?: return Result.failure(IllegalStateException("Agent not found"))
        if (!agent.isEnabled) {
            return Result.failure(IllegalStateException("Agent is disabled"))
        }
        val project = projectRepository.findById(task.projectId)
            ?: return Result.failure(IllegalStateException("Project not found"))
        val repositories = repositoryRepository.findByProject(project.id)
        val configuration = agent.llmConfigurationId?.let { llmConfigurationRepository.findById(it) }
            ?: llmConfigurationRepository.findDefault()
            ?: return Result.failure(IllegalStateException("No local LLM profile configured"))
        val apiKey = if (configuration.hasApiKey) llmConfigurationRepository.findApiKey(configuration.id) else null
        val renderedPrompt = renderPrompt(agent, task, project, repositories)
        val prompt = geppaPromptOptimizationService?.optimizeIfEnabled(renderedPrompt, "agent_execution")
            ?: renderedPrompt

        val result = agentExecutionService.execute(
            prompt = prompt,
            configuration = configuration,
            apiKey = apiKey
        )

        val geppaApplied = geppaPromptOptimizationService != null && prompt != renderedPrompt
        result.onSuccess { execution ->
            logAgentActivity(
                agent = agent,
                task = task,
                project = project,
                status = ActivityStatus.SUCCESS,
                output = execution.generatedText,
                configuration = configuration,
                geppaOptimized = geppaApplied
            )
        }.onFailure { error ->
            logAgentActivity(
                agent = agent,
                task = task,
                project = project,
                status = ActivityStatus.FAILED,
                output = error.message ?: error.javaClass.simpleName,
                configuration = configuration,
                geppaOptimized = geppaApplied
            )
        }

        return result.map { execution ->
            RunAgentResult(
                agentId = agent.id,
                agentName = agent.name,
                generatedText = execution.generatedText,
                configurationName = execution.configurationName,
                modelIdentifier = execution.modelIdentifier,
                endpointUrl = execution.endpointUrl
            )
        }
    }

    private fun renderPrompt(
        agent: AgentDefinition,
        task: Task,
        project: Project,
        repositories: List<Repository>
    ): String {
        val repositoryNames = repositories.joinToString(", ") { it.name }
        val template = agent.promptTemplate.trim()
        val mergedTemplate = if (template.contains("{{")) template else buildString {
            appendLine(template)
            appendLine()
            appendLine("Task title: {{taskTitle}}")
            appendLine("Task description: {{taskDescription}}")
            appendLine("Project: {{projectName}}")
            appendLine("Branch: {{branchName}}")
            appendLine("Repositories: {{repositoryNames}}")
        }

        return mergedTemplate
            .replace("{{taskTitle}}", task.title)
            .replace("{{taskDescription}}", task.description.orEmpty())
            .replace("{{projectName}}", project.name)
            .replace("{{branchName}}", task.branchName.orEmpty())
            .replace("{{repositoryNames}}", repositoryNames)
    }

    private suspend fun logAgentActivity(
        agent: AgentDefinition,
        task: Task,
        project: Project,
        status: ActivityStatus,
        output: String,
        configuration: LlmConfiguration,
        geppaOptimized: Boolean = false
    ) {
        runCatching {
            activityRepository.create(
                Activity(
                    id = UUID.randomUUID(),
                    type = ActivityType.AGENT_EXECUTED,
                    entityType = "task",
                    entityId = task.id,
                    description = "Executed agent '${agent.name}' for task: ${task.title}",
                    metadata = mapOf(
                        "agentName" to agent.name,
                        "scope" to agent.scope.name,
                        "trigger" to agent.trigger.name,
                        "configurationName" to configuration.name,
                        "modelIdentifier" to configuration.modelIdentifier,
                        "outputLength" to output.length.toString(),
                        "geppaOptimized" to geppaOptimized.toString()
                    ),
                    createdAt = Instant.now(),
                    status = status,
                    projectId = project.id
                )
            )
        }
    }
}

