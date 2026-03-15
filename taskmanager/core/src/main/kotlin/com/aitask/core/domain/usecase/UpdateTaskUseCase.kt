package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.UpdateTaskRequest
import com.aitask.core.domain.repository.TaskRepository
import java.util.UUID

class UpdateTaskUseCase(
    private val taskRepository: TaskRepository
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
                workspacePath = request.workspacePath,
                branchName = request.branchName
            )
            
            val savedTask = taskRepository.update(updated)
            Result.success(savedTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class TaskNotFoundException(message: String) : Exception(message)
class ArchivedTaskException(message: String) : Exception(message)

