package com.aitask.core.infrastructure.llm

import com.aitask.core.domain.model.AgentExecutionResult
import com.aitask.core.domain.model.LlmConfiguration
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.time.Instant
import java.util.UUID

class DefaultAgentExecutionServiceTest {
    @Test
    fun `execute sends openai style request and returns generated text`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        try {
            lateinit var capturedBody: String
            server.createContext("/v1/chat/completions") { exchange ->
                capturedBody = exchange.requestBody.bufferedReader().readText()
                respond(exchange, 200, """{"choices":[{"message":{"content":"agent output"}}]}""")
            }
            server.start()

            val service = DefaultAgentExecutionService()
            val result = service.execute(
                prompt = "Summarize task progress",
                configuration = LlmConfiguration(
                    id = UUID.randomUUID(),
                    name = "Remote Agent",
                    endpointUrl = "http://localhost:${server.address.port}/v1/chat/completions",
                    modelIdentifier = "gpt-4o-mini",
                    hasApiKey = false,
                    isDefault = true,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                ),
                apiKey = null
            )

            assertTrue(result.isSuccess)
            assertEquals("agent output", result.getOrThrow().generatedText)
            assertTrue(capturedBody.contains("Summarize task progress"))
        } finally {
            server.stop(0)
        }
    }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}

