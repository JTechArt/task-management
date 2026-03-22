package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Supported OAuth providers. Framework is reusable for multiple integration types (AC5).
 */
enum class OAuthProvider {
    SLACK,
    GITHUB,
    GITLAB,
    BITBUCKET;

    val displayName: String
        get() = when (this) {
            SLACK -> "Slack"
            GITHUB -> "GitHub"
            GITLAB -> "GitLab"
            BITBUCKET -> "Bitbucket"
        }
}

/**
 * OAuth token pair. Access token for API calls; refresh token for renewal.
 */
data class OAuthToken(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant?,
    val scope: String?
)

/**
 * Stored OAuth connection for a provider.
 * Tokens are stored encrypted; this model holds metadata only (AC4).
 */
data class OAuthConnection(
    val id: UUID,
    val provider: OAuthProvider,
    val userId: String?,
    val teamId: String?,
    val scope: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val expiresAt: Instant?
) {
    val isExpired: Boolean
        get() = expiresAt?.let { it.isBefore(Instant.now()) } ?: false
}

/**
 * Authentication state for UI display (AC2).
 */
enum class OAuthConnectionStatus {
    CONNECTED,
    EXPIRED,
    INVALID,
    DISCONNECTED
}
