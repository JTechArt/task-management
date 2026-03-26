package com.aitask.core.infrastructure.llm

import com.aitask.core.domain.model.GitAssistantSuggestionMode
import com.aitask.core.domain.model.GitAssistantSuggestionRequest
import com.aitask.core.domain.model.GitAssistantSuggestionResult
import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.service.GeppaPromptOptimizationService
import com.aitask.core.domain.service.GitAssistantSuggestionService
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

class DefaultGitAssistantSuggestionService(
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
) : GitAssistantSuggestionService {
    override suspend fun generate(
        request: GitAssistantSuggestionRequest,
        configuration: LlmConfiguration,
        apiKey: String?
    ): Result<GitAssistantSuggestionResult> = withContext(Dispatchers.IO) {
        runCatching {
            val rawPrompt = buildPrompt(request)
            val prompt = geppaPromptOptimizationService?.optimizeIfEnabled(rawPrompt, "git_suggestion") ?: rawPrompt
            val candidates = endpointCandidates(configuration.endpointUrl)
            var lastError: String? = null

            for (candidate in candidates) {
                val response = submit(candidate, configuration.modelIdentifier, prompt, apiKey)
                if (response.isSuccessful()) {
                    val generatedText = response.extractText(detectFlavor(candidate))
                    if (generatedText.isNotBlank()) {
                        return@runCatching GitAssistantSuggestionResult(
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
                "Unable to generate git assistant suggestion from the configured local LLM." +
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
            GitAssistantEndpointFlavor.OLLAMA -> setBody(GitOllamaGenerateRequest(model = modelIdentifier, prompt = prompt, stream = false))
            GitAssistantEndpointFlavor.OPENAI -> setBody(
                GitOpenAiChatRequest(
                    model = modelIdentifier,
                    messages = listOf(
                        GitOpenAiMessage(role = "system", content = "You write concise, practical git metadata for developers."),
                        GitOpenAiMessage(role = "user", content = prompt)
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

    private fun detectFlavor(endpointUrl: String): GitAssistantEndpointFlavor =
        when {
            endpointUrl.contains("/v1/", ignoreCase = true) || endpointUrl.endsWith("/v1/chat/completions", ignoreCase = true) -> GitAssistantEndpointFlavor.OPENAI
            else -> GitAssistantEndpointFlavor.OLLAMA
        }

    private fun buildPrompt(request: GitAssistantSuggestionRequest): String = buildString {
        appendLine("Generate ${request.mode.name.lowercase().replace('_', ' ')} for a software task.")
        appendLine("Task title: ${request.taskTitle}")
        request.projectName?.takeIf { it.isNotBlank() }?.let { appendLine("Project: $it") }
        request.branchName?.takeIf { it.isNotBlank() }?.let { appendLine("Branch: $it") }
        request.taskDescription?.takeIf { it.isNotBlank() }?.let { appendLine("Task context: $it") }
        if (request.repositoryNames.isNotEmpty()) {
            appendLine("Repositories: ${request.repositoryNames.joinToString(", ")}")
        }
        request.stagedChangesSummary?.takeIf { it.isNotBlank() }?.let {
            appendLine("Staged git context:")
            appendLine(it)
        }
        when (request.mode) {
            GitAssistantSuggestionMode.COMMIT_MESSAGE -> {
                appendLine("Write a concise commit message. Prefer a short subject line; include a body only if needed.")
                appendLine("Keep it grounded in the task context, branch context, and staged git context.")
            }
            GitAssistantSuggestionMode.PR_DESCRIPTION -> {
                appendLine("Write a pull request description with summary, key changes, and testing notes.")
                appendLine("Use clear bullet points or short sections.")
            }
            GitAssistantSuggestionMode.COMMENT_DRAFT -> {
                appendLine("Write a concise, professional comment draft that can be posted on a PR or issue.")
            }
        }
        appendLine("Return only the generated content. Do not include markdown fences or explanations.")
    }

    private fun HttpResponse.isSuccessful(): Boolean = status.value in 200..299

    private suspend fun HttpResponse.extractText(flavor: GitAssistantEndpointFlavor): String {
        val body = bodyAsText()
        return when (flavor) {
            GitAssistantEndpointFlavor.OLLAMA -> json.decodeFromString(GitOllamaGenerateResponse.serializer(), body).response
            GitAssistantEndpointFlavor.OPENAI -> json.decodeFromString(GitOpenAiChatResponse.serializer(), body).choices.firstOrNull()?.message?.content.orEmpty()
        }
    }

    private val json = Json { ignoreUnknownKeys = true }
}

private enum class GitAssistantEndpointFlavor {
    OLLAMA,
    OPENAI
}

@Serializable
private data class GitOllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false
)

@Serializable
private data class GitOllamaGenerateResponse(
    val response: String = ""
)

@Serializable
private data class GitOpenAiChatRequest(
    val model: String,
    val messages: List<GitOpenAiMessage>,
    val temperature: Double = 0.2
)

@Serializable
private data class GitOpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
private data class GitOpenAiChatResponse(
    val choices: List<GitOpenAiChoice> = emptyList()
)

@Serializable
private data class GitOpenAiChoice(
    val message: GitOpenAiMessageResponse = GitOpenAiMessageResponse()
)

@Serializable
private data class GitOpenAiMessageResponse(
    val content: String = ""
)
