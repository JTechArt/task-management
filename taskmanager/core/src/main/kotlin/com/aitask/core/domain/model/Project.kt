package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

data class Project(
    val id: UUID,
    val name: String,
    val description: String?,
    val workspacePath: String,
    val branchTemplate: String = "task-{taskId}",
    val tags: List<String> = emptyList(),
    val team: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val archivedAt: Instant? = null
) {
    val isArchived: Boolean
        get() = archivedAt != null
    
    fun archive(): Project = copy(archivedAt = Instant.now())
    
    fun update(
        name: String? = null,
        description: String? = null,
        workspacePath: String? = null,
        branchTemplate: String? = null,
        tags: List<String>? = null,
        team: String? = null
    ): Project = copy(
        name = name ?: this.name,
        description = description ?: this.description,
        workspacePath = workspacePath ?: this.workspacePath,
        branchTemplate = branchTemplate ?: this.branchTemplate,
        tags = tags ?: this.tags,
        team = team ?: this.team,
        updatedAt = Instant.now()
    )
}

data class CreateProjectRequest(
    val name: String,
    val description: String? = null,
    val workspacePath: String,
    val branchTemplate: String = "task-{taskId}",
    val tags: List<String> = emptyList(),
    val team: String? = null
)

data class UpdateProjectRequest(
    val name: String? = null,
    val description: String? = null,
    val workspacePath: String? = null,
    val branchTemplate: String? = null,
    val tags: List<String>? = null,
    val team: String? = null
)

