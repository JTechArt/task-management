package com.aitask.core.infrastructure.slack

import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.SlackChannelMessage
import com.aitask.core.domain.model.SlackConversationSummaryItem
import com.aitask.core.domain.service.SlackChannelSummaryBatch
import com.aitask.core.domain.service.SlackSummaryGenerationService
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
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URI

class DefaultSlackSummaryGenerationService(
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
) : SlackSummaryGenerationService {
    private val json: Json = Json { ignoreUnknownKeys = true }
    override suspend fun summarizeChannelDays(
        channelId: String,
        messagesByDay: Map<String, List<SlackChannelMessage>>,
        configuration: LlmConfiguration?,
        apiKey: String?
    ): SlackChannelSummaryBatch = withContext(Dispatchers.IO) {
        val items: MutableList<SlackConversationSummaryItem> = mutableListOf()
        val diagnostics: MutableList<String> = mutableListOf()
        val channelUrl: String = buildChannelUrl(channelId = channelId)
        val sortedDays: List<String> = messagesByDay.keys.sorted()
        for (day: String in sortedDays) {
            val dayMessages: List<SlackChannelMessage> = messagesByDay[day].orEmpty()
            if (dayMessages.isEmpty()) {
                continue
            }
            if (configuration == null) {
                items.add(
                    buildFallbackSingleTopic(
                        channelId = channelId,
                        day = day,
                        messages = dayMessages,
                        channelUrl = channelUrl,
                        degraded = true
                    )
                )
                diagnostics.add("$day: no default LLM profile; used digest fallback")
                continue
            }
            try {
                val prompt: String = buildTopicsPrompt(channelId = channelId, day = day, messages = dayMessages)
                val raw: String = submitChat(configuration = configuration, apiKey = apiKey, userPrompt = prompt)
                val parsed: List<SlackConversationSummaryItem> = parseTopicsFromModel(
                    rawText = raw,
                    channelId = channelId,
                    day = day,
                    messages = dayMessages,
                    channelUrl = channelUrl
                )
                if (parsed.isEmpty()) {
                    items.add(
                        buildFallbackSingleTopic(
                            channelId = channelId,
                            day = day,
                            messages = dayMessages,
                            channelUrl = channelUrl,
                            degraded = true
                        )
                    )
                    diagnostics.add("$day: model returned no topics; used single-topic fallback")
                } else {
                    items.addAll(parsed)
                }
            } catch (e: Exception) {
                diagnostics.add("$day: ${e.message ?: "summary generation failed"}")
                items.add(
                    buildFallbackSingleTopic(
                        channelId = channelId,
                        day = day,
                        messages = dayMessages,
                        channelUrl = channelUrl,
                        degraded = true
                    )
                )
            }
        }
        SlackChannelSummaryBatch(items = items, diagnostics = diagnostics)
    }

    private fun buildChannelUrl(channelId: String): String = "https://slack.com/archives/$channelId"

    private fun buildFallbackSingleTopic(
        channelId: String,
        day: String,
        messages: List<SlackChannelMessage>,
        channelUrl: String,
        degraded: Boolean
    ): SlackConversationSummaryItem {
        val digest: String = messages.take(40).joinToString(separator = "\n") { message: SlackChannelMessage ->
            "[${message.ts}] ${message.text}"
        }.let { text: String ->
            if (text.length > 4_000) text.take(4_000) + "…" else text
        }
        val anchorTs: String = messages.first().ts
        return SlackConversationSummaryItem(
            analysisDay = day,
            channelId = channelId,
            topicLabel = "Channel activity",
            category = "general",
            summaryText = digest,
            channelUrl = channelUrl,
            messageUrl = buildSlackMessageUrl(channelId = channelId, messageTs = anchorTs),
            threadUrl = messages.first().threadTs?.let { ts: String -> buildSlackMessageUrl(channelId = channelId, messageTs = ts) },
            threadRootTimestamp = messages.first().threadTs,
            sourceMessageTimestamp = anchorTs,
            degradedSingleTopicFallback = degraded
        )
    }

    private fun buildTopicsPrompt(channelId: String, day: String, messages: List<SlackChannelMessage>): String {
        val payload: String = buildJsonArray {
            messages.forEach { message: SlackChannelMessage ->
                add(
                    buildJsonObject {
                        put("ts", message.ts)
                        put("text", message.text)
                        message.threadTs?.let { put("thread_ts", it) }
                        message.userId?.let { put("user", it) }
                    }
                )
            }
        }.toString()
        val capped: String = if (payload.length > 12_000) payload.take(12_000) + "…" else payload
        return buildString {
            appendLine("You summarize Slack channel threads for auditing.")
            appendLine("Channel id: $channelId")
            appendLine("UTC day: $day")
            appendLine("Messages JSON:")
            appendLine(capped)
            appendLine()
            appendLine(
                "Return ONLY valid JSON (no markdown fences) with this exact shape: " +
                    "{\"topics\":[{\"label\":\"short title\",\"category\":\"one of planning|discussion|incident|release|decision|general|other\"," +
                    "\"summary\":\"2-5 sentences\",\"anchor_ts\":\"must match a ts from input\",\"thread_ts\":null or a thread root ts from input\"}]}."
            )
            appendLine("If discussions clearly belong to different subjects, emit multiple topics; otherwise one topic is fine.")
            appendLine("anchor_ts must be copied exactly from the input messages.")
        }
    }

    private suspend fun submitChat(configuration: LlmConfiguration, apiKey: String?, userPrompt: String): String {
        val candidates: List<String> = endpointCandidates(endpointUrl = configuration.endpointUrl)
        var lastError: String? = null
        for (candidate: String in candidates) {
            val response: HttpResponse = httpClient.post(candidate) {
                contentType(ContentType.Application.Json)
                apiKey?.takeIf { it.isNotBlank() }?.let { key: String -> header("Authorization", "Bearer $key") }
                when (detectFlavor(endpointUrl = candidate)) {
                    SummaryEndpointFlavor.OLLAMA -> setBody(
                        OllamaGenerateRequest(model = configuration.modelIdentifier, prompt = userPrompt, stream = false)
                    )
                    SummaryEndpointFlavor.OPENAI -> setBody(
                        OpenAiChatRequest(
                            model = configuration.modelIdentifier,
                            messages = listOf(
                                OpenAiMessage(role = "system", content = "You output only compact JSON for Slack summaries."),
                                OpenAiMessage(role = "user", content = userPrompt)
                            ),
                            temperature = 0.2
                        )
                    )
                }
            }
            if (response.status.value !in 200..299) {
                lastError = "HTTP ${response.status.value} from $candidate"
                continue
            }
            val body: String = response.bodyAsText()
            val text: String = when (detectFlavor(endpointUrl = candidate)) {
                SummaryEndpointFlavor.OLLAMA -> json.decodeFromString(OllamaGenerateResponse.serializer(), body).response
                SummaryEndpointFlavor.OPENAI -> json.decodeFromString(OpenAiChatResponse.serializer(), body)
                    .choices.firstOrNull()?.message?.content.orEmpty()
            }
            if (text.isNotBlank()) {
                return text.trim()
            }
            lastError = "Empty response from $candidate"
        }
        throw IllegalStateException("Unable to generate Slack summaries. ${lastError ?: ""}".trim())
    }

    private fun endpointCandidates(endpointUrl: String): List<String> {
        val normalized: String = endpointUrl.trim().trimEnd('/')
        val uri: URI = URI.create(normalized)
        val explicitPath: String? = uri.path?.takeIf { it.isNotBlank() && it != "/" }
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

    private fun detectFlavor(endpointUrl: String): SummaryEndpointFlavor =
        when {
            endpointUrl.contains("/v1/", ignoreCase = true) || endpointUrl.endsWith("/v1/chat/completions", ignoreCase = true) ->
                SummaryEndpointFlavor.OPENAI
            else -> SummaryEndpointFlavor.OLLAMA
        }

    private fun parseTopicsFromModel(
        rawText: String,
        channelId: String,
        day: String,
        messages: List<SlackChannelMessage>,
        channelUrl: String
    ): List<SlackConversationSummaryItem> {
        val jsonBlock: String = extractJsonObject(text = rawText)
        val envelope: LlmTopicsEnvelope = try {
            json.decodeFromString(LlmTopicsEnvelope.serializer(), jsonBlock)
        } catch (e: Exception) {
            return emptyList()
        }
        val allowedTs: Set<String> = messages.map { it.ts }.toSet()
        return envelope.topics.mapNotNull { topic: LlmTopicPayload ->
            val anchor: String? = topic.anchor_ts?.takeIf { allowedTs.contains(it) }
                ?: messages.firstOrNull()?.ts
            if (anchor == null) {
                return@mapNotNull null
            }
            val threadTs: String? = topic.thread_ts?.takeIf { allowedTs.contains(it) || messages.any { m -> m.threadTs == it } }
            SlackConversationSummaryItem(
                analysisDay = day,
                channelId = channelId,
                topicLabel = topic.label.trim().ifEmpty { "Discussion" },
                category = normalizeCategory(raw = topic.category),
                summaryText = topic.summary.trim(),
                channelUrl = channelUrl,
                messageUrl = buildSlackMessageUrl(channelId = channelId, messageTs = anchor),
                threadUrl = threadTs?.let { ts: String -> buildSlackMessageUrl(channelId = channelId, messageTs = ts) },
                threadRootTimestamp = threadTs,
                sourceMessageTimestamp = anchor,
                degradedSingleTopicFallback = false
            )
        }
    }

    private fun normalizeCategory(raw: String): String {
        val normalized: String = raw.trim().lowercase()
        val allowed: Set<String> = setOf("planning", "discussion", "incident", "release", "decision", "general", "other")
        return if (normalized in allowed) normalized else "general"
    }

    private fun extractJsonObject(text: String): String {
        val trimmed: String = text.trim()
        val withoutFence: String = trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .trimEnd('`')
            .trim()
        val start: Int = withoutFence.indexOf('{')
        val end: Int = withoutFence.lastIndexOf('}')
        if (start >= 0 && end > start) {
            return withoutFence.substring(start, end + 1)
        }
        return withoutFence
    }

    private fun buildSlackMessageUrl(channelId: String, messageTs: String): String {
        val p: String = messageTs.replace(".", "")
        return "https://slack.com/archives/$channelId/p$p"
    }

    @Serializable
    private data class LlmTopicPayload(
        val label: String,
        val category: String,
        val summary: String,
        val anchor_ts: String? = null,
        val thread_ts: String? = null
    )

    @Serializable
    private data class LlmTopicsEnvelope(
        val topics: List<LlmTopicPayload> = emptyList()
    )

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
        val temperature: Double = 0.2
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

    private enum class SummaryEndpointFlavor {
        OLLAMA,
        OPENAI
    }
}
