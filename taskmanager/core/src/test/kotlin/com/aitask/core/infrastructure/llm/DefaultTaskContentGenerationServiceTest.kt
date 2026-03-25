package com.aitask.core.infrastructure.llm

import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.TaskContentGenerationMode
import com.aitask.core.domain.model.TaskContentGenerationRequest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.time.Instant
import java.util.UUID

class DefaultTaskContentGenerationServiceTest {
    @Test
    fun `generate sends ollama style request and returns generated text`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        try {
            lateinit var capturedBody: String
            var capturedAuth: String? = null
            server.createContext("/api/generate") { exchange ->
                capturedBody = exchange.requestBody.bufferedReader().readText()
                capturedAuth = exchange.requestHeaders.getFirst("Authorization")
                respond(exchange, 200, """{"response":"Write a crisp task description."}""")
            }
            server.start()

            val service = DefaultTaskContentGenerationService()
            val result = service.generate(
                request = TaskContentGenerationRequest(
                    projectId = UUID.randomUUID(),
                    projectName = "Atlas",
                    title = "Add local LLM generation",
                    currentDescription = "Current draft",
                    taskType = com.aitask.core.domain.model.TaskType.FEATURE,
                    mode = TaskContentGenerationMode.DESCRIPTION
                ),
                configuration = LlmConfiguration(
                    id = UUID.randomUUID(),
                    name = "Local Ollama",
                    endpointUrl = "http://localhost:${server.address.port}",
                    modelIdentifier = "llama3.1",
                    hasApiKey = true,
                    isDefault = true,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                ),
                apiKey = "local-token"
            )

            assertTrue(result.isSuccess)
            assertEquals("Write a crisp task description.", result.getOrThrow().generatedText)
            assertTrue(capturedBody.contains("\"model\":\"llama3.1\""))
            assertTrue(capturedBody.contains("Add local LLM generation"))
            assertEquals("Bearer local-token", capturedAuth)
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
