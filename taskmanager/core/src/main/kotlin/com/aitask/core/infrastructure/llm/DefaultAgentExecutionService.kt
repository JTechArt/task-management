package com.aitask.core.infrastructure.llm

import com.aitask.core.domain.model.AgentExecutionResult
import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.service.AgentExecutionService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI

class DefaultAgentExecutionService(
    private val httpClient: HttpClient = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 3_000
            requestTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : AgentExecutionService {
    override suspend fun execute(
        prompt: String,
        configuration: LlmConfiguration,
        apiKey: String?
    ): Result<AgentExecutionResult> = withContext(Dispatchers.IO) {
        runCatching {
            val candidates = endpointCandidates(configuration.endpointUrl)
            var lastError: String? = null

            for (candidate in candidates) {
                val response = submit(candidate, configuration.modelIdentifier, prompt, apiKey)
                if (response.isSuccessful()) {
                    val generatedText = response.extractText(detectFlavor(candidate))
                    if (generatedText.isNotBlank()) {
                        return@runCatching AgentExecutionResult(
                            generatedText = generatedText.trim(),
                            configurationName = configuration.name,
                            modelIdentifier = configuration.modelIdentifier,
                            endpointUrl = candidate
                        )
                    }
                    lastError = "Empty response from $candidate"
                } else {
                    lastError = "HTTP ${response.status.value} from $candidate"
                }
            }

            throw IllegalStateException(
                "Unable to execute agent with the configured local LLM." +
                    (lastError?.let { " Last error: $it" } ?: "")
            )
        }
    }

    private suspend fun submit(
        endpointUrl: String,
        modelIdentifier: String,
        prompt: String,
        apiKey: String?
    ): HttpResponse = httpClient.post(endpointUrl) {
        contentType(ContentType.Application.Json)
        apiKey?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
        when (detectFlavor(endpointUrl)) {
            AgentEndpointFlavor.OLLAMA -> setBody(AgentOllamaGenerateRequest(model = modelIdentifier, prompt = prompt, stream = false))
            AgentEndpointFlavor.OPENAI -> setBody(
                AgentOpenAiChatRequest(
                    model = modelIdentifier,
                    messages = listOf(
                        AgentOpenAiMessage(role = "system", content = "You are a concise, capable automation agent."),
                        AgentOpenAiMessage(role = "user", content = prompt)
                    ),
                    temperature = 0.2
                )
            )
        }
    }

    private fun endpointCandidates(endpointUrl: String): List<String> {
        val normalized = endpointUrl.trim().trimEnd('/')
        val uri = URI(normalized)
        val explicitPath = uri.path?.takeIf { it.isNotBlank() && it != "/" }
        return if (explicitPath != null) {
            listOf(normalized)
        } else {
            listOf(
                "$normalized/api/generate",
                "$normalized/api/chat",
                "$normalized/v1/chat/completions"
            )
        }
    }

    private fun detectFlavor(endpointUrl: String): AgentEndpointFlavor =
        when {
            endpointUrl.contains("/v1/", ignoreCase = true) || endpointUrl.endsWith("/v1/chat/completions", ignoreCase = true) -> AgentEndpointFlavor.OPENAI
            else -> AgentEndpointFlavor.OLLAMA
        }

    private fun HttpResponse.isSuccessful(): Boolean = status.value in 200..299

    private suspend fun HttpResponse.extractText(flavor: AgentEndpointFlavor): String {
        val body = bodyAsText()
        return when (flavor) {
            AgentEndpointFlavor.OLLAMA -> json.decodeFromString(AgentOllamaGenerateResponse.serializer(), body).response
            AgentEndpointFlavor.OPENAI -> json.decodeFromString(AgentOpenAiChatResponse.serializer(), body).choices.firstOrNull()?.message?.content.orEmpty()
        }
    }

    private val json = Json { ignoreUnknownKeys = true }
}

private enum class AgentEndpointFlavor {
    OLLAMA,
    OPENAI
}

@Serializable
private data class AgentOllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false
)

@Serializable
private data class AgentOllamaGenerateResponse(
    val response: String = ""
)

@Serializable
private data class AgentOpenAiChatRequest(
    val model: String,
    val messages: List<AgentOpenAiMessage>,
    val temperature: Double = 0.2
)

@Serializable
private data class AgentOpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
private data class AgentOpenAiChatResponse(
    val choices: List<AgentOpenAiChoice> = emptyList()
)

@Serializable
private data class AgentOpenAiChoice(
    val message: AgentOpenAiMessageResponse = AgentOpenAiMessageResponse()
)

@Serializable
private data class AgentOpenAiMessageResponse(
    val content: String = ""
)

