package com.aitask.core.infrastructure.claude

import com.aitask.core.domain.service.ClaudeAnthropicApiService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

private const val ANTHROPIC_VERSION: String = "2023-06-01"
private const val DEFAULT_MODEL: String = "claude-3-5-haiku-20241022"

class DefaultClaudeAnthropicApiService(
    private val httpClient: HttpClient = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 120_000
            socketTimeoutMillis = 120_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ClaudeAnthropicApiService {
    override suspend fun validateApiKey(apiKey: String, baseUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = apiKey.trim()
            if (trimmed.isEmpty()) {
                throw IllegalStateException("Anthropic API key is required for API mode.")
            }
            val url = normalizeBaseUrl(baseUrl)
            val response = httpClient.post("$url/v1/messages") {
                header("x-api-key", trimmed)
                header("anthropic-version", ANTHROPIC_VERSION)
                contentType(ContentType.Application.Json)
                setBody(
                    AnthropicMessagesRequest(
                        model = DEFAULT_MODEL,
                        maxTokens = 1,
                        messages = listOf(AnthropicMessageBody(role = "user", content = "ping"))
                    )
                )
            }
            when (response.status.value) {
                in 200..299 -> Unit
                401 -> throw IllegalStateException("Invalid Anthropic API key (HTTP 401).")
                else -> {
                    val body = response.bodyAsText()
                    logger.warn { "Anthropic validate failed: ${response.status} $body" }
                    throw IllegalStateException("Anthropic API error: HTTP ${response.status.value}")
                }
            }
        }
    }

    override suspend fun sendTaskContextPrompt(
        apiKey: String,
        baseUrl: String,
        userPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = apiKey.trim()
            if (trimmed.isEmpty()) {
                throw IllegalStateException("Anthropic API key is required for API mode.")
            }
            val url = normalizeBaseUrl(baseUrl)
            val response = httpClient.post("$url/v1/messages") {
                header("x-api-key", trimmed)
                header("anthropic-version", ANTHROPIC_VERSION)
                contentType(ContentType.Application.Json)
                setBody(
                    AnthropicMessagesRequest(
                        model = DEFAULT_MODEL,
                        maxTokens = 1024,
                        messages = listOf(AnthropicMessageBody(role = "user", content = userPrompt))
                    )
                )
            }
            if (response.status.value !in 200..299) {
                val errBody = response.bodyAsText()
                throw IllegalStateException("Anthropic API error: HTTP ${response.status.value} ${errBody.take(500)}")
            }
            val raw = response.bodyAsText()
            val parsed = json.decodeFromString(AnthropicMessagesResponse.serializer(), raw)
            val text = parsed.content.orEmpty()
                .firstOrNull { it.type == "text" }
                ?.text
                ?.trim()
                .orEmpty()
            if (text.isEmpty()) {
                "(empty response)"
            } else {
                text
            }
        }
    }

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        val withScheme: String = when {
            trimmed.isEmpty() -> "https://api.anthropic.com"
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
        return withScheme.trimEnd('/')
    }
}

@Serializable
private data class AnthropicMessagesRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<AnthropicMessageBody>
)

@Serializable
private data class AnthropicMessageBody(
    val role: String,
    val content: String
)

@Serializable
internal data class AnthropicMessagesResponse(
    val content: List<AnthropicContentBlock>? = null
)

@Serializable
internal data class AnthropicContentBlock(
    val type: String,
    val text: String? = null
)
