package com.aitask.core.domain.service

import com.aitask.core.domain.model.AuthType

data class GitAuthConfig(
    val authType: AuthType,
    val username: String? = null,
    val password: String? = null,
    val sshKeyPath: String? = null,
    val token: String? = null
) {
    override fun toString(): String {
        val hasPassword = password != null
        val hasToken = token != null
        val maskNote = when {
            hasPassword && hasToken -> "password=***, token=***"
            hasPassword -> "password=***"
            hasToken -> "token=***"
            else -> ""
        }
        return "GitAuthConfig(authType=$authType, username=$username, sshKeyPath=$sshKeyPath${
            if (maskNote.isNotEmpty()) ", $maskNote" else ""
        })"
    }
}

data class CloneProgress(
    val phase: String,
    val percentComplete: Int,
    val message: String
)

data class RepositoryInfo(
    val url: String,
    val isAccessible: Boolean,
    val defaultBranch: String? = null,
    val errorMessage: String? = null
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

    suspend fun validateRemoteRepository(
        url: String,
        authConfig: GitAuthConfig
    ): Result<RepositoryInfo>
}

