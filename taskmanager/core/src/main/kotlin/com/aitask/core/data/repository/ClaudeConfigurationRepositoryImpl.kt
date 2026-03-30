package com.aitask.core.data.repository

import com.aitask.core.data.entity.ClaudeConfigurations
import com.aitask.core.domain.model.ClaudeConfiguration
import com.aitask.core.domain.model.ClaudeConfigurationRequest
import com.aitask.core.domain.model.ClaudeIntegrationMode
import com.aitask.core.domain.repository.ClaudeConfigurationRepository
import com.aitask.core.domain.service.EncryptionService
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ClaudeConfigurationRepositoryImpl(
    private val encryptionService: EncryptionService
) : ClaudeConfigurationRepository {
    override suspend fun find(): ClaudeConfiguration? = newSuspendedTransaction {
        ClaudeConfigurations.selectAll()
            .map { it.toDomain() }
            .singleOrNull()
    }

    override suspend fun findApiKey(id: UUID): String? = newSuspendedTransaction {
        val row = ClaudeConfigurations.selectAll()
            .where { ClaudeConfigurations.id eq id }
            .singleOrNull() ?: return@newSuspendedTransaction null
        val encrypted = row[ClaudeConfigurations.apiKeyEncrypted] ?: return@newSuspendedTransaction null
        encryptionService.decrypt(encrypted).getOrNull()
    }

    override suspend fun save(request: ClaudeConfigurationRequest): ClaudeConfiguration = newSuspendedTransaction {
        val existing = ClaudeConfigurations.selectAll().singleOrNull()
        val now = Instant.now()
        val id = request.id ?: existing?.get(ClaudeConfigurations.id)?.value ?: UUID.randomUUID()
        val apiKey = request.apiKey?.trim()?.takeIf { it.isNotBlank() }
        val encryptedApiKey = when {
            apiKey != null -> encryptionService.encrypt(apiKey).getOrThrow()
            existing != null -> existing[ClaudeConfigurations.apiKeyEncrypted]
            else -> null
        }
        val modeString = request.integrationMode.name
        val trimmedBase = request.apiBaseUrl?.trim()?.takeIf { it.isNotBlank() }
        if (existing == null) {
            ClaudeConfigurations.insert {
                it[ClaudeConfigurations.id] = id
                it[isEnabled] = request.isEnabled
                it[integrationMode] = modeString
                it[cliPath] = request.cliPath.trim()
                it[apiBaseUrl] = trimmedBase
                it[ClaudeConfigurations.apiKeyEncrypted] = encryptedApiKey
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            ClaudeConfigurations.update({ ClaudeConfigurations.id eq id }) {
                it[isEnabled] = request.isEnabled
                it[integrationMode] = modeString
                it[cliPath] = request.cliPath.trim()
                it[apiBaseUrl] = trimmedBase
                it[ClaudeConfigurations.apiKeyEncrypted] = encryptedApiKey
                it[updatedAt] = now
            }
        }
        ClaudeConfigurations.selectAll()
            .where { ClaudeConfigurations.id eq id }
            .single()
            .toDomain()
    }

    private fun ResultRow.toDomain(): ClaudeConfiguration = ClaudeConfiguration(
        id = this[ClaudeConfigurations.id].value,
        isEnabled = this[ClaudeConfigurations.isEnabled],
        integrationMode = ClaudeIntegrationMode.valueOf(this[ClaudeConfigurations.integrationMode]),
        cliPath = this[ClaudeConfigurations.cliPath],
        apiBaseUrl = this[ClaudeConfigurations.apiBaseUrl],
        hasApiKey = this[ClaudeConfigurations.apiKeyEncrypted] != null,
        createdAt = this[ClaudeConfigurations.createdAt],
        updatedAt = this[ClaudeConfigurations.updatedAt]
    )
}
