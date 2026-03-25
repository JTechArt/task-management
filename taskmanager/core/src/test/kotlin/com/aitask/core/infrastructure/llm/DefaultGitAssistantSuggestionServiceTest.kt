package com.aitask.core.infrastructure.llm

import com.aitask.core.domain.model.GitAssistantSuggestionMode
import com.aitask.core.domain.model.GitAssistantSuggestionRequest
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

class DefaultGitAssistantSuggestionServiceTest {
    @Test
    fun `generate sends ollama style request and returns generated text`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        try {
            lateinit var capturedBody: String
            var capturedAuth: String? = null
            server.createContext("/api/generate") { exchange ->
                capturedBody = exchange.requestBody.bufferedReader().readText()
                capturedAuth = exchange.requestHeaders.getFirst("Authorization")
                respond(exchange, 200, """{"response":"feat: add git assistant"}""")
            }
            server.start()

            val service = DefaultGitAssistantSuggestionService()
            val result = service.generate(
                request = GitAssistantSuggestionRequest(
                    projectId = UUID.randomUUID(),
                    projectName = "Atlas",
                    taskId = UUID.randomUUID(),
                    taskTitle = "Add git assistant",
                    taskDescription = "Draft git metadata with local LLM",
                    branchName = "feature/git-assist",
                    repositoryNames = listOf("desktop-app"),
                    stagedChangesSummary = "Staged files:\n- src/main/kotlin/com/example/GitAssistant.kt",
                    mode = GitAssistantSuggestionMode.COMMIT_MESSAGE
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
            assertEquals("feat: add git assistant", result.getOrThrow().generatedText)
            assertTrue(capturedBody.contains("\"model\":\"llama3.1\""))
            assertTrue(capturedBody.contains("Add git assistant"))
            assertTrue(capturedBody.contains("Staged git context"))
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
