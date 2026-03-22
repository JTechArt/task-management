package com.aitask.core.data.repository

import com.aitask.core.data.entity.OAuthConnections
import com.aitask.core.domain.model.OAuthConnection
import com.aitask.core.domain.model.OAuthProvider
import com.aitask.core.domain.model.OAuthToken
import com.aitask.core.domain.repository.OAuthConnectionRepository
import com.aitask.core.domain.service.EncryptionService
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

private fun parseProvider(value: String): OAuthProvider = OAuthProvider.valueOf(value)

class OAuthConnectionRepositoryImpl(
    private val encryptionService: EncryptionService
) : OAuthConnectionRepository {
    override suspend fun findByProvider(provider: OAuthProvider): OAuthConnection? =
        newSuspendedTransaction {
            OAuthConnections.selectAll()
                .where { OAuthConnections.provider eq provider.name }
                .map { it.toOAuthConnection() }
                .singleOrNull()
        }

    override suspend fun findById(id: UUID): OAuthConnection? = newSuspendedTransaction {
        OAuthConnections.selectAll().where { OAuthConnections.id eq id }
            .map { it.toOAuthConnection() }
            .singleOrNull()
    }

    override suspend fun save(
        provider: OAuthProvider,
        token: OAuthToken,
        userId: String?,
        teamId: String?
    ): OAuthConnection = newSuspendedTransaction {
        deleteByProvider(provider)
        val id = UUID.randomUUID()
        val accessEnc = encryptionService.encrypt(token.accessToken).getOrThrow()
        val refreshEnc = token.refreshToken?.let { encryptionService.encrypt(it).getOrThrow() }
        val now = Instant.now()
        OAuthConnections.insert {
            it[OAuthConnections.id] = id
            it[OAuthConnections.provider] = provider.name
            it[OAuthConnections.userId] = userId
            it[OAuthConnections.teamId] = teamId
            it[OAuthConnections.scope] = token.scope
            it[OAuthConnections.accessTokenEncrypted] = accessEnc
            it[OAuthConnections.refreshTokenEncrypted] = refreshEnc
            it[OAuthConnections.expiresAt] = token.expiresAt
            it[OAuthConnections.createdAt] = now
            it[OAuthConnections.updatedAt] = now
        }
        OAuthConnection(
            id = id,
            provider = provider,
            userId = userId,
            teamId = teamId,
            scope = token.scope,
            createdAt = now,
            updatedAt = now,
            expiresAt = token.expiresAt
        )
    }

    override suspend fun deleteByProvider(provider: OAuthProvider): Boolean =
        newSuspendedTransaction {
            val deleted = OAuthConnections.deleteWhere { OAuthConnections.provider eq provider.name }
            deleted > 0
        }

    override suspend fun getStoredToken(connectionId: UUID): OAuthToken? =
        newSuspendedTransaction {
            OAuthConnections.selectAll().where { OAuthConnections.id eq connectionId }
                .map { row ->
                    val access = encryptionService.decrypt(row[OAuthConnections.accessTokenEncrypted]).getOrNull() ?: return@newSuspendedTransaction null
                    val refresh = row[OAuthConnections.refreshTokenEncrypted]?.let {
                        encryptionService.decrypt(it).getOrNull()
                    }
                    OAuthToken(
                        accessToken = access,
                        refreshToken = refresh,
                        expiresAt = row[OAuthConnections.expiresAt],
                        scope = row[OAuthConnections.scope]
                    )
                }
                .singleOrNull()
        }

    override suspend fun updateToken(connectionId: UUID, token: OAuthToken): Boolean =
        newSuspendedTransaction {
            val accessEnc = encryptionService.encrypt(token.accessToken).getOrThrow()
            val refreshEnc = token.refreshToken?.let { encryptionService.encrypt(it).getOrThrow() }
            val now = Instant.now()
            val updated = OAuthConnections.update({ OAuthConnections.id eq connectionId }) {
                it[OAuthConnections.accessTokenEncrypted] = accessEnc
                it[OAuthConnections.refreshTokenEncrypted] = refreshEnc
                it[OAuthConnections.expiresAt] = token.expiresAt
                it[OAuthConnections.updatedAt] = now
            }
            updated > 0
        }

    private fun ResultRow.toOAuthConnection(): OAuthConnection = OAuthConnection(
        id = this[OAuthConnections.id].value,
        provider = parseProvider(this[OAuthConnections.provider]),
        userId = this[OAuthConnections.userId],
        teamId = this[OAuthConnections.teamId],
        scope = this[OAuthConnections.scope],
        createdAt = this[OAuthConnections.createdAt],
        updatedAt = this[OAuthConnections.updatedAt],
        expiresAt = this[OAuthConnections.expiresAt]
    )
}
