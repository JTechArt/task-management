package com.aitask.core.domain.repository

import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import java.util.UUID

interface TaskRepository {
    suspend fun findById(id: UUID): Task?
    suspend fun findAll(): List<Task>
    suspend fun findByProject(projectId: UUID): List<Task>
    suspend fun findByProjectAndStatus(projectId: UUID, status: TaskStatus): List<Task>
    suspend fun findByStatus(status: TaskStatus): List<Task>
    suspend fun create(task: Task): Task
    suspend fun update(task: Task): Task
    suspend fun updateStatus(id: UUID, status: TaskStatus): Task?
    suspend fun delete(id: UUID)
}

