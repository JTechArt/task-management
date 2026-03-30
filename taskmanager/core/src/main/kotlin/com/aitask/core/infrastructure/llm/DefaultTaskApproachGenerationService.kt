package com.aitask.core.infrastructure.llm

import com.aitask.core.domain.model.AgentToolKind
import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.TaskApproachStep
import com.aitask.core.domain.service.GeppaPromptOptimizationService
import com.aitask.core.domain.service.TaskApproachGenerationLlmOutput
import com.aitask.core.domain.service.TaskApproachGenerationService
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

class DefaultTaskApproachGenerationService(
    private val geppaPromptOptimizationService: GeppaPromptOptimizationService? = null,
    private val httpClient: HttpClient = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 3_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : TaskApproachGenerationService {
    override suspend fun generate(
        userPrompt: String,
        configuration: LlmConfiguration,
        apiKey: String?
    ): Result<TaskApproachGenerationLlmOutput> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = geppaPromptOptimizationService?.optimizeIfEnabled(userPrompt, "task_approach") ?: userPrompt
            val candidates = endpointCandidates(configuration.endpointUrl)
            var lastError: String? = null
            for (candidate in candidates) {
                val response = submit(candidate, configuration.modelIdentifier, prompt, apiKey)
                if (response.isSuccessful()) {
                    val generatedText = response.extractText(detectFlavor(candidate))
                    if (generatedText.isNotBlank()) {
                        val parsed = parseApproachJson(generatedText.trim())
                        return@runCatching TaskApproachGenerationLlmOutput(
                            summary = parsed.summary,
                            steps = parsed.steps,
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
                "Unable to generate a task solving approach from the configured local LLM." +
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
            ApproachLlmFlavor.OLLAMA -> setBody(ApproachOllamaGenerateRequest(model = modelIdentifier, prompt = prompt, stream = false))
            ApproachLlmFlavor.OPENAI -> setBody(
                ApproachOpenAiChatRequest(
                    model = modelIdentifier,
                    messages = listOf(
                        ApproachOpenAiMessage(role = "system", content = "You produce structured JSON plans for software tasks."),
                        ApproachOpenAiMessage(role = "user", content = prompt)
                    ),
                    temperature = 0.25
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

    private fun detectFlavor(endpointUrl: String): ApproachLlmFlavor =
        when {
            endpointUrl.contains("/v1/", ignoreCase = true) || endpointUrl.endsWith("/v1/chat/completions", ignoreCase = true) ->
                ApproachLlmFlavor.OPENAI
            else -> ApproachLlmFlavor.OLLAMA
        }

    private fun HttpResponse.isSuccessful(): Boolean = status.value in 200..299

    private suspend fun HttpResponse.extractText(flavor: ApproachLlmFlavor): String {
        val body = bodyAsText()
        return when (flavor) {
            ApproachLlmFlavor.OLLAMA -> {
                val parsed = json.decodeFromString(ApproachOllamaGenerateResponse.serializer(), body)
                parsed.response
            }
            ApproachLlmFlavor.OPENAI -> {
                val parsed = json.decodeFromString(ApproachOpenAiChatResponse.serializer(), body)
                parsed.choices.firstOrNull()?.message?.content.orEmpty()
            }
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun parseApproachJson(raw: String): ParsedApproach {
        val jsonText = extractJsonObject(raw)
            ?: throw IllegalStateException("The model did not return a JSON object. Try again.")
        val dto = json.decodeFromString(TaskApproachJsonDto.serializer(), jsonText)
        val summary = dto.summary.trim().ifBlank { "Solving approach" }
        val steps = dto.steps.map { stepDto ->
            TaskApproachStep(
                title = stepDto.title.trim().ifBlank { "Step" },
                detail = stepDto.detail.trim(),
                suggestedTools = stepDto.tools.mapNotNull { token ->
                    runCatching { AgentToolKind.valueOf(token.trim()) }.getOrNull()
                }.distinct(),
                prompt = stepDto.prompt?.trim()?.takeIf { it.isNotBlank() }
            )
        }.filter { it.title.isNotBlank() || it.detail.isNotBlank() }
        if (steps.isEmpty()) {
            throw IllegalStateException("The model returned no usable steps. Try again or adjust the task description.")
        }
        return ParsedApproach(summary = summary, steps = steps)
    }

    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) {
            return null
        }
        return text.substring(start, end + 1)
    }

    private data class ParsedApproach(
        val summary: String,
        val steps: List<TaskApproachStep>
    )
}

private enum class ApproachLlmFlavor {
    OLLAMA,
    OPENAI
}

@Serializable
private data class TaskApproachJsonDto(
    val summary: String = "",
    val steps: List<TaskApproachStepJsonDto> = emptyList()
)

@Serializable
private data class TaskApproachStepJsonDto(
    val title: String = "",
    val detail: String = "",
    val tools: List<String> = emptyList(),
    val prompt: String? = null
)

@Serializable
private data class ApproachOllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false
)

@Serializable
private data class ApproachOllamaGenerateResponse(
    val response: String = ""
)

@Serializable
private data class ApproachOpenAiChatRequest(
    val model: String,
    val messages: List<ApproachOpenAiMessage>,
    val temperature: Double = 0.25
)

@Serializable
private data class ApproachOpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
private data class ApproachOpenAiChatResponse(
    val choices: List<ApproachOpenAiChoice> = emptyList()
)

@Serializable
private data class ApproachOpenAiChoice(
    val message: ApproachOpenAiMessageResponse = ApproachOpenAiMessageResponse()
)

@Serializable
private data class ApproachOpenAiMessageResponse(
    val content: String = ""
)
