package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.IDEService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Request to launch an IDE for a task
 */
data class LaunchIDERequest(
    val taskId: UUID,
    val ideType: IDEType,
    val updateTaskStatus: Boolean = true
)

/**
 * Response from launching an IDE
 */
data class LaunchIDEResponse(
    val task: Task,
    val activity: Activity
)

/**
 * Use case for launching an IDE with a task workspace
 * 
 * This use case:
 * 1. Validates the task exists and has a workspace
 * 2. Validates the IDE type is configured for the project's repository
 * 3. Launches the IDE with the workspace path
 * 4. Updates the task status to IN_PROGRESS (if requested)
 * 5. Records an activity log entry
 */
class LaunchIDEUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val repositoryRepository: RepositoryRepository,
    private val ideService: IDEService,
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(request: LaunchIDERequest): Result<LaunchIDEResponse> {
        return try {
            logger.info { "Launching IDE for task ${request.taskId} with IDE type ${request.ideType}" }
            
            // 1. Validate task exists
            val task = taskRepository.findById(request.taskId)
                ?: return Result.failure(TaskNotFoundException("Task not found: ${request.taskId}"))
            
            // 2. Validate task has a workspace
            if (task.workspacePath == null) {
                return Result.failure(
                    IllegalStateException("Task does not have a workspace. Generate workspace first.")
                )
            }
            
            // 3. Get project
            val project = projectRepository.findById(task.projectId)
                ?: return Result.failure(ProjectNotFoundException("Project not found: ${task.projectId}"))
            
            // 4. Validate IDE type is configured for the project's repositories
            val repositories = repositoryRepository.findByProject(project.id)
            val isIDEConfigured = repositories.any { repo ->
                repo.preferredIDEs.contains(request.ideType)
            }
            
            if (!isIDEConfigured) {
                logger.warn { "IDE type ${request.ideType} is not configured for project ${project.name}" }
                // We'll allow it but log a warning - user might want to use a different IDE
            }
            
            // 5. Create task context
            val taskContext = TaskContext(
                taskId = task.id,
                title = task.title,
                description = task.description,
                projectName = project.name,
                branchName = task.branchName
            )
            
            // 6. Launch IDE
            val launchResult = ideService.launchIDE(
                ideType = request.ideType,
                workspacePath = task.workspacePath,
                taskContext = taskContext
            )
            
            if (launchResult.isFailure) {
                logger.error { "Failed to launch IDE: ${launchResult.exceptionOrNull()?.message}" }
                return Result.failure(
                    launchResult.exceptionOrNull() 
                        ?: Exception("Failed to launch IDE")
                )
            }
            
            // 7. Update task status to IN_PROGRESS if requested
            val updatedTask = if (request.updateTaskStatus && task.status == TaskStatus.PENDING) {
                val newTask = task.updateStatus(TaskStatus.IN_PROGRESS)
                taskRepository.update(newTask)
                logger.info { "Updated task ${task.id} status to IN_PROGRESS" }
                newTask
            } else {
                task
            }
            
            // 8. Record activity
            val activity = Activity(
                id = UUID.randomUUID(),
                type = ActivityType.IDE_LAUNCHED,
                entityType = "task",
                entityId = task.id,
                description = "Launched ${request.ideType} for task: ${task.title}",
                metadata = mapOf(
                    "ideType" to request.ideType.name,
                    "workspacePath" to task.workspacePath,
                    "projectName" to project.name
                ),
                createdAt = Instant.now()
            )
            activityRepository.create(activity)
            
            logger.info { "Successfully launched IDE for task ${task.id}" }
            Result.success(LaunchIDEResponse(updatedTask, activity))

        } catch (e: Exception) {
            logger.error(e) { "Error launching IDE for task ${request.taskId}" }
            Result.failure(e)
        }
    }
}

