package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.UpdateTaskRequest
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.TaskRepository
import java.time.Instant
import java.util.UUID

class UpdateTaskUseCase(
    private val taskRepository: TaskRepository,
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(id: UUID, request: UpdateTaskRequest): Result<Task> {
        return try {
            val existing = taskRepository.findById(id)
                ?: return Result.failure(TaskNotFoundException("Task with id $id not found"))
            if (existing.isArchived) {
                return Result.failure(ArchivedTaskException("Cannot update archived task"))
            }
            val updated = existing.update(
                title = request.title,
                description = request.description,
                taskType = request.taskType,
                status = request.status,
                methodologyOverride = if (request.clearMethodologyOverride) null else request.methodologyOverride ?: existing.methodologyOverride,
                bmadToolOverrideIds = when {
                    request.clearBmadToolOverrideIds -> null
                    request.bmadToolOverrideIds != null -> request.bmadToolOverrideIds
                    request.methodologyOverride == com.aitask.core.domain.model.Methodology.BMAD && existing.bmadToolOverrideIds == null ->
                        com.aitask.core.domain.model.BmadToolCatalog.defaultToolIds
                    else -> existing.bmadToolOverrideIds
                },
                workspacePath = request.workspacePath,
                branchName = request.branchName
            )
            val savedTask = taskRepository.update(updated)
            if (request.status != null && request.status != existing.status) {
                activityRepository.create(
                    Activity(
                        id = UUID.randomUUID(),
                        type = ActivityType.TASK_STATUS_CHANGED,
                        entityType = "task",
                        entityId = savedTask.id,
                        description = "Task status changed: ${existing.status.name} → ${savedTask.status.name}",
                        metadata = mapOf(
                            "projectId" to savedTask.projectId.toString(),
                            "previousStatus" to existing.status.name,
                            "newStatus" to savedTask.status.name
                        ),
                        status = ActivityStatus.SUCCESS,
                        createdAt = Instant.now(),
                        projectId = savedTask.projectId
                    )
                )
            }
            Result.success(savedTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class TaskNotFoundException(message: String) : Exception(message)
class ArchivedTaskException(message: String) : Exception(message)
