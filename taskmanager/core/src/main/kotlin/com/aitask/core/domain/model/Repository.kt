package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

enum class GitProvider {
    GITHUB,
    GITLAB,
    BITBUCKET,
    OTHER
}

enum class AuthType {
    SSH,
    HTTPS,
    TOKEN
}

enum class IDEType {
    CURSOR,
    VS_CODE,
    INTELLIJ_IDEA,
    WEBSTORM,
    PYCHARM,
    GOLAND
}

data class Repository(
    val id: UUID,
    val projectId: UUID,
    val name: String,
    val cloneUrl: String,
    val provider: GitProvider,
    val authType: AuthType,
    val preferredIDEs: List<IDEType> = emptyList(),
    val isPrimary: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun update(
        name: String? = null,
        cloneUrl: String? = null,
        provider: GitProvider? = null,
        authType: AuthType? = null,
        preferredIDEs: List<IDEType>? = null,
        isPrimary: Boolean? = null
    ): Repository = copy(
        name = name ?: this.name,
        cloneUrl = cloneUrl ?: this.cloneUrl,
        provider = provider ?: this.provider,
        authType = authType ?: this.authType,
        preferredIDEs = preferredIDEs ?: this.preferredIDEs,
        isPrimary = isPrimary ?: this.isPrimary,
        updatedAt = Instant.now()
    )
}

data class CreateRepositoryRequest(
    val projectId: UUID,
    val name: String,
    val cloneUrl: String,
    val provider: GitProvider,
    val authType: AuthType,
    val preferredIDEs: List<IDEType> = emptyList(),
    val isPrimary: Boolean = false
)

data class UpdateRepositoryRequest(
    val name: String? = null,
    val cloneUrl: String? = null,
    val provider: GitProvider? = null,
    val authType: AuthType? = null,
    val preferredIDEs: List<IDEType>? = null,
    val isPrimary: Boolean? = null
)

