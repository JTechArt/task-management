package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.OAuthConnection
import com.aitask.core.domain.model.OAuthConnectionStatus
import com.aitask.core.domain.model.OAuthProvider
import com.aitask.core.domain.repository.OAuthConnectionRepository
import com.aitask.core.domain.service.OAuthService

/**
 * Returns OAuth status per provider for UI display (AC2, AC3).
 */
class GetOAuthStatusUseCase(
    private val oauthConnectionRepository: OAuthConnectionRepository,
    private val oauthService: OAuthService
) {
    data class OAuthProviderStatus(
        val provider: OAuthProvider,
        val connection: OAuthConnection?,
        val status: OAuthConnectionStatus,
        val canConnect: Boolean,
        val message: String
    )

    suspend fun getStatusForProviders(providers: List<OAuthProvider>): List<OAuthProviderStatus> =
        providers.map { getStatus(it) }

    suspend fun getStatus(provider: OAuthProvider): OAuthProviderStatus {
        val connection = oauthConnectionRepository.findByProvider(provider)
        return when {
            connection == null -> OAuthProviderStatus(
                provider = provider,
                connection = null,
                status = OAuthConnectionStatus.DISCONNECTED,
                canConnect = true,
                message = "Not connected"
            )
            connection.isExpired -> OAuthProviderStatus(
                provider = provider,
                connection = connection,
                status = OAuthConnectionStatus.EXPIRED,
                canConnect = true,
                message = "Token expired; reconnect required"
            )
            else -> {
                val valid = oauthService.validateConnection(connection).getOrNull() ?: false
                OAuthProviderStatus(
                    provider = provider,
                    connection = connection,
                    status = if (valid) OAuthConnectionStatus.CONNECTED else OAuthConnectionStatus.INVALID,
                    canConnect = !valid,
                    message = if (valid) "Connected" else "Invalid authorization; reconnect required"
                )
            }
        }
    }
}
