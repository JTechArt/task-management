package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

enum class TaskType {
    FEATURE,
    BUG_FIX,
    RESEARCH,
    ENHANCEMENT,
    DOCUMENTATION,
    REFACTORING
}

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    ARCHIVED
}

data class Task(
    val id: UUID,
    val title: String,
    val description: String?,
    val taskType: TaskType,
    val status: TaskStatus,
    val projectId: UUID,
    val methodologyOverride: Methodology? = null,
    val bmadToolOverrideIds: List<String>? = null,
    val workspacePath: String?,
    val workspaceCleanedAt: Instant? = null,
    val branchName: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant?
) {
    val isCompleted: Boolean
        get() = status == TaskStatus.COMPLETED
    
    val isArchived: Boolean
        get() = status == TaskStatus.ARCHIVED
    
    val isActive: Boolean
        get() = status == TaskStatus.PENDING || status == TaskStatus.IN_PROGRESS

    fun effectiveMethodology(projectMethodology: Methodology): Methodology =
        methodologyOverride ?: projectMethodology

    fun effectiveBmadToolIds(projectBmadToolIds: List<String>): List<String> =
        bmadToolOverrideIds ?: projectBmadToolIds
    
    fun updateStatus(newStatus: TaskStatus): Task {
        val now = Instant.now()
        return copy(
            status = newStatus,
            updatedAt = now,
            completedAt = if (newStatus == TaskStatus.COMPLETED) now else completedAt
        )
    }
    
    fun update(
        title: String? = null,
        description: String? = null,
        taskType: TaskType? = null,
        status: TaskStatus? = null,
        methodologyOverride: Methodology? = this.methodologyOverride,
        bmadToolOverrideIds: List<String>? = this.bmadToolOverrideIds,
        workspacePath: String? = null,
        branchName: String? = null
    ): Task {
        val now = Instant.now()
        val newStatus = status ?: this.status
        return copy(
            title = title ?: this.title,
            description = description ?: this.description,
            taskType = taskType ?: this.taskType,
            status = newStatus,
            methodologyOverride = methodologyOverride,
            bmadToolOverrideIds = bmadToolOverrideIds,
            workspacePath = workspacePath ?: this.workspacePath,
            branchName = branchName ?: this.branchName,
            updatedAt = now,
            completedAt = if (newStatus == TaskStatus.COMPLETED && this.status != TaskStatus.COMPLETED) {
                now
            } else {
                this.completedAt
            }
        )
    }
}

data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    val taskType: TaskType,
    val projectId: UUID,
    val methodologyOverride: Methodology? = null,
    val bmadToolOverrideIds: List<String>? = null,
    val workspacePath: String? = null,
    val branchName: String? = null
)

data class UpdateTaskRequest(
    val title: String? = null,
    val description: String? = null,
    val taskType: TaskType? = null,
    val status: TaskStatus? = null,
    val methodologyOverride: Methodology? = null,
    val clearMethodologyOverride: Boolean = false,
    val bmadToolOverrideIds: List<String>? = null,
    val clearBmadToolOverrideIds: Boolean = false,
    val workspacePath: String? = null,
    val branchName: String? = null
)
