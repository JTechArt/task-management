package com.aitask.core.infrastructure.oauth

import com.aitask.core.config.OAuthConfig
import com.aitask.core.domain.model.OAuthConnection
import com.aitask.core.domain.model.OAuthProvider
import com.aitask.core.domain.model.OAuthToken
import com.aitask.core.domain.repository.OAuthConnectionRepository
import com.aitask.core.domain.service.EncryptionService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class OAuthServiceImplTest {

    private val oauthConfig = OAuthConfig(
        slackClientId = "slack-client-id",
        slackClientSecret = "slack-secret",
        redirectUri = "http://localhost:38473/oauth/callback"
    )
    private val oauthConnectionRepository = mockk<OAuthConnectionRepository>()
    private val encryptionService = mockk<EncryptionService>()

    @Test
    fun `buildAuthorizationUrl returns failure when Slack client ID not configured`() = runTest {
        val config = OAuthConfig(slackClientId = null, slackClientSecret = "secret", redirectUri = "http://localhost/cb")
        val service = OAuthServiceImpl(config, oauthConnectionRepository, encryptionService)
        coEvery { encryptionService.isAvailable() } returns true
        val result = service.buildAuthorizationUrl(OAuthProvider.SLACK)
        assertTrue(result.isFailure)
    }

    @Test
    fun `buildAuthorizationUrl returns failure when encryption not available`() = runTest {
        val service = OAuthServiceImpl(oauthConfig, oauthConnectionRepository, encryptionService)
        coEvery { encryptionService.isAvailable() } returns false
        val result = service.buildAuthorizationUrl(OAuthProvider.SLACK)
        assertTrue(result.isFailure)
    }

    @Test
    fun `buildAuthorizationUrl returns url and state when configured`() = runTest {
        val service = OAuthServiceImpl(oauthConfig, oauthConnectionRepository, encryptionService)
        coEvery { encryptionService.isAvailable() } returns true
        val result = service.buildAuthorizationUrl(OAuthProvider.SLACK)
        assertTrue(result.isSuccess)
        val (url, state) = result.getOrThrow()
        assertTrue(url.startsWith("https://slack.com/oauth/v2/authorize?"))
        assertTrue(url.contains("client_id=slack-client-id"))
        assertTrue(url.contains("redirect_uri="))
        assertTrue(url.contains("state=$state"))
        assertTrue(state.length > 10)
    }

    @Test
    fun `handleCallback returns failure for invalid or expired state`() = runTest {
        val service = OAuthServiceImpl(oauthConfig, oauthConnectionRepository, encryptionService)
        coEvery { encryptionService.isAvailable() } returns true
        val result = service.handleCallback(OAuthProvider.SLACK, "auth-code", "unknown-state")
        assertTrue(result.isFailure)
    }

    @Test
    fun `handleCallback returns failure when provider mismatch with state`() = runTest {
        val service = OAuthServiceImpl(oauthConfig, oauthConnectionRepository, encryptionService)
        coEvery { encryptionService.isAvailable() } returns true
        val (_, state) = service.buildAuthorizationUrl(OAuthProvider.SLACK).getOrThrow()
        val result = service.handleCallback(OAuthProvider.GITHUB, "code", state)
        assertTrue(result.isFailure)
    }

    @Test
    fun `validateConnection returns false when connection is expired`() = runTest {
        val service = OAuthServiceImpl(oauthConfig, oauthConnectionRepository, encryptionService)
        val expiredConnection = OAuthConnection(
            id = java.util.UUID.randomUUID(),
            provider = OAuthProvider.SLACK,
            userId = "U123",
            teamId = "T456",
            scope = "chat:write",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            expiresAt = Instant.now().minusSeconds(3600)
        )
        val result = service.validateConnection(expiredConnection)
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow())
    }

    @Test
    fun `validateConnection returns false when no stored token`() = runTest {
        val service = OAuthServiceImpl(oauthConfig, oauthConnectionRepository, encryptionService)
        val connection = OAuthConnection(
            id = java.util.UUID.randomUUID(),
            provider = OAuthProvider.SLACK,
            userId = "U123",
            teamId = "T456",
            scope = "chat:write",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            expiresAt = null
        )
        coEvery { oauthConnectionRepository.getStoredToken(connection.id) } returns null
        val result = service.validateConnection(connection)
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow())
    }

    @Test
    fun `buildAuthorizationUrl returns failure for unsupported provider`() = runTest {
        val service = OAuthServiceImpl(oauthConfig, oauthConnectionRepository, encryptionService)
        val result = service.buildAuthorizationUrl(OAuthProvider.GITHUB)
        assertTrue(result.isFailure)
    }
}
