package com.aitask.core.infrastructure.llm

import com.aitask.core.domain.model.GeppaConnectionTestResult
import com.aitask.core.domain.service.GeppaConnectionValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.math.max

class DefaultGeppaConnectionValidator(
    private val connectTimeoutMillis: Int = 3_000,
    private val readTimeoutMillis: Int = 3_000
) : GeppaConnectionValidator {

    override suspend fun validate(endpointUrl: String): Result<GeppaConnectionTestResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = normalizeEndpoint(endpointUrl)
                val candidates = buildCandidates(normalized)
                var lastFailureMessage: String? = null

                for (candidate in candidates) {
                    val result = probe(candidate)
                    if (result.success != null) {
                        return@runCatching result.success
                    }
                    lastFailureMessage = result.failureMessage
                }

                throw IllegalStateException(
                    "Unable to reach the GEPPA endpoint. Verify the URL and ensure the service is running." +
                        (lastFailureMessage?.let { " Last error: $it" } ?: "")
                )
            }
        }

    private data class ProbeOutcome(
        val success: GeppaConnectionTestResult? = null,
        val failureMessage: String? = null
    )

    private fun buildCandidates(baseUrl: URL): List<URL> {
        val uri = URI(baseUrl.toString())
        val explicitPath = uri.path?.takeIf { it.isNotBlank() && it != "/" }
        return if (explicitPath != null) {
            listOf(baseUrl)
        } else {
            listOf(
                baseUrl,
                URL(baseUrl, "health"),
                URL(baseUrl, "api/health"),
                URL(baseUrl, "v1/health")
            ).distinctBy { it.toString() }
        }
    }

    private fun probe(candidate: URL): ProbeOutcome {
        val start = System.nanoTime()
        val connection = (candidate.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val statusCode = connection.responseCode
            if (statusCode in 200..299) {
                ProbeOutcome(
                    success = GeppaConnectionTestResult(
                        probeUrl = candidate.toString(),
                        statusCode = statusCode,
                        latencyMillis = max(1L, (System.nanoTime() - start) / 1_000_000)
                    )
                )
            } else {
                ProbeOutcome(failureMessage = "HTTP $statusCode from $candidate")
            }
        } catch (e: Exception) {
            ProbeOutcome(failureMessage = e.message ?: "Connection failed for $candidate")
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeEndpoint(endpointUrl: String): URL {
        val trimmed = endpointUrl.trim()
        require(trimmed.isNotBlank()) { "GEPPA endpoint URL must not be blank" }
        val uri = URI(trimmed)
        require(uri.scheme == "http" || uri.scheme == "https") { "GEPPA endpoint URL must use http or https" }
        return uri.toURL()
    }
}
