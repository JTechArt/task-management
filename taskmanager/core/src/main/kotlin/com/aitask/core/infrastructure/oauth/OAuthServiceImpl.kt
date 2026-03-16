package com.aitask.core.infrastructure.oauth

import com.aitask.core.config.OAuthConfig
import com.aitask.core.domain.model.OAuthConnection
import com.aitask.core.domain.model.OAuthProvider
import com.aitask.core.domain.model.OAuthToken
import com.aitask.core.domain.repository.OAuthConnectionRepository
import com.aitask.core.domain.service.EncryptionService
import com.aitask.core.domain.service.OAuthService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

private const val SLACK_AUTH_URL = "https://slack.com/oauth/v2/authorize"
private const val SLACK_TOKEN_URL = "https://slack.com/api/oauth.v2.access"
private const val SLACK_SCOPE = "chat:write,channels:read,incoming-webhook"

/**
 * OAuth2 service implementation. Reusable for multiple providers (AC5).
 * Slack is the first supported provider.
 */
class OAuthServiceImpl(
    private val oauthConfig: OAuthConfig,
    private val oauthConnectionRepository: OAuthConnectionRepository,
    private val encryptionService: EncryptionService,
    private val httpClient: HttpClient = HttpClient(CIO) { expectSuccess = false }
) : OAuthService {
    private val pendingState = ConcurrentHashMap<String, OAuthProvider>()

    override suspend fun buildAuthorizationUrl(provider: OAuthProvider): Result<Pair<String, String>> =
        when (provider) {
            OAuthProvider.SLACK -> buildSlackAuthUrl()
            else -> Result.failure(UnsupportedOAuthProviderException(provider))
        }

    private fun buildSlackAuthUrl(): Result<Pair<String, String>> {
        val clientId = oauthConfig.slackClientId
            ?: return Result.failure(IllegalStateException("SLACK_CLIENT_ID not configured"))
        if (!encryptionService.isAvailable()) {
            return Result.failure(IllegalStateException("OAuth encryption not configured. Set AITASK_OAUTH_ENCRYPTION_KEY."))
        }
        val state = generateState()
        pendingState[state] = OAuthProvider.SLACK
        val url = buildString {
            append(SLACK_AUTH_URL)
            append("?client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8))
            append("&scope=").append(URLEncoder.encode(SLACK_SCOPE, StandardCharsets.UTF_8))
            append("&redirect_uri=").append(URLEncoder.encode(oauthConfig.redirectUri, StandardCharsets.UTF_8))
            append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8))
        }
        return Result.success(url to state)
    }

    override suspend fun handleCallback(
        provider: OAuthProvider,
        code: String,
        state: String
    ): Result<OAuthConnection> {
        val expectedProvider = pendingState.remove(state)
            ?: return Result.failure(IllegalArgumentException("Invalid or expired OAuth state"))
        if (expectedProvider != provider) {
            return Result.failure(IllegalArgumentException("State mismatch"))
        }
        if (!encryptionService.isAvailable()) {
            return Result.failure(IllegalStateException("OAuth encryption not configured"))
        }
        return when (provider) {
            OAuthProvider.SLACK -> exchangeSlackCode(code)
            else -> Result.failure(UnsupportedOAuthProviderException(provider))
        }
    }

    private suspend fun exchangeSlackCode(code: String): Result<OAuthConnection> {
        val clientSecret = oauthConfig.slackClientSecret
            ?: return Result.failure(IllegalStateException("SLACK_CLIENT_SECRET not configured"))
        val response = httpClient.post(SLACK_TOKEN_URL) {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("client_id", oauthConfig.slackClientId!!)
                        append("client_secret", clientSecret)
                        append("code", code)
                        append("redirect_uri", oauthConfig.redirectUri)
                    }
                )
            )
        }
        if (response.status != HttpStatusCode.OK) {
            return Result.failure(OAuthExchangeException("Slack token exchange failed: ${response.status}"))
        }
        val body = response.bodyAsText()
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val parsed = runCatching {
            json.decodeFromString<SlackOAuthResponse>(body)
        }.getOrElse { e ->
            return Result.failure(OAuthExchangeException("Failed to parse Slack response: ${e.message}"))
        }
        if (!parsed.ok) {
            return Result.failure(OAuthExchangeException("Slack error: ${parsed.error ?: "unknown"}"))
        }
        val accessToken = parsed.accessToken ?: return Result.failure(OAuthExchangeException("No access token"))
        val token = OAuthToken(
            accessToken = accessToken,
            refreshToken = null,
            expiresAt = null,
            scope = parsed.scope
        )
        val connection = oauthConnectionRepository.save(
            provider = OAuthProvider.SLACK,
            token = token,
            userId = parsed.authedUser?.id,
            teamId = parsed.team?.id
        )
        return Result.success(connection)
    }

    override suspend fun refreshToken(connection: OAuthConnection): Result<OAuthToken> =
        when (connection.provider) {
            OAuthProvider.SLACK -> Result.failure(UnsupportedOperationException("Slack tokens do not expire; reconnect if needed"))
            else -> Result.failure(UnsupportedOAuthProviderException(connection.provider))
        }

    override suspend fun validateConnection(connection: OAuthConnection): Result<Boolean> {
        if (connection.isExpired) return Result.success(false)
        val token = oauthConnectionRepository.getStoredToken(connection.id) ?: return Result.success(false)
        return when (connection.provider) {
            OAuthProvider.SLACK -> validateSlackToken(token.accessToken)
            else -> Result.success(false)
        }
    }

    private suspend fun validateSlackToken(accessToken: String): Result<Boolean> = runCatching {
        val response = httpClient.get("https://slack.com/api/auth.test") {
            header("Authorization", "Bearer $accessToken")
        }
        if (response.status != HttpStatusCode.OK) return@runCatching false
        val body = response.bodyAsText()
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val parsed = json.decodeFromString<SlackAuthTestResponse>(body)
        parsed.ok
    }

    override suspend fun getValidAccessToken(provider: OAuthProvider): Result<String> {
        val connection = oauthConnectionRepository.findByProvider(provider) ?: return Result.failure(
            IllegalStateException("Not connected to ${provider.displayName}")
        )
        if (connection.isExpired) {
            return Result.failure(IllegalStateException("${provider.displayName} token expired; please reconnect"))
        }
        val token = oauthConnectionRepository.getStoredToken(connection.id) ?: return Result.failure(
            IllegalStateException("Could not retrieve token")
        )
        return Result.success(token.accessToken)
    }

    private fun generateState(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

@kotlinx.serialization.Serializable
private data class SlackOAuthResponse(
    val ok: Boolean = false,
    val accessToken: String? = null,
    val scope: String? = null,
    val tokenType: String? = null,
    val appId: String? = null,
    val team: SlackTeam? = null,
    val authedUser: SlackAuthedUser? = null,
    val error: String? = null
)

@kotlinx.serialization.Serializable
private data class SlackTeam(val id: String? = null, val name: String? = null)

@kotlinx.serialization.Serializable
private data class SlackAuthedUser(val id: String? = null)

@kotlinx.serialization.Serializable
private data class SlackAuthTestResponse(val ok: Boolean = false)

class UnsupportedOAuthProviderException(provider: OAuthProvider) :
    Exception("OAuth provider $provider not yet implemented")

class OAuthExchangeException(message: String, cause: Throwable? = null) : Exception(message, cause)
