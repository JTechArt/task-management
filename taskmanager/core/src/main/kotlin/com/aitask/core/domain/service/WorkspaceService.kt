package com.aitask.core.domain.service

import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Repository
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.Workspace

enum class RetentionPolicy {
    KEEP_ALL,
    DELETE_ON_COMPLETION,
    DELETE_AFTER_DAYS
}

interface WorkspaceService {
    suspend fun createWorkspace(
        task: Task,
        project: Project,
        selectedRepositories: List<Repository>
    ): Result<Workspace>
    
    suspend fun prepareWorkspace(
        workspace: Workspace,
        repositories: List<Repository>,
        onProgress: ((String) -> Unit)? = null
    ): Result<Workspace>
    
    suspend fun cleanupWorkspace(
        workspacePath: String,
        retentionPolicy: RetentionPolicy
    ): Result<Unit>
    
    suspend fun getWorkspacePath(task: Task, project: Project): String
}

