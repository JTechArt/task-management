package com.aitask.core.domain.service

import com.aitask.core.domain.model.OAuthConnection
import com.aitask.core.domain.model.OAuthProvider
import com.aitask.core.domain.model.OAuthToken

/**
 * Reusable OAuth2 framework for external integrations (AC1, AC5).
 */
interface OAuthService {
    /**
     * Build authorization URL and return state for callback validation.
     * Caller opens URL in browser; user authorizes and is redirected to callback.
     */
    suspend fun buildAuthorizationUrl(provider: OAuthProvider): Result<Pair<String, String>>

    /**
     * Exchange authorization code for tokens and persist connection.
     */
    suspend fun handleCallback(
        provider: OAuthProvider,
        code: String,
        state: String
    ): Result<OAuthConnection>

    /**
     * Refresh expired token.
     */
    suspend fun refreshToken(connection: OAuthConnection): Result<OAuthToken>

    /**
     * Validate token with provider; detect expired or invalid (AC3).
     */
    suspend fun validateConnection(connection: OAuthConnection): Result<Boolean>

    /**
     * Get valid access token for API calls, refreshing if needed.
     */
    suspend fun getValidAccessToken(provider: OAuthProvider): Result<String>
}
