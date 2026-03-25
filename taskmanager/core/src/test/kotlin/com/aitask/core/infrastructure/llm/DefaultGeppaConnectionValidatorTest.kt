package com.aitask.core.infrastructure.llm

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress

class DefaultGeppaConnectionValidatorTest {
    @Test
    fun `validate succeeds against a health endpoint returning 200`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        try {
            server.createContext("/health") { exchange -> respond(exchange, 200, """{"status":"ok"}""") }
            server.start()

            val port = server.address.port
            val validator = DefaultGeppaConnectionValidator(connectTimeoutMillis = 1_000, readTimeoutMillis = 1_000)
            val result = validator.validate("http://localhost:$port")

            assertTrue(result.isSuccess)
            assertEquals(200, result.getOrThrow().statusCode)
            assertTrue(result.getOrThrow().probeUrl.contains("/health"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `validate uses explicit path when provided`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        try {
            server.createContext("/custom/status") { exchange -> respond(exchange, 200, "ok") }
            server.start()

            val port = server.address.port
            val validator = DefaultGeppaConnectionValidator(connectTimeoutMillis = 1_000, readTimeoutMillis = 1_000)
            val result = validator.validate("http://localhost:$port/custom/status")

            assertTrue(result.isSuccess)
            assertEquals("http://localhost:$port/custom/status", result.getOrThrow().probeUrl)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `validate fails when endpoint is unreachable`() = runTest {
        val validator = DefaultGeppaConnectionValidator(connectTimeoutMillis = 200, readTimeoutMillis = 200)
        val result = validator.validate("http://localhost:19999")

        assertTrue(result.isFailure)
    }

    @Test
    fun `validate rejects blank endpoint url`() = runTest {
        val validator = DefaultGeppaConnectionValidator()
        val result = validator.validate("   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("blank") == true)
    }

    @Test
    fun `validate rejects non-http scheme`() = runTest {
        val validator = DefaultGeppaConnectionValidator()
        val result = validator.validate("ftp://localhost:8080")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("http") == true)
    }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
