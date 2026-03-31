package com.aitask.core.infrastructure.plugin

import com.aitask.core.domain.model.SlackChannelMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SlackConversationHistoryPage(
    val messages: List<SlackChannelMessage>,
    val hasMore: Boolean,
    val nextCursor: String?
) {
    val messageTimestamps: List<String> get() = messages.map { message: SlackChannelMessage -> message.ts }
}

sealed class SlackConversationHistoryResult {
    data class Ok(val page: SlackConversationHistoryPage) : SlackConversationHistoryResult()
    data class Error(val diagnostic: String) : SlackConversationHistoryResult()
}

interface SlackWebApiClient {
    fun authTest(token: String): Boolean
    fun conversationInfoOk(token: String, channelId: String): Boolean
    fun conversationHistory(
        token: String,
        channelId: String,
        oldestTs: String?,
        limit: Int,
        cursor: String?
    ): SlackConversationHistoryResult
}

private val slackHistoryJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
private data class SlackHistoryResponseDto(
    val ok: Boolean = false,
    val error: String? = null,
    val messages: List<SlackMessageDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("response_metadata") val responseMetadata: SlackResponseMetadataDto? = null
)

@Serializable
private data class SlackResponseMetadataDto(
    @SerialName("next_cursor") val nextCursor: String? = null
)

@Serializable
private data class SlackMessageDto(
    val ts: String? = null,
    val text: String? = null,
    @SerialName("thread_ts") val threadTs: String? = null,
    val user: String? = null,
    val subtype: String? = null
)

class DefaultSlackWebApiClient : SlackWebApiClient {
    override fun authTest(token: String): Boolean {
        return try {
            val connection = URI.create("https://slack.com/api/auth.test").toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 5000
            connection.readTimeout = 8000
            connection.connect()
            val responseCode = connection.responseCode
            val body = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader()
                .use { it.readText() }
            slackJsonResponseOk(body)
        } catch (e: Exception) {
            false
        }
    }

    override fun conversationInfoOk(token: String, channelId: String): Boolean {
        return try {
            val connection = URI.create("https://slack.com/api/conversations.info").toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 8000
            val body = "channel=${URLEncoder.encode(channelId, StandardCharsets.UTF_8)}"
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val responseCode = connection.responseCode
            val text = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader()
                .use { it.readText() }
            slackJsonResponseOk(text)
        } catch (e: Exception) {
            false
        }
    }

    override fun conversationHistory(
        token: String,
        channelId: String,
        oldestTs: String?,
        limit: Int,
        cursor: String?
    ): SlackConversationHistoryResult {
        return try {
            val connection = URI.create("https://slack.com/api/conversations.history").toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            connection.doOutput = true
            connection.connectTimeout = 8000
            connection.readTimeout = 20000
            val form = buildString {
                append("channel=").append(URLEncoder.encode(channelId, StandardCharsets.UTF_8))
                append("&limit=").append(limit.coerceIn(1, 200))
                if (cursor.isNullOrBlank()) {
                    if (!oldestTs.isNullOrBlank()) {
                        append("&oldest=").append(URLEncoder.encode(oldestTs, StandardCharsets.UTF_8))
                    }
                } else {
                    append("&cursor=").append(URLEncoder.encode(cursor, StandardCharsets.UTF_8))
                }
            }
            connection.outputStream.use { it.write(form.toByteArray(StandardCharsets.UTF_8)) }
            val responseCode = connection.responseCode
            val text = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader()
                .use { it.readText() }
            val dto = slackHistoryJson.decodeFromString(SlackHistoryResponseDto.serializer(), text)
            if (!dto.ok) {
                return SlackConversationHistoryResult.Error(diagnostic = dto.error ?: "Slack conversations.history failed")
            }
            val mapped: List<SlackChannelMessage> = dto.messages.mapNotNull { message: SlackMessageDto ->
                val ts: String = message.ts?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val body: String = message.text?.trim().orEmpty().ifEmpty {
                    message.subtype?.let { subtype: String -> "(system: $subtype)" } ?: "(no text)"
                }
                SlackChannelMessage(ts = ts, text = body, threadTs = message.threadTs, userId = message.user)
            }
            val next: String? = dto.responseMetadata?.nextCursor?.takeIf { it.isNotBlank() }
            SlackConversationHistoryResult.Ok(
                page = SlackConversationHistoryPage(
                    messages = mapped,
                    hasMore = dto.hasMore,
                    nextCursor = next
                )
            )
        } catch (e: Exception) {
            SlackConversationHistoryResult.Error(diagnostic = e.message ?: "Slack conversations.history request failed")
        }
    }
}

internal fun slackJsonResponseOk(body: String): Boolean =
    body.contains("\"ok\":true") || body.contains("\"ok\": true")
