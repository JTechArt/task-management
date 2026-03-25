package com.aitask.core.infrastructure.llm

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress

class DefaultLlmConnectionValidatorTest {
    @Test
    fun `validate succeeds against a local ollama compatible endpoint`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        try {
            server.createContext("/") { exchange -> respond(exchange, 404, "not found") }
            server.createContext("/api/tags") { exchange -> respond(exchange, 200, """{"models":[]}""") }
            server.start()

            val port = server.address.port
            val validator = DefaultLlmConnectionValidator(connectTimeoutMillis = 1_000, readTimeoutMillis = 1_000)
            val result = validator.validate("http://localhost:$port", null)

            assertTrue(result.isSuccess)
            assertEquals(200, result.getOrThrow().statusCode)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `validate forwards bearer auth header when api key is provided`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        try {
            server.createContext("/v1/models") { exchange ->
                val header = exchange.requestHeaders.getFirst("Authorization")
                if (header == "Bearer local-key") {
                    respond(exchange, 200, """{"data":[]}""")
                } else {
                    respond(exchange, 401, "missing auth")
                }
            }
            server.start()

            val port = server.address.port
            val validator = DefaultLlmConnectionValidator(connectTimeoutMillis = 1_000, readTimeoutMillis = 1_000)
            val result = validator.validate("http://localhost:$port", "local-key")

            assertTrue(result.isSuccess)
            assertEquals(200, result.getOrThrow().statusCode)
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
