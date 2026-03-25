package com.aitask.core.data.repository

import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.LlmConfigurationRequest
import com.aitask.core.domain.repository.LlmConfigurationRepository
import java.time.Instant
import java.util.UUID

class InMemoryLlmConfigurationRepository : LlmConfigurationRepository {
    private data class StoredConfiguration(
        val id: UUID,
        val name: String,
        val endpointUrl: String,
        val modelIdentifier: String,
        val apiKey: String?,
        val isDefault: Boolean,
        val createdAt: Instant,
        val updatedAt: Instant
    ) {
        fun toDomain() = LlmConfiguration(
            id = id,
            name = name,
            endpointUrl = endpointUrl,
            modelIdentifier = modelIdentifier,
            hasApiKey = apiKey != null,
            isDefault = isDefault,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private val configurations = linkedMapOf<UUID, StoredConfiguration>()

    override suspend fun findAll(): List<LlmConfiguration> =
        configurations.values
            .sortedWith(compareByDescending<StoredConfiguration> { it.isDefault }.thenByDescending { it.updatedAt })
            .map { it.toDomain() }

    override suspend fun findById(id: UUID): LlmConfiguration? =
        configurations[id]?.toDomain()

    override suspend fun findDefault(): LlmConfiguration? =
        configurations.values.firstOrNull { it.isDefault }?.toDomain()

    override suspend fun findApiKey(id: UUID): String? =
        configurations[id]?.apiKey

    override suspend fun save(request: LlmConfigurationRequest): LlmConfiguration {
        val now = Instant.now()
        val existing = request.id?.let { configurations[it] }
        val id = request.id ?: UUID.randomUUID()
        val apiKey = request.apiKey?.trim()?.takeIf { it.isNotBlank() } ?: existing?.apiKey
        val shouldBeDefault = request.isDefault || findDefault() == null || existing?.isDefault == true

        val snapshot = StoredConfiguration(
            id = id,
            name = request.name.trim(),
            endpointUrl = request.endpointUrl.trim(),
            modelIdentifier = request.modelIdentifier.trim(),
            apiKey = apiKey,
            isDefault = shouldBeDefault,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        if (shouldBeDefault) {
            configurations.replaceAll { key, current ->
                if (key == id) current.copy(isDefault = false) else current.copy(isDefault = false)
            }
        }
        configurations[id] = snapshot.copy(isDefault = shouldBeDefault)
        if (configurations.values.none { it.isDefault }) {
            configurations[id] = configurations[id]!!.copy(isDefault = true)
        }
        return configurations[id]!!.toDomain()
    }

    override suspend fun delete(id: UUID): Boolean {
        val removed = configurations.remove(id) != null
        if (removed && configurations.values.none { it.isDefault } && configurations.isNotEmpty()) {
            val firstId = configurations.entries.maxByOrNull { it.value.updatedAt }?.key ?: configurations.keys.first()
            configurations[firstId] = configurations[firstId]!!.copy(isDefault = true)
        }
        return removed
    }

    override suspend fun setDefault(id: UUID): LlmConfiguration? {
        val current = configurations[id] ?: return null
        configurations.replaceAll { key, value ->
            value.copy(isDefault = key == id)
        }
        return current.copy(isDefault = true).toDomain()
    }
}
