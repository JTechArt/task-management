package com.aitask.core.data.repository

import com.aitask.core.data.entity.GeppaConfigurations
import com.aitask.core.domain.model.GeppaConfiguration
import com.aitask.core.domain.model.GeppaConfigurationRequest
import com.aitask.core.domain.repository.GeppaConfigurationRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class GeppaConfigurationRepositoryImpl : GeppaConfigurationRepository {

    override suspend fun find(): GeppaConfiguration? = newSuspendedTransaction {
        GeppaConfigurations.selectAll()
            .map { it.toDomain() }
            .singleOrNull()
    }

    override suspend fun save(request: GeppaConfigurationRequest): GeppaConfiguration = newSuspendedTransaction {
        val existing = GeppaConfigurations.selectAll().singleOrNull()
        val now = Instant.now()
        val id = request.id ?: existing?.get(GeppaConfigurations.id)?.value ?: UUID.randomUUID()

        if (existing == null) {
            GeppaConfigurations.insert {
                it[GeppaConfigurations.id] = id
                it[isEnabled] = request.isEnabled
                it[endpointUrl] = request.endpointUrl.trim()
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            GeppaConfigurations.update({ GeppaConfigurations.id eq id }) {
                it[isEnabled] = request.isEnabled
                it[endpointUrl] = request.endpointUrl.trim()
                it[updatedAt] = now
            }
        }

        GeppaConfigurations.selectAll()
            .where { GeppaConfigurations.id eq id }
            .single()
            .toDomain()
    }

    private fun ResultRow.toDomain(): GeppaConfiguration = GeppaConfiguration(
        id = this[GeppaConfigurations.id].value,
        isEnabled = this[GeppaConfigurations.isEnabled],
        endpointUrl = this[GeppaConfigurations.endpointUrl],
        createdAt = this[GeppaConfigurations.createdAt],
        updatedAt = this[GeppaConfigurations.updatedAt]
    )
}
