package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.RetentionPolicy
import com.aitask.core.domain.service.WorkspaceService
import com.aitask.core.infrastructure.workspace.WorkspaceCleanupException
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

class CleanupWorkspaceUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val workspaceService: WorkspaceService,
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(taskId: UUID): Result<Unit> {
        return try {
            val task = taskRepository.findById(taskId)
                ?: return Result.failure(TaskNotFoundException("Task with id $taskId not found"))
            if (task.status != TaskStatus.COMPLETED) {
                return Result.failure(
                    WorkspaceCleanupException("Workspace cleanup only applies to completed tasks")
                )
            }
            val workspacePath = task.workspacePath
                ?: return Result.failure(
                    WorkspaceCleanupException("Task has no workspace to clean up")
                )
            if (task.workspaceCleanedAt != null) {
                return Result.failure(
                    WorkspaceCleanupException("Workspace already cleaned up")
                )
            }
            val project = projectRepository.findById(task.projectId)
                ?: return Result.failure(ProjectNotFoundException("Project not found"))
            val cleanupResult = workspaceService.cleanupWorkspace(
                workspacePath = workspacePath,
                retentionPolicy = RetentionPolicy.DELETE_ON_COMPLETION
            )
            if (cleanupResult.isSuccess) {
                val updatedTask = task.copy(
                    workspaceCleanedAt = Instant.now()
                )
                taskRepository.update(updatedTask)
                activityRepository.create(
                    Activity(
                        id = UUID.randomUUID(),
                        type = ActivityType.WORKSPACE_CLEANUP,
                        entityType = "task",
                        entityId = taskId,
                        description = "Workspace cleaned up for task: ${task.title}",
                        metadata = mapOf("workspacePath" to workspacePath),
                        status = ActivityStatus.SUCCESS,
                        createdAt = Instant.now(),
                        projectId = task.projectId
                    )
                )
                Result.success(Unit)
            } else {
                val error = cleanupResult.exceptionOrNull()
                val message = error?.message ?: "Unknown error"
                logger.error(error) { "Workspace cleanup failed for task $taskId: $message" }
                activityRepository.create(
                    Activity(
                        id = UUID.randomUUID(),
                        type = ActivityType.WORKSPACE_CLEANUP,
                        entityType = "task",
                        entityId = taskId,
                        description = "Workspace cleanup failed for task: ${task.title}",
                        metadata = mapOf("error" to message),
                        status = ActivityStatus.FAILED,
                        createdAt = Instant.now(),
                        projectId = task.projectId
                    )
                )
                val actionableMessage = buildActionableCleanupFailureMessage(message)
                Result.failure(WorkspaceCleanupException(actionableMessage))
            }
        } catch (e: Exception) {
            logger.error(e) { "Workspace cleanup failed for task $taskId: ${e.message}" }
            Result.failure(e)
        }
    }
}

private fun buildActionableCleanupFailureMessage(rawMessage: String): String {
    val prefix = "Workspace cleanup failed. "
    val suggestion = when {
        rawMessage.contains("permission", ignoreCase = true) ->
            "Check file permissions and ensure the application can delete the workspace directory."
        rawMessage.contains("locked", ignoreCase = true) ||
            rawMessage.contains("in use", ignoreCase = true) ->
            "Close any applications using files in the workspace and retry."
        rawMessage.contains("not found", ignoreCase = true) ->
            "Workspace path may have been moved or deleted. No action needed."
        else ->
            "Try closing any open files in the workspace and retry, or manually delete the folder."
    }
    return prefix + suggestion
}
