package com.aitask.core.infrastructure.llm

import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.TaskContentGenerationMode
import com.aitask.core.domain.model.TaskContentGenerationRequest
import com.aitask.core.domain.model.TaskContentGenerationResult
import com.aitask.core.domain.service.GeppaPromptOptimizationService
import com.aitask.core.domain.service.TaskContentGenerationService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI

class DefaultTaskContentGenerationService(
    private val geppaPromptOptimizationService: GeppaPromptOptimizationService? = null,
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
) : TaskContentGenerationService {
    override suspend fun generate(
        request: TaskContentGenerationRequest,
        configuration: LlmConfiguration,
        apiKey: String?
    ): Result<TaskContentGenerationResult> = withContext(Dispatchers.IO) {
        runCatching {
            val rawPrompt = buildPrompt(request)
            val prompt = geppaPromptOptimizationService?.optimizeIfEnabled(rawPrompt, "task_generation") ?: rawPrompt
            val candidates = endpointCandidates(configuration.endpointUrl)
            var lastError: String? = null

            for (candidate in candidates) {
                val response = submit(candidate, configuration.modelIdentifier, prompt, apiKey)
                if (response.isSuccessful()) {
                    val generatedText = response.extractText(detectFlavor(candidate))
                    if (generatedText.isNotBlank()) {
                        return@runCatching TaskContentGenerationResult(
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
                "Unable to generate task content from the configured local LLM." +
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
            LlmEndpointFlavor.OLLAMA -> setBody(OllamaGenerateRequest(model = modelIdentifier, prompt = prompt, stream = false))
            LlmEndpointFlavor.OPENAI -> setBody(
                OpenAiChatRequest(
                    model = modelIdentifier,
                    messages = listOf(
                        OpenAiMessage(role = "system", content = "You write concise, practical task content for developers."),
                        OpenAiMessage(role = "user", content = prompt)
                    ),
                    temperature = 0.3
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

    private fun detectFlavor(endpointUrl: String): LlmEndpointFlavor =
        when {
            endpointUrl.contains("/v1/", ignoreCase = true) || endpointUrl.endsWith("/v1/chat/completions", ignoreCase = true) -> LlmEndpointFlavor.OPENAI
            else -> LlmEndpointFlavor.OLLAMA
        }

    private fun buildPrompt(request: TaskContentGenerationRequest): String = buildString {
        appendLine("Create a ${request.mode.name.lowercase()} for a software task.")
        appendLine("Task title: ${request.title}")
        appendLine("Task type: ${request.taskType.name.replace('_', ' ').lowercase()}")
        request.projectName?.takeIf { it.isNotBlank() }?.let {
            appendLine("Project: $it")
        }
        request.currentDescription?.takeIf { it.isNotBlank() }?.let {
            appendLine("Current description: $it")
        }
        when (request.mode) {
            TaskContentGenerationMode.DESCRIPTION -> {
                appendLine("Write 2-4 concise paragraphs that can serve as a task description.")
                appendLine("Include enough context to be actionable, but do not invent requirements that are not implied by the title.")
            }
            TaskContentGenerationMode.SUMMARY -> {
                appendLine("Write 1-2 concise sentences that summarize the task for a busy developer.")
            }
        }
        appendLine("Return only the generated content. Do not include markdown fences or explanations.")
    }

    private fun HttpResponse.isSuccessful(): Boolean = status.value in 200..299

    private suspend fun HttpResponse.extractText(flavor: LlmEndpointFlavor): String {
        val body = bodyAsText()
        return when (flavor) {
            LlmEndpointFlavor.OLLAMA -> {
                val parsed = json.decodeFromString(OllamaGenerateResponse.serializer(), body)
                parsed.response
            }
            LlmEndpointFlavor.OPENAI -> {
                val parsed = json.decodeFromString(OpenAiChatResponse.serializer(), body)
                parsed.choices.firstOrNull()?.message?.content.orEmpty()
            }
        }
    }

    private val json = Json { ignoreUnknownKeys = true }
}

private enum class LlmEndpointFlavor {
    OLLAMA,
    OPENAI
}

@Serializable
private data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false
)

@Serializable
private data class OllamaGenerateResponse(
    val response: String = ""
)

@Serializable
private data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.3
)

@Serializable
private data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
private data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList()
)

@Serializable
private data class OpenAiChoice(
    val message: OpenAiMessageResponse = OpenAiMessageResponse()
)

@Serializable
private data class OpenAiMessageResponse(
    val content: String = ""
)
