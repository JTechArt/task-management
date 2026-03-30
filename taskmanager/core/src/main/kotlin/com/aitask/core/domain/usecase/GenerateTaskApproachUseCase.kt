package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.AgentDefinition
import com.aitask.core.domain.model.AgentToolKind
import com.aitask.core.domain.model.TaskSolvingApproach
import com.aitask.core.domain.repository.AgentDefinitionRepository
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.TaskApproachGenerationService
import java.util.UUID

data class GenerateTaskApproachRequest(
    val taskId: UUID,
    val agentId: UUID? = null
)

data class GenerateTaskApproachResult(
    val approach: TaskSolvingApproach
)

class GenerateTaskApproachUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val repositoryRepository: RepositoryRepository,
    private val agentDefinitionRepository: AgentDefinitionRepository,
    private val llmConfigurationRepository: LlmConfigurationRepository,
    private val taskApproachGenerationService: TaskApproachGenerationService
) {
    suspend operator fun invoke(request: GenerateTaskApproachRequest): Result<GenerateTaskApproachResult> {
        val task = taskRepository.findById(request.taskId)
            ?: return Result.failure(IllegalStateException("Task not found"))
        val project = projectRepository.findById(task.projectId)
            ?: return Result.failure(IllegalStateException("Project not found"))
        val repositories = repositoryRepository.findByProject(project.id)
        val availableAgents = agentDefinitionRepository.findAvailableForProject(task.projectId, task.taskType)
        val resolvedAgent: AgentDefinition? = when {
            request.agentId != null -> {
                val match = availableAgents.find { it.id == request.agentId }
                if (match == null) {
                    return Result.failure(
                        IllegalStateException(
                            "The selected agent is not available for this project or task type."
                        )
                    )
                }
                match
            }
            else -> availableAgents.firstOrNull { it.isEnabled }
        }
        val configuration = resolvedAgent?.llmConfigurationId?.let { llmConfigurationRepository.findById(it) }
            ?: llmConfigurationRepository.findDefault()
            ?: return Result.failure(IllegalStateException("No default local LLM profile configured"))
        val apiKey = if (configuration.hasApiKey) llmConfigurationRepository.findApiKey(configuration.id) else null
        val toolNames = formatToolNames(resolvedAgent?.associatedTools)
        val prompt = buildPrompt(
            taskTitle = task.title,
            taskDescription = task.description,
            taskTypeLabel = task.taskType.name.replace('_', ' ').lowercase(),
            projectName = project.name,
            repositoryNames = repositories.joinToString(", ") { it.name }.ifBlank { "none listed" },
            agentName = resolvedAgent?.name,
            agentTools = toolNames
        )
        return taskApproachGenerationService.generate(
            userPrompt = prompt,
            configuration = configuration,
            apiKey = apiKey
        ).map { output ->
            GenerateTaskApproachResult(
                approach = TaskSolvingApproach(
                    summary = output.summary,
                    steps = output.steps,
                    contextAgentId = resolvedAgent?.id,
                    contextAgentName = resolvedAgent?.name
                )
            )
        }
    }

    private fun formatToolNames(tools: Set<AgentToolKind>?): String {
        val set = tools?.takeIf { it.isNotEmpty() } ?: setOf(AgentToolKind.LOCAL_LLM)
        return set.joinToString(", ") { kind ->
            when (kind) {
                AgentToolKind.LOCAL_LLM -> "Local LLM (OpenAI-compatible API)"
                AgentToolKind.CODEX -> "Codex CLI"
                AgentToolKind.CLAUDE -> "Claude CLI"
            }
        }
    }

    private fun buildPrompt(
        taskTitle: String,
        taskDescription: String?,
        taskTypeLabel: String,
        projectName: String,
        repositoryNames: String,
        agentName: String?,
        agentTools: String
    ): String = buildString {
        appendLine("You are planning how a developer should solve a software task.")
        appendLine("Produce a JSON object ONLY (no markdown fences, no commentary) with this exact shape:")
        appendLine("{")
        appendLine("  \"summary\": \"One short paragraph summarizing the overall approach.\",")
        appendLine("  \"steps\": [")
        appendLine("    {")
        appendLine("      \"title\": \"Short step title\",")
        appendLine("      \"detail\": \"What to do and why.\",")
        appendLine("      \"tools\": [\"LOCAL_LLM\"],")
        appendLine("      \"prompt\": \"Optional prompt or checklist text for this step, or null\"")
        appendLine("    }")
        appendLine("  ]")
        appendLine("}")
        appendLine("Rules for \"tools\" array: use only these exact tokens: LOCAL_LLM, CODEX, CLAUDE.")
        appendLine("Pick tools that fit each step; they must align with the associations available for this task.")
        appendLine("Include at least 2 and at most 8 steps.")
        appendLine()
        appendLine("Task title: $taskTitle")
        appendLine("Task type: $taskTypeLabel")
        appendLine("Project: $projectName")
        appendLine("Repositories: $repositoryNames")
        appendLine("Task description: ${taskDescription?.trim()?.takeIf { it.isNotBlank() } ?: "(none)"}")
        appendLine()
        if (agentName != null) {
            appendLine("Preferred agent context: $agentName")
        } else {
            appendLine("No specific saved agent was selected; infer reasonable tool usage.")
        }
        appendLine("Associated AI tools for this task scope: $agentTools")
    }
}
