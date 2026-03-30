package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ClaudeConfigurationRepository
import com.aitask.core.domain.repository.PreRunScriptRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.BmadConfigurationResolver
import com.aitask.core.domain.service.ClaudeAnthropicApiService
import com.aitask.core.domain.service.ClaudeCliService
import com.aitask.core.domain.service.PreRunScriptService
import com.aitask.core.infrastructure.ide.TaskContextFileWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val workspaceMetadataJson = Json { ignoreUnknownKeys = true }

data class LaunchClaudeRequest(
    val taskId: UUID,
    val updateTaskStatus: Boolean = true
)

data class LaunchClaudeResponse(
    val task: Task,
    val activity: Activity,
    val userMessage: String
)

class LaunchClaudeUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val preRunScriptRepository: PreRunScriptRepository,
    private val preRunScriptService: PreRunScriptService,
    private val bmadConfigurationResolver: BmadConfigurationResolver,
    private val claudeConfigurationRepository: ClaudeConfigurationRepository,
    private val claudeCliService: ClaudeCliService,
    private val claudeAnthropicApiService: ClaudeAnthropicApiService,
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(request: LaunchClaudeRequest): Result<LaunchClaudeResponse> {
        return try {
            logger.info { "Launching Claude for task ${request.taskId}" }
            val configuration = claudeConfigurationRepository.find()
            if (configuration == null || !configuration.isEnabled) {
                return Result.failure(
                    IllegalStateException("Claude integration is disabled. Enable it in Settings.")
                )
            }
            val task = taskRepository.findById(request.taskId)
                ?: return Result.failure(TaskNotFoundException("Task not found: ${request.taskId}"))
            if (task.workspacePath == null) {
                return Result.failure(
                    IllegalStateException("Task does not have a workspace. Generate workspace first.")
                )
            }
            val project = projectRepository.findById(task.projectId)
                ?: return Result.failure(ProjectNotFoundException("Project not found: ${task.projectId}"))
            val bmadConfiguration = bmadConfigurationResolver.resolve(project, task)
            val allScripts = preRunScriptRepository.findByProject(project.id)
            val selectedRepositoryIds = loadSelectedRepositoryIds(task.workspacePath)
            val projectScripts = allScripts
                .filter { it.repositoryId == null }
                .sortedBy { it.executionOrder }
            val repositoryScripts = selectedRepositoryIds.flatMap { repositoryId ->
                allScripts
                    .filter { it.repositoryId == repositoryId }
                    .sortedBy { it.executionOrder }
            }
            val orderedScripts = projectScripts + repositoryScripts
            if (orderedScripts.isNotEmpty()) {
                val executionResult = preRunScriptService.executeScripts(
                    scripts = orderedScripts,
                    workingDirectory = task.workspacePath
                )
                if (executionResult.isFailure) {
                    val failure = executionResult.exceptionOrNull() ?: Exception("Pre-run scripts failed")
                    activityRepository.create(
                        Activity(
                            id = UUID.randomUUID(),
                            type = ActivityType.PRE_RUN_FAILED,
                            entityType = "task",
                            entityId = task.id,
                            description = "Pre-run scripts failed for task: ${task.title}",
                            metadata = mapOf(
                                "projectName" to project.name,
                                "executionOrder" to "project-then-repository",
                                "error" to (failure.message ?: "Unknown pre-run error")
                            ),
                            status = ActivityStatus.FAILED,
                            createdAt = Instant.now(),
                            projectId = task.projectId
                        )
                    )
                    return Result.failure(failure)
                }
                val details = executionResult.getOrThrow()
                activityRepository.create(
                    Activity(
                        id = UUID.randomUUID(),
                        type = ActivityType.PRE_RUN_SUCCESS,
                        entityType = "task",
                        entityId = task.id,
                        description = "Completed ${details.size} pre-run script(s) for task: ${task.title}",
                        metadata = mapOf(
                            "projectName" to project.name,
                            "scriptCount" to details.size.toString(),
                            "executionOrder" to "project-then-repository"
                        ),
                        status = ActivityStatus.SUCCESS,
                        createdAt = Instant.now(),
                        projectId = task.projectId
                    )
                )
            }
            val taskContext = TaskContext(
                taskId = task.id,
                title = task.title,
                description = task.description,
                projectName = project.name,
                branchName = task.branchName,
                activeBmadTools = if (bmadConfiguration.methodology == Methodology.BMAD) {
                    BmadToolCatalog.labelsFor(bmadConfiguration.toolIds)
                } else {
                    emptyList()
                },
                repositoryPath = task.workspacePath
            )
            TaskContextFileWriter.write(task.workspacePath, taskContext)
            val contextFile = File(task.workspacePath, "TASK_CONTEXT.md")
            val env = mutableMapOf<String, String>(
                "AITASK_PROJECT_NAME" to project.name,
                "AITASK_REPOSITORY_PATH" to task.workspacePath,
                "AITASK_TASK_CONTEXT_FILE" to contextFile.absolutePath
            )
            val apiKey = claudeConfigurationRepository.findApiKey(configuration.id)
            if (apiKey != null && apiKey.isNotBlank()) {
                env["ANTHROPIC_API_KEY"] = apiKey
            }
            return when (configuration.integrationMode) {
                ClaudeIntegrationMode.API -> launchViaApi(
                    request = request,
                    configuration = configuration,
                    task = task,
                    project = project,
                    apiKey = apiKey
                )
                ClaudeIntegrationMode.CLI -> launchViaCli(
                    request = request,
                    configuration = configuration,
                    task = task,
                    project = project,
                    env = env
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error launching Claude for task ${request.taskId}" }
            Result.failure(e)
        }
    }

    private suspend fun launchViaApi(
        request: LaunchClaudeRequest,
        configuration: ClaudeConfiguration,
        task: Task,
        project: Project,
        apiKey: String?
    ): Result<LaunchClaudeResponse> {
        val key = apiKey?.trim()?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Anthropic API key is required for Claude API mode."))
        val baseUrl = effectiveAnthropicBaseUrl(configuration.apiBaseUrl)
        val prompt = buildApiPrompt(project, task)
        val apiResult = claudeAnthropicApiService.sendTaskContextPrompt(
            apiKey = key,
            baseUrl = baseUrl,
            userPrompt = prompt
        )
        if (apiResult.isFailure) {
            val err = apiResult.exceptionOrNull() ?: Exception("Claude API request failed")
            recordClaudeActivity(task, project.name, err.message ?: "Unknown error", ActivityStatus.FAILED, "API")
            return Result.failure(err)
        }
        val responseText = apiResult.getOrThrow()
        val preview = responseText.take(500)
        val updatedTask = if (request.updateTaskStatus && task.status == TaskStatus.PENDING) {
            val newTask = task.updateStatus(TaskStatus.IN_PROGRESS)
            taskRepository.update(newTask)
            newTask
        } else {
            task
        }
        val activity = Activity(
            id = UUID.randomUUID(),
            type = ActivityType.CLAUDE_LAUNCHED,
            entityType = "task",
            entityId = task.id,
            description = "Completed Claude API request for task: ${task.title}",
            metadata = mapOf(
                "mode" to "API",
                "projectName" to project.name,
                "responsePreview" to preview
            ),
            status = ActivityStatus.SUCCESS,
            createdAt = Instant.now(),
            projectId = task.projectId
        )
        activityRepository.create(activity)
        logger.info { "Successfully invoked Claude API for task ${task.id}" }
        return Result.success(
            LaunchClaudeResponse(
                task = updatedTask,
                activity = activity,
                userMessage = "Claude API request completed successfully"
            )
        )
    }

    private suspend fun launchViaCli(
        request: LaunchClaudeRequest,
        configuration: ClaudeConfiguration,
        task: Task,
        project: Project,
        env: Map<String, String>
    ): Result<LaunchClaudeResponse> {
        val resolved = claudeCliService.resolveExecutable(
            configuration.cliPath.takeIf { it.isNotBlank() }
        )
        if (resolved.isFailure) {
            val err = resolved.exceptionOrNull() ?: Exception("Could not resolve Claude CLI")
            recordClaudeActivity(task, project.name, err.message ?: "Unknown error", ActivityStatus.FAILED, "CLI")
            return Result.failure(err)
        }
        val executablePath = resolved.getOrThrow()
        val validated = claudeCliService.validateExecutable(executablePath)
        if (validated.isFailure) {
            val err = validated.exceptionOrNull() ?: Exception("Claude CLI validation failed")
            recordClaudeActivity(task, project.name, err.message ?: "Unknown error", ActivityStatus.FAILED, "CLI")
            return Result.failure(err)
        }
        val launchResult = claudeCliService.launchInTerminal(
            resolvedExecutable = executablePath,
            workspacePath = task.workspacePath!!,
            environment = env
        )
        if (launchResult.isFailure) {
            val err = launchResult.exceptionOrNull() ?: Exception("Failed to launch Claude")
            recordClaudeActivity(task, project.name, err.message ?: "Unknown error", ActivityStatus.FAILED, "CLI")
            return Result.failure(err)
        }
        val updatedTask = if (request.updateTaskStatus && task.status == TaskStatus.PENDING) {
            val newTask = task.updateStatus(TaskStatus.IN_PROGRESS)
            taskRepository.update(newTask)
            newTask
        } else {
            task
        }
        val activity = Activity(
            id = UUID.randomUUID(),
            type = ActivityType.CLAUDE_LAUNCHED,
            entityType = "task",
            entityId = task.id,
            description = "Launched Claude CLI for task: ${task.title}",
            metadata = mapOf(
                "mode" to "CLI",
                "workspacePath" to task.workspacePath!!,
                "projectName" to project.name
            ),
            status = ActivityStatus.SUCCESS,
            createdAt = Instant.now(),
            projectId = task.projectId
        )
        activityRepository.create(activity)
        logger.info { "Successfully launched Claude CLI for task ${task.id}" }
        return Result.success(
            LaunchClaudeResponse(
                task = updatedTask,
                activity = activity,
                userMessage = "Launched Claude CLI successfully"
            )
        )
    }

    private fun buildApiPrompt(
        project: Project,
        task: Task
    ): String {
        return buildString {
            appendLine("You are assisting via AiTask with the following task context.")
            appendLine()
            appendLine("Project: ${project.name}")
            appendLine("Task: ${task.title}")
            appendLine("Task description:")
            appendLine(task.description ?: "(none)")
            appendLine()
            appendLine("Repository/workspace path: ${task.workspacePath}")
            appendLine()
            appendLine("Respond briefly with a suggested next step for this task.")
        }
    }

    private fun effectiveAnthropicBaseUrl(apiBaseUrl: String?): String {
        val trimmed = apiBaseUrl?.trim().orEmpty()
        val withScheme: String = when {
            trimmed.isEmpty() -> "https://api.anthropic.com"
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
        return withScheme.trimEnd('/')
    }

    private suspend fun recordClaudeActivity(
        task: Task,
        projectName: String,
        errorMessage: String,
        status: ActivityStatus,
        mode: String
    ) {
        activityRepository.create(
            Activity(
                id = UUID.randomUUID(),
                type = ActivityType.CLAUDE_LAUNCHED,
                entityType = "task",
                entityId = task.id,
                description = "Failed to launch Claude for task: ${task.title}",
                metadata = mapOf(
                    "projectName" to projectName,
                    "mode" to mode,
                    "error" to errorMessage
                ),
                status = status,
                createdAt = Instant.now(),
                projectId = task.projectId
            )
        )
    }

    private fun loadSelectedRepositoryIds(workspacePath: String): List<UUID> {
        return runCatching {
            val metadataFile = File(workspacePath, "workspace-metadata.json")
            if (!metadataFile.exists()) {
                emptyList()
            } else {
                workspaceMetadataJson.parseToJsonElement(metadataFile.readText())
                    .jsonObject["repositories"]
                    ?.jsonArray
                    ?.mapNotNull { element ->
                        element.jsonObject["repositoryId"]?.jsonPrimitive?.content?.let { UUID.fromString(it) }
                    }
                    ?: emptyList()
            }
        }.getOrElse { error ->
            logger.warn(error) { "Failed to load workspace metadata for pre-run resolution from $workspacePath" }
            emptyList()
        }
    }
}
