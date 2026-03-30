package com.aitask.core.data.repository

import com.aitask.core.data.entity.CodexConfigurations
import com.aitask.core.domain.model.CodexConfiguration
import com.aitask.core.domain.model.CodexConfigurationRequest
import com.aitask.core.domain.repository.CodexConfigurationRepository
import com.aitask.core.domain.service.EncryptionService
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class CodexConfigurationRepositoryImpl(
    private val encryptionService: EncryptionService
) : CodexConfigurationRepository {
    override suspend fun find(): CodexConfiguration? = newSuspendedTransaction {
        CodexConfigurations.selectAll()
            .map { it.toDomain() }
            .singleOrNull()
    }

    override suspend fun findApiKey(id: UUID): String? = newSuspendedTransaction {
        val row = CodexConfigurations.selectAll()
            .where { CodexConfigurations.id eq id }
            .singleOrNull() ?: return@newSuspendedTransaction null
        val encrypted = row[CodexConfigurations.apiKeyEncrypted] ?: return@newSuspendedTransaction null
        encryptionService.decrypt(encrypted).getOrNull()
    }

    override suspend fun save(request: CodexConfigurationRequest): CodexConfiguration = newSuspendedTransaction {
        val existing = CodexConfigurations.selectAll().singleOrNull()
        val now = Instant.now()
        val id = request.id ?: existing?.get(CodexConfigurations.id)?.value ?: UUID.randomUUID()
        val apiKey = request.apiKey?.trim()?.takeIf { it.isNotBlank() }
        val encryptedApiKey = when {
            apiKey != null -> encryptionService.encrypt(apiKey).getOrThrow()
            existing != null -> existing[CodexConfigurations.apiKeyEncrypted]
            else -> null
        }
        if (existing == null) {
            CodexConfigurations.insert {
                it[CodexConfigurations.id] = id
                it[isEnabled] = request.isEnabled
                it[cliPath] = request.cliPath.trim()
                it[CodexConfigurations.apiKeyEncrypted] = encryptedApiKey
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            CodexConfigurations.update({ CodexConfigurations.id eq id }) {
                it[isEnabled] = request.isEnabled
                it[cliPath] = request.cliPath.trim()
                it[CodexConfigurations.apiKeyEncrypted] = encryptedApiKey
                it[updatedAt] = now
            }
        }
        CodexConfigurations.selectAll()
            .where { CodexConfigurations.id eq id }
            .single()
            .toDomain()
    }

    private fun ResultRow.toDomain(): CodexConfiguration = CodexConfiguration(
        id = this[CodexConfigurations.id].value,
        isEnabled = this[CodexConfigurations.isEnabled],
        cliPath = this[CodexConfigurations.cliPath],
        hasApiKey = this[CodexConfigurations.apiKeyEncrypted] != null,
        createdAt = this[CodexConfigurations.createdAt],
        updatedAt = this[CodexConfigurations.updatedAt]
    )
}
