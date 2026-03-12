package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.repository.TaskRepository
import java.util.UUID

class GetTasksUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        projectId: UUID? = null,
        status: TaskStatus? = null
    ): Result<List<Task>> {
        return try {
            val tasks = when {
                projectId != null && status != null -> 
                    taskRepository.findByProjectAndStatus(projectId, status)
                projectId != null -> 
                    taskRepository.findByProject(projectId)
                status != null -> 
                    taskRepository.findByStatus(status)
                else -> 
                    taskRepository.findAll()
            }
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

