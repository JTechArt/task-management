package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.CodexConfigurationRepository
import com.aitask.core.domain.repository.PreRunScriptRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.BmadConfigurationResolver
import com.aitask.core.domain.service.CodexCliService
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

data class LaunchCodexRequest(
    val taskId: UUID,
    val updateTaskStatus: Boolean = true
)

data class LaunchCodexResponse(
    val task: Task,
    val activity: Activity
)

class LaunchCodexUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val preRunScriptRepository: PreRunScriptRepository,
    private val preRunScriptService: PreRunScriptService,
    private val bmadConfigurationResolver: BmadConfigurationResolver,
    private val codexConfigurationRepository: CodexConfigurationRepository,
    private val codexCliService: CodexCliService,
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(request: LaunchCodexRequest): Result<LaunchCodexResponse> {
        return try {
            logger.info { "Launching Codex CLI for task ${request.taskId}" }
            val configuration = codexConfigurationRepository.find()
            if (configuration == null || !configuration.isEnabled) {
                return Result.failure(
                    IllegalStateException("Codex CLI is disabled. Enable it and set the executable path in Settings.")
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
            val resolved = codexCliService.resolveExecutable(
                configuration.cliPath.takeIf { it.isNotBlank() }
            )
            if (resolved.isFailure) {
                val err = resolved.exceptionOrNull() ?: Exception("Could not resolve Codex CLI")
                recordCodexActivity(task, project.name, err.message ?: "Unknown error", ActivityStatus.FAILED)
                return Result.failure(err)
            }
            val executablePath = resolved.getOrThrow()
            val validated = codexCliService.validateExecutable(executablePath)
            if (validated.isFailure) {
                val err = validated.exceptionOrNull() ?: Exception("Codex CLI validation failed")
                recordCodexActivity(task, project.name, err.message ?: "Unknown error", ActivityStatus.FAILED)
                return Result.failure(err)
            }
            val contextFile = File(task.workspacePath, "TASK_CONTEXT.md")
            val env = mutableMapOf<String, String>(
                "AITASK_PROJECT_NAME" to project.name,
                "AITASK_REPOSITORY_PATH" to task.workspacePath,
                "AITASK_TASK_CONTEXT_FILE" to contextFile.absolutePath
            )
            val apiKey = codexConfigurationRepository.findApiKey(configuration.id)
            if (apiKey != null && apiKey.isNotBlank()) {
                env["OPENAI_API_KEY"] = apiKey
            }
            val launchResult = codexCliService.launchInTerminal(
                resolvedExecutable = executablePath,
                workspacePath = task.workspacePath,
                environment = env
            )
            if (launchResult.isFailure) {
                val err = launchResult.exceptionOrNull() ?: Exception("Failed to launch Codex")
                recordCodexActivity(task, project.name, err.message ?: "Unknown error", ActivityStatus.FAILED)
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
                type = ActivityType.CODEX_LAUNCHED,
                entityType = "task",
                entityId = task.id,
                description = "Launched Codex CLI for task: ${task.title}",
                metadata = mapOf(
                    "workspacePath" to task.workspacePath,
                    "projectName" to project.name
                ),
                status = ActivityStatus.SUCCESS,
                createdAt = Instant.now(),
                projectId = task.projectId
            )
            activityRepository.create(activity)
            logger.info { "Successfully launched Codex for task ${task.id}" }
            Result.success(LaunchCodexResponse(updatedTask, activity))
        } catch (e: Exception) {
            logger.error(e) { "Error launching Codex for task ${request.taskId}" }
            Result.failure(e)
        }
    }

    private suspend fun recordCodexActivity(
        task: Task,
        projectName: String,
        errorMessage: String,
        status: ActivityStatus
    ) {
        activityRepository.create(
            Activity(
                id = UUID.randomUUID(),
                type = ActivityType.CODEX_LAUNCHED,
                entityType = "task",
                entityId = task.id,
                description = "Failed to launch Codex for task: ${task.title}",
                metadata = mapOf(
                    "projectName" to projectName,
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
