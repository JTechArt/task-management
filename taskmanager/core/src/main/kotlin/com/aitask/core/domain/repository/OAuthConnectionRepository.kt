package com.aitask.core.domain.repository

import com.aitask.core.domain.model.OAuthConnection
import com.aitask.core.domain.model.OAuthProvider
import com.aitask.core.domain.model.OAuthToken
import java.util.UUID

/**
 * Persists OAuth connections. Token values are stored encrypted (AC4).
 */
interface OAuthConnectionRepository {
    suspend fun findByProvider(provider: OAuthProvider): OAuthConnection?
    suspend fun findById(id: UUID): OAuthConnection?
    suspend fun save(
        provider: OAuthProvider,
        token: OAuthToken,
        userId: String?,
        teamId: String?
    ): OAuthConnection
    suspend fun deleteByProvider(provider: OAuthProvider): Boolean
    suspend fun getStoredToken(connectionId: UUID): OAuthToken?
    suspend fun updateToken(connectionId: UUID, token: OAuthToken): Boolean
}
