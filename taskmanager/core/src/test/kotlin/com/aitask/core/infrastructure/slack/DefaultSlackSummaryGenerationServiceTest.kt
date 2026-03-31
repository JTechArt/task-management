package com.aitask.core.infrastructure.slack

import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.SlackChannelMessage
import com.aitask.core.domain.model.SlackConversationSummaryItem
import com.aitask.core.domain.service.SlackChannelSummaryBatch
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID

class DefaultSlackSummaryGenerationServiceTest {
    @Test
    fun `summarizeChannelDays parses openai style json topics`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        try {
            server.createContext("/v1/chat/completions") { exchange: HttpExchange ->
                val content =
                    """{"topics":[{"label":"Deploy","category":"release","summary":"Shipped v2.","anchor_ts":"10.0","thread_ts":null}]}"""
                val escaped: String = content.replace("\\", "\\\\").replace("\"", "\\\"")
                respond(
                    exchange = exchange,
                    status = 200,
                    payload = """{"choices":[{"message":{"content":"$escaped"}}]}"""
                )
            }
            server.start()
            val service = DefaultSlackSummaryGenerationService()
            val configuration = LlmConfiguration(
                id = UUID.randomUUID(),
                name = "Test",
                endpointUrl = "http://localhost:${server.address.port}/v1/chat/completions",
                modelIdentifier = "test",
                hasApiKey = false,
                isDefault = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            val batch: SlackChannelSummaryBatch = service.summarizeChannelDays(
                channelId = "C1",
                messagesByDay = mapOf(
                    "1970-01-01" to listOf(SlackChannelMessage(ts = "10.0", text = "we shipped"))
                ),
                configuration = configuration,
                apiKey = null
            )
            val item: SlackConversationSummaryItem = batch.items.single()
            assertEquals("Deploy", item.topicLabel)
            assertEquals("release", item.category)
            assertEquals("https://slack.com/archives/C1/p100", item.messageUrl)
            assertTrue(batch.diagnostics.isEmpty())
        } finally {
            server.stop(0)
        }
    }

    private fun respond(exchange: HttpExchange, status: Int, payload: String) {
        val bytes = payload.toByteArray()
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
