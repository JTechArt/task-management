package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.TaskRepository
import java.time.Instant
import java.util.UUID

class DeleteTaskUseCase(
    private val taskRepository: TaskRepository,
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(id: UUID): Result<Unit> {
        return try {
            val existing = taskRepository.findById(id)
                ?: return Result.failure(TaskNotFoundException("Task with id $id not found"))
            activityRepository.create(
                Activity(
                    id = UUID.randomUUID(),
                    type = ActivityType.TASK_DELETED,
                    entityType = "task",
                    entityId = existing.id,
                    description = "Deleted task: ${existing.title}",
                    metadata = mapOf("projectId" to existing.projectId.toString()),
                    status = ActivityStatus.SUCCESS,
                    createdAt = Instant.now(),
                    projectId = existing.projectId
                )
            )
            taskRepository.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

