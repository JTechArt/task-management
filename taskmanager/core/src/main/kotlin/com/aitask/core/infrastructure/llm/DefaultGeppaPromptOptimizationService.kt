package com.aitask.core.infrastructure.llm

import com.aitask.core.domain.repository.GeppaConfigurationRepository
import com.aitask.core.domain.service.GeppaPromptOptimizationService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

class DefaultGeppaPromptOptimizationService(
    private val geppaConfigurationRepository: GeppaConfigurationRepository,
    private val httpClient: HttpClient = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 3_000
            requestTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
) : GeppaPromptOptimizationService {

    override suspend fun optimizeIfEnabled(prompt: String, context: String): String =
        withContext(Dispatchers.IO) {
            val config = geppaConfigurationRepository.find()
            if (config == null || !config.isEnabled) {
                return@withContext prompt
            }

            runCatching {
                val endpointUrl = config.endpointUrl.trimEnd('/') + "/optimize"
                val response = httpClient.post(endpointUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(GeppaOptimizeRequest(prompt = prompt, context = context))
                }

                if (response.status.value in 200..299) {
                    val body = response.bodyAsText()
                    val parsed = json.decodeFromString(GeppaOptimizeResponse.serializer(), body)
                    val optimized = parsed.optimizedPrompt.takeIf { it.isNotBlank() } ?: prompt
                    logger.info {
                        "GEPPA optimization applied: context=$context " +
                            "originalLength=${prompt.length} optimizedLength=${optimized.length}"
                    }
                    optimized
                } else {
                    logger.warn {
                        "GEPPA optimization returned HTTP ${response.status.value} for context=$context, using original prompt"
                    }
                    prompt
                }
            }.getOrElse { error ->
                logger.warn {
                    "GEPPA optimization failed for context=$context (${error.javaClass.simpleName}), using original prompt"
                }
                prompt
            }
        }

    private val json = Json { ignoreUnknownKeys = true }
}

@Serializable
private data class GeppaOptimizeRequest(
    val prompt: String,
    val context: String
)

@Serializable
private data class GeppaOptimizeResponse(
    @SerialName("optimized_prompt") val optimizedPrompt: String = ""
)
