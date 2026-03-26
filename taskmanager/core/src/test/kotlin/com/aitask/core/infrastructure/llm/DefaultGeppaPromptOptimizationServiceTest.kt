package com.aitask.core.infrastructure.llm

import com.aitask.core.domain.model.GeppaConfiguration
import com.aitask.core.domain.repository.GeppaConfigurationRepository
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID

class DefaultGeppaPromptOptimizationServiceTest {

    private fun makeConfig(port: Int, enabled: Boolean = true) = GeppaConfiguration(
        id = UUID.randomUUID(),
        isEnabled = enabled,
        endpointUrl = "http://localhost:$port",
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `returns original prompt when GEPPA is disabled`() = runTest {
        val repo = mockk<GeppaConfigurationRepository>()
        coEvery { repo.find() } returns makeConfig(9999, enabled = false)

        val service = DefaultGeppaPromptOptimizationService(repo)
        val result = service.optimizeIfEnabled("original prompt", "task_generation")

        assertEquals("original prompt", result)
    }

    @Test
    fun `returns original prompt when GEPPA configuration is null`() = runTest {
        val repo = mockk<GeppaConfigurationRepository>()
        coEvery { repo.find() } returns null

        val service = DefaultGeppaPromptOptimizationService(repo)
        val result = service.optimizeIfEnabled("original prompt", "task_generation")

        assertEquals("original prompt", result)
    }

    @Test
    fun `returns optimized prompt when GEPPA returns success`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        try {
            var capturedBody: String? = null
            server.createContext("/optimize") { exchange ->
                capturedBody = exchange.requestBody.bufferedReader().readText()
                respond(exchange, 200, """{"optimized_prompt":"optimized content"}""")
            }
            server.start()

            val repo = mockk<GeppaConfigurationRepository>()
            coEvery { repo.find() } returns makeConfig(server.address.port)

            val service = DefaultGeppaPromptOptimizationService(repo)
            val result = service.optimizeIfEnabled("original prompt", "task_generation")

            assertEquals("optimized content", result)
            assertEquals(true, capturedBody?.contains("original prompt"))
            assertEquals(true, capturedBody?.contains("task_generation"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `falls back to original prompt when GEPPA returns non-200`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        try {
            server.createContext("/optimize") { exchange ->
                respond(exchange, 503, """{"error":"service unavailable"}""")
            }
            server.start()

            val repo = mockk<GeppaConfigurationRepository>()
            coEvery { repo.find() } returns makeConfig(server.address.port)

            val service = DefaultGeppaPromptOptimizationService(repo)
            val result = service.optimizeIfEnabled("original prompt", "git_suggestion")

            assertEquals("original prompt", result)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `falls back to original prompt when GEPPA endpoint is unreachable`() = runTest {
        val repo = mockk<GeppaConfigurationRepository>()
        coEvery { repo.find() } returns makeConfig(19999) // nothing listening

        val service = DefaultGeppaPromptOptimizationService(repo)
        val result = service.optimizeIfEnabled("original prompt", "agent_execution")

        assertEquals("original prompt", result)
    }

    @Test
    fun `falls back to original prompt when GEPPA returns blank optimized_prompt`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        try {
            server.createContext("/optimize") { exchange ->
                respond(exchange, 200, """{"optimized_prompt":""}""")
            }
            server.start()

            val repo = mockk<GeppaConfigurationRepository>()
            coEvery { repo.find() } returns makeConfig(server.address.port)

            val service = DefaultGeppaPromptOptimizationService(repo)
            val result = service.optimizeIfEnabled("original prompt", "task_generation")

            assertEquals("original prompt", result)
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
