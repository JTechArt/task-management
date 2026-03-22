package com.aitask.core.infrastructure.slack

import com.aitask.core.domain.service.SlackBlock
import com.aitask.core.domain.service.SlackService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Ktor-based Slack Incoming Webhook client.
 * Sends messages without blocking; failures return Result.failure.
 */
class SlackWebhookClient(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
            })
        }
        expectSuccess = false
    }
) : SlackService {

    override suspend fun sendToWebhook(
        webhookUrl: String,
        text: String,
        blocks: List<SlackBlock>?
    ): Result<Unit> {
        return try {
            val payload = SlackWebhookPayload(
                text = text,
                blocks = blocks?.map { it.toSlackPayloadBlock() }
            )
            val response = httpClient.post(webhookUrl) {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            when (response.status) {
                HttpStatusCode.OK -> Result.success(Unit)
                else -> {
                    val body = response.bodyAsText()
                    logger.warn { "Slack webhook failed: status=${response.status}, body=$body" }
                    Result.failure(SlackDeliveryException("Slack returned ${response.status}: $body"))
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Slack webhook request failed: ${e.message}" }
            Result.failure(SlackDeliveryException(e.message ?: "Unknown error", e))
        }
    }

    fun close() {
        httpClient.close()
    }
}

@Serializable
private data class SlackWebhookPayload(
    val text: String,
    val blocks: List<SlackPayloadBlock>? = null
)

@Serializable
private data class SlackPayloadBlock(
    val type: String,
    val text: SlackPayloadText? = null,
    val elements: List<SlackPayloadElement>? = null
)

@Serializable
private data class SlackPayloadText(
    val type: String = "mrkdwn",
    val text: String
)

@Serializable
private data class SlackPayloadElement(
    val type: String,
    val text: String
)

private fun SlackBlock.toSlackPayloadBlock(): SlackPayloadBlock =
    SlackPayloadBlock(
        type = type,
        text = text?.let { SlackPayloadText(text = it) },
        elements = elements?.map { SlackPayloadElement(type = it.type, text = it.text) }
    )

/**
 * Thrown when Slack delivery fails; used for non-blocking feedback (AC4).
 */
class SlackDeliveryException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
