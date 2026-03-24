package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.CreateTaskRequest
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.validation.TaskValidator
import java.time.Instant
import java.util.UUID

class CreateTaskUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val activityRepository: ActivityRepository,
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
                methodologyOverride = request.methodologyOverride,
                bmadToolOverrideIds = if (
                    request.methodologyOverride == com.aitask.core.domain.model.Methodology.BMAD &&
                    request.bmadToolOverrideIds == null
                ) {
                    com.aitask.core.domain.model.BmadToolCatalog.defaultToolIds
                } else {
                    request.bmadToolOverrideIds
                },
                bmadInjectionEnabledOverride = request.bmadInjectionEnabledOverride,
                workspacePath = request.workspacePath,
                branchName = request.branchName,
                createdAt = now,
                updatedAt = now,
                completedAt = null
            )
            
            val createdTask = taskRepository.create(task)
            activityRepository.create(
                Activity(
                    id = UUID.randomUUID(),
                    type = ActivityType.TASK_CREATED,
                    entityType = "task",
                    entityId = createdTask.id,
                    description = "Created task: ${createdTask.title}",
                    metadata = mapOf("projectId" to createdTask.projectId.toString()),
                    status = ActivityStatus.SUCCESS,
                    createdAt = Instant.now(),
                    projectId = createdTask.projectId
                )
            )
            Result.success(createdTask)
        } catch (e: Exception) {
            val projectId = request.projectId
            activityRepository.create(
                Activity(
                    id = UUID.randomUUID(),
                    type = ActivityType.TASK_CREATED,
                    entityType = "task",
                    entityId = UUID.randomUUID(),
                    description = "Failed to create task: ${request.title}",
                    metadata = mapOf("error" to (e.message ?: "Unknown"), "projectId" to projectId.toString()),
                    status = ActivityStatus.FAILED,
                    createdAt = Instant.now(),
                    projectId = projectId
                )
            )
            Result.failure(e)
        }
    }
}
