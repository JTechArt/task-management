package com.aitask.core.data.repository

import com.aitask.core.data.entity.LlmConfigurations
import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.LlmConfigurationRequest
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.service.EncryptionService
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class LlmConfigurationRepositoryImpl(
    private val encryptionService: EncryptionService
) : LlmConfigurationRepository {
    override suspend fun findAll(): List<LlmConfiguration> = newSuspendedTransaction {
        LlmConfigurations.selectAll()
            .orderBy(LlmConfigurations.isDefault to SortOrder.DESC, LlmConfigurations.updatedAt to SortOrder.DESC)
            .map { it.toDomain() }
    }

    override suspend fun findById(id: UUID): LlmConfiguration? = newSuspendedTransaction {
        LlmConfigurations.selectAll()
            .where { LlmConfigurations.id eq id }
            .map { it.toDomain() }
            .singleOrNull()
    }

    override suspend fun findDefault(): LlmConfiguration? = newSuspendedTransaction {
        LlmConfigurations.selectAll()
            .where { LlmConfigurations.isDefault eq true }
            .map { it.toDomain() }
            .singleOrNull()
    }

    override suspend fun findApiKey(id: UUID): String? = newSuspendedTransaction {
        val row = findRowById(id) ?: return@newSuspendedTransaction null
        val encrypted = row[LlmConfigurations.apiKeyEncrypted] ?: return@newSuspendedTransaction null
        encryptionService.decrypt(encrypted).getOrNull()
    }

    override suspend fun save(request: LlmConfigurationRequest): LlmConfiguration = newSuspendedTransaction {
        val existing = request.id?.let { findRowById(it) }
        val now = Instant.now()
        val id = request.id ?: UUID.randomUUID()
        val apiKey = request.apiKey?.trim()?.takeIf { it.isNotBlank() }
        val encryptedApiKey = when {
            apiKey != null -> encryptionService.encrypt(apiKey).getOrThrow()
            existing != null -> existing[LlmConfigurations.apiKeyEncrypted]
            else -> null
        }
        val hasDefault = LlmConfigurations.selectAll().where { LlmConfigurations.isDefault eq true }.any()
        val shouldBeDefault = request.isDefault || !hasDefault || existing?.let { it[LlmConfigurations.isDefault] } == true
        if (shouldBeDefault) {
            LlmConfigurations.update({ LlmConfigurations.id neq id }) {
                it[isDefault] = false
            }
        }
        if (existing == null) {
            LlmConfigurations.insert {
                it[LlmConfigurations.id] = id
                it[name] = request.name.trim()
                it[endpointUrl] = request.endpointUrl.trim()
                it[modelIdentifier] = request.modelIdentifier.trim()
                it[apiKeyEncrypted] = encryptedApiKey
                it[isDefault] = shouldBeDefault
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            LlmConfigurations.update({ LlmConfigurations.id eq id }) {
                it[name] = request.name.trim()
                it[endpointUrl] = request.endpointUrl.trim()
                it[modelIdentifier] = request.modelIdentifier.trim()
                it[apiKeyEncrypted] = encryptedApiKey
                it[isDefault] = shouldBeDefault
                it[updatedAt] = now
            }
        }
        findRowById(id)?.toDomain() ?: error("Saved LLM configuration could not be reloaded")
    }

    override suspend fun delete(id: UUID): Boolean = newSuspendedTransaction {
        val deleted = LlmConfigurations.deleteWhere { LlmConfigurations.id eq id }
        if (deleted > 0 && LlmConfigurations.selectAll().none()) {
            return@newSuspendedTransaction true
        }
        if (deleted > 0) {
            val hasDefault = LlmConfigurations.selectAll().where { LlmConfigurations.isDefault eq true }.any()
            if (!hasDefault) {
                val firstId = LlmConfigurations.selectAll()
                    .orderBy(LlmConfigurations.updatedAt to SortOrder.DESC)
                    .map { it[LlmConfigurations.id].value }
                    .firstOrNull()
                if (firstId != null) {
                    LlmConfigurations.update({ LlmConfigurations.id eq firstId }) {
                        it[isDefault] = true
                    }
                }
            }
        }
        deleted > 0
    }

    override suspend fun setDefault(id: UUID): LlmConfiguration? = newSuspendedTransaction {
        findRowById(id) ?: return@newSuspendedTransaction null
        LlmConfigurations.update({ LlmConfigurations.id neq id }) {
            it[isDefault] = false
        }
        LlmConfigurations.update({ LlmConfigurations.id eq id }) {
            it[isDefault] = true
            it[updatedAt] = Instant.now()
        }
        findRowById(id)?.toDomain()
    }

    private fun ResultRow.toDomain(): LlmConfiguration = LlmConfiguration(
        id = this[LlmConfigurations.id].value,
        name = this[LlmConfigurations.name],
        endpointUrl = this[LlmConfigurations.endpointUrl],
        modelIdentifier = this[LlmConfigurations.modelIdentifier],
        hasApiKey = this[LlmConfigurations.apiKeyEncrypted] != null,
        isDefault = this[LlmConfigurations.isDefault],
        createdAt = this[LlmConfigurations.createdAt],
        updatedAt = this[LlmConfigurations.updatedAt]
    )

    private fun findRowById(id: UUID): ResultRow? =
        LlmConfigurations.selectAll()
            .where { LlmConfigurations.id eq id }
            .singleOrNull()
}
