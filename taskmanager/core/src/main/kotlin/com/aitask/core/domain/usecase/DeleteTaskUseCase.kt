package com.aitask.core.domain.usecase

import com.aitask.core.domain.repository.TaskRepository
import java.util.UUID

class DeleteTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: UUID): Result<Unit> {
        return try {
            val existing = taskRepository.findById(id)
                ?: return Result.failure(TaskNotFoundException("Task with id $id not found"))
            
            taskRepository.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

