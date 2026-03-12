package com.aitask.core.domain.service

import com.aitask.core.domain.model.AuthType

data class GitAuthConfig(
    val authType: AuthType,
    val username: String? = null,
    val password: String? = null,
    val sshKeyPath: String? = null,
    val token: String? = null
)

data class CloneProgress(
    val phase: String,
    val percentComplete: Int,
    val message: String
)

interface GitService {
    suspend fun cloneRepository(
        url: String,
        destination: String,
        authConfig: GitAuthConfig,
        shallow: Boolean = true,
        onProgress: ((CloneProgress) -> Unit)? = null
    ): Result<Unit>
    
    suspend fun createBranch(
        repositoryPath: String,
        branchName: String,
        checkout: Boolean = true
    ): Result<Unit>
    
    suspend fun validateRepository(path: String): Result<Boolean>
}

