package com.aitask.desktop.oauth

import com.aitask.core.domain.model.OAuthConnection
import com.aitask.core.domain.model.OAuthProvider
import com.aitask.core.domain.service.OAuthService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.net.URI
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer

private val logger = KotlinLogging.logger {}

private const val SUCCESS_HTML = """
<!DOCTYPE html><html><head><meta charset="utf-8"><title>Connected</title></head>
<body><h2>OAuth connected successfully</h2><p>You can close this window and return to AiTask.</p></body></html>
"""

private const val ERROR_HTML = """
<!DOCTYPE html><html><head><meta charset="utf-8"><title>Error</title></head>
<body><h2>Connection failed</h2><p>%s</p><p>You can close this window and try again in AiTask.</p></body></html>
"""

/**
 * Temporary HTTP server to receive OAuth redirect callback.
 * Listens on redirect URI port; on GET request, extracts code/state, exchanges for tokens.
 */
class OAuthCallbackServer(
    private val port: Int,
    private val oauthService: OAuthService,
    private val onComplete: (Result<OAuthConnection>) -> Unit
) {
    private var server: HttpServer? = null

    fun start(provider: OAuthProvider, expectedState: String) {
        if (server != null) return
        server = HttpServer.create(InetSocketAddress(port), 0).apply {
            createContext("/oauth/callback") { exchange ->
                handleCallback(exchange, provider, expectedState)
            }
            executor = null
            start()
        }
        logger.info { "OAuth callback server started on port $port" }
    }

    fun stop() {
        server?.stop(0)
        server = null
        logger.info { "OAuth callback server stopped" }
    }

    private fun handleCallback(
        exchange: HttpExchange,
        provider: OAuthProvider,
        expectedState: String
    ) {
        if (exchange.requestMethod != "GET") {
            respond(exchange, 405, "Method not allowed")
            return
        }
        val uri = URI(exchange.requestURI.toString())
        val query = uri.query ?: ""
        val params = query.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            parts[0] to (parts.getOrNull(1) ?: "")
        }
        val code = params["code"]
        val state = params["state"]
        val error = params["error"]
        when {
            error != null -> {
                logger.warn { "OAuth error from provider: $error" }
                respondHtml(exchange, 400, ERROR_HTML.format(error))
                onComplete(Result.failure(Exception("OAuth error: $error")))
            }
            code == null || state == null -> {
                respondHtml(exchange, 400, ERROR_HTML.format("Missing code or state"))
                onComplete(Result.failure(IllegalArgumentException("Missing code or state")))
            }
            state != expectedState -> {
                respondHtml(exchange, 400, ERROR_HTML.format("Invalid state"))
                onComplete(Result.failure(IllegalArgumentException("Invalid state")))
            }
            else -> {
                val result = runBlocking {
                    oauthService.handleCallback(provider, code, state)
                }
                result.fold(
                    onSuccess = {
                        respondHtml(exchange, 200, SUCCESS_HTML)
                        onComplete(Result.success(it))
                    },
                    onFailure = { e ->
                        logger.warn(e) { "OAuth token exchange failed" }
                        respondHtml(exchange, 500, ERROR_HTML.format(e.message ?: "Token exchange failed"))
                        onComplete(Result.failure(e))
                    }
                )
            }
        }
        stop()
    }

    private fun respond(exchange: HttpExchange, code: Int, body: String) {
        exchange.sendResponseHeaders(code, body.toByteArray().size.toLong())
        exchange.responseBody.use { it.write(body.toByteArray()) }
    }

    private fun respondHtml(exchange: HttpExchange, code: Int, html: String) {
        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        val bytes = html.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
