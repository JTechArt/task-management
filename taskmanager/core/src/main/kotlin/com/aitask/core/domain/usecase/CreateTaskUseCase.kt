package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.CreateTaskRequest
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.validation.TaskValidator
import java.time.Instant
import java.util.UUID

class CreateTaskUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val taskValidator: TaskValidator
) {
    suspend operator fun invoke(request: CreateTaskRequest): Result<Task> {
        return try {
            // Validate task
            val validation = taskValidator.validate(request)
            if (!validation.isValid) {
                return Result.failure(
                    ValidationException("Task validation failed: ${validation.errors.joinToString { it.message }}")
                )
            }
            
            // Verify project exists
            val project = projectRepository.findById(request.projectId)
                ?: return Result.failure(ProjectNotFoundException("Project with id ${request.projectId} not found"))
            
            if (project.isArchived) {
                return Result.failure(ArchivedProjectException("Cannot create task in archived project"))
            }
            
            // Create task
            val now = Instant.now()
            val task = Task(
                id = UUID.randomUUID(),
                title = request.title,
                description = request.description,
                taskType = request.taskType,
                status = TaskStatus.PENDING,
                projectId = request.projectId,
                workspacePath = request.workspacePath,
                branchName = request.branchName,
                createdAt = now,
                updatedAt = now,
                completedAt = null
            )
            
            val createdTask = taskRepository.create(task)
            Result.success(createdTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

