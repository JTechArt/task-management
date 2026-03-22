package com.aitask.core.domain.model
import java.time.Instant
import java.util.UUID

enum class WorkspaceStatus {
    PENDING,
    CREATING,
    CLONING,
    COMPLETED,
    FAILED
}

enum class CloneStatus {
    PENDING,
    CLONING,
    COMPLETED,
    FAILED
}

data class WorkspaceRepository(
    val repositoryId: UUID,
    val localPath: String,
    val branchName: String?,
    val cloneStatus: CloneStatus,
    val errorMessage: String? = null
)

data class Workspace(
    val taskId: UUID,
    val projectId: UUID,
    val path: String,
    val repositories: List<WorkspaceRepository>,
    val appliedRules: List<UUID> = emptyList(),
    val status: WorkspaceStatus,
    val createdAt: Instant,
    val errorMessage: String? = null
) {
    val isCompleted: Boolean
        get() = status == WorkspaceStatus.COMPLETED
    
    val isFailed: Boolean
        get() = status == WorkspaceStatus.FAILED
    
    val isInProgress: Boolean
        get() = status == WorkspaceStatus.CREATING || status == WorkspaceStatus.CLONING
}

data class CreateWorkspaceRequest(
    val taskId: UUID,
    val projectId: UUID,
    val selectedRepositories: List<UUID> = emptyList(),
    val ideType: IDEType? = null
)

data class WorkspaceMetadata(
    val taskId: UUID,
    val projectId: UUID,
    val createdAt: Instant,
    val repositories: List<WorkspaceRepositoryMetadata>,
    val appliedRules: List<UUID>
)

data class WorkspaceRepositoryMetadata(
    val repositoryId: UUID,
    val localPath: String,
    val branchName: String?,
    val cloneStatus: String
)
