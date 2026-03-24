package com.aitask.core.data.repository

import com.aitask.core.data.entity.Tasks
import com.aitask.core.domain.model.Methodology
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.TaskType
import com.aitask.core.domain.repository.TaskRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

class TaskRepositoryImpl : TaskRepository {
    
    override suspend fun findById(id: UUID): Task? = newSuspendedTransaction {
        Tasks.selectAll().where { Tasks.id eq id }
            .map { it.toTask() }
            .singleOrNull()
    }
    
    override suspend fun findAll(): List<Task> = newSuspendedTransaction {
        Tasks.selectAll()
            .orderBy(Tasks.createdAt to SortOrder.DESC)
            .map { it.toTask() }
    }
    
    override suspend fun findByProject(projectId: UUID): List<Task> = newSuspendedTransaction {
        Tasks.selectAll().where { Tasks.projectId eq projectId }
            .orderBy(Tasks.createdAt to SortOrder.DESC)
            .map { it.toTask() }
    }
    
    override suspend fun findByProjectAndStatus(projectId: UUID, status: TaskStatus): List<Task> = 
        newSuspendedTransaction {
            Tasks.selectAll().where {
                (Tasks.projectId eq projectId) and (Tasks.status eq status.name)
            }
                .orderBy(Tasks.createdAt to SortOrder.DESC)
                .map { it.toTask() }
        }
    
    override suspend fun findByStatus(status: TaskStatus): List<Task> = newSuspendedTransaction {
        Tasks.selectAll().where { Tasks.status eq status.name }
            .orderBy(Tasks.createdAt to SortOrder.DESC)
            .map { it.toTask() }
    }
    
    override suspend fun create(task: Task): Task = newSuspendedTransaction {
        Tasks.insert {
            it[id] = task.id
            it[title] = task.title
            it[description] = task.description
            it[taskType] = task.taskType.name
            it[status] = task.status.name
            it[projectId] = task.projectId
            it[methodologyOverride] = task.methodologyOverride?.name
            it[bmadToolOverrideIds] = task.bmadToolOverrideIds
            it[bmadInjectionEnabledOverride] = task.bmadInjectionEnabledOverride
            it[workspacePath] = task.workspacePath
            it[branchName] = task.branchName
            it[workspaceCleanedAt] = task.workspaceCleanedAt
            it[createdAt] = task.createdAt
            it[updatedAt] = task.updatedAt
            it[completedAt] = task.completedAt
        }
        task
    }
    
    override suspend fun update(task: Task): Task = newSuspendedTransaction {
        Tasks.update({ Tasks.id eq task.id }) {
            it[title] = task.title
            it[description] = task.description
            it[taskType] = task.taskType.name
            it[status] = task.status.name
            it[methodologyOverride] = task.methodologyOverride?.name
            it[bmadToolOverrideIds] = task.bmadToolOverrideIds
            it[bmadInjectionEnabledOverride] = task.bmadInjectionEnabledOverride
            it[workspacePath] = task.workspacePath
            it[branchName] = task.branchName
            it[workspaceCleanedAt] = task.workspaceCleanedAt
            it[updatedAt] = Instant.now()
            it[completedAt] = task.completedAt
        }
        task.copy(updatedAt = Instant.now())
    }
    
    override suspend fun updateStatus(id: UUID, status: TaskStatus): Task? = newSuspendedTransaction {
        val now = Instant.now()
        Tasks.update({ Tasks.id eq id }) {
            it[Tasks.status] = status.name
            it[updatedAt] = now
            if (status == TaskStatus.COMPLETED) {
                it[completedAt] = now
            }
        }
        findById(id)
    }
    
    override suspend fun delete(id: UUID) = newSuspendedTransaction {
        Tasks.deleteWhere { Tasks.id eq id }
        Unit
    }
    
    private fun ResultRow.toTask() = Task(
        id = this[Tasks.id].value,
        title = this[Tasks.title],
        description = this[Tasks.description],
        taskType = TaskType.valueOf(this[Tasks.taskType]),
        status = TaskStatus.valueOf(this[Tasks.status]),
        projectId = this[Tasks.projectId].value,
        methodologyOverride = this[Tasks.methodologyOverride]?.let(Methodology::valueOf),
        bmadToolOverrideIds = this[Tasks.bmadToolOverrideIds],
        bmadInjectionEnabledOverride = this[Tasks.bmadInjectionEnabledOverride],
        workspacePath = this[Tasks.workspacePath],
        branchName = this[Tasks.branchName],
        workspaceCleanedAt = this[Tasks.workspaceCleanedAt],
        createdAt = this[Tasks.createdAt],
        updatedAt = this[Tasks.updatedAt],
        completedAt = this[Tasks.completedAt]
    )
}
