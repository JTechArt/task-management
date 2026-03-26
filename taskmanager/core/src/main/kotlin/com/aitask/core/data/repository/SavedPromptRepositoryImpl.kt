package com.aitask.core.data.repository

import com.aitask.core.data.entity.SavedPrompts
import com.aitask.core.domain.model.AgentScope
import com.aitask.core.domain.model.SavedPrompt
import com.aitask.core.domain.model.SavedPromptRequest
import com.aitask.core.domain.repository.SavedPromptRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class SavedPromptRepositoryImpl : SavedPromptRepository {

    override suspend fun findAll(): List<SavedPrompt> = newSuspendedTransaction {
        SavedPrompts.selectAll()
            .orderBy(SavedPrompts.createdAt to SortOrder.DESC)
            .map { it.toDomain() }
    }

    override suspend fun findById(id: UUID): SavedPrompt? = newSuspendedTransaction {
        SavedPrompts.selectAll()
            .where { SavedPrompts.id eq id }
            .map { it.toDomain() }
            .singleOrNull()
    }

    override suspend fun findGlobal(): List<SavedPrompt> = newSuspendedTransaction {
        SavedPrompts.selectAll()
            .where { SavedPrompts.projectId.isNull() }
            .orderBy(SavedPrompts.createdAt to SortOrder.DESC)
            .map { it.toDomain() }
    }

    override suspend fun findByProject(projectId: UUID): List<SavedPrompt> = newSuspendedTransaction {
        SavedPrompts.selectAll()
            .where { SavedPrompts.projectId eq projectId }
            .orderBy(SavedPrompts.createdAt to SortOrder.DESC)
            .map { it.toDomain() }
    }

    override suspend fun findAvailableForProject(projectId: UUID?): List<SavedPrompt> = newSuspendedTransaction {
        SavedPrompts.selectAll()
            .where {
                if (projectId == null) {
                    SavedPrompts.projectId.isNull()
                } else {
                    SavedPrompts.projectId.isNull() or (SavedPrompts.projectId eq projectId)
                }
            }
            .orderBy(SavedPrompts.createdAt to SortOrder.DESC)
            .map { it.toDomain() }
    }

    override suspend fun save(request: SavedPromptRequest): SavedPrompt = newSuspendedTransaction {
        val existing = request.id?.let { findRowById(it) }
        val now = Instant.now()
        val id = request.id ?: UUID.randomUUID()

        if (existing == null) {
            SavedPrompts.insert {
                it[SavedPrompts.id] = id
                it[name] = request.name.trim()
                it[content] = request.content.trim()
                it[category] = request.category?.takeIf { c -> c.isNotBlank() }
                it[scope] = request.scope.name
                it[projectId] = request.projectId
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            SavedPrompts.update({ SavedPrompts.id eq id }) {
                it[name] = request.name.trim()
                it[content] = request.content.trim()
                it[category] = request.category?.takeIf { c -> c.isNotBlank() }
                it[scope] = request.scope.name
                it[projectId] = request.projectId
                it[updatedAt] = now
            }
        }

        findRowById(id)?.toDomain() ?: error("Saved prompt could not be reloaded after save")
    }

    override suspend fun delete(id: UUID): Boolean = newSuspendedTransaction {
        SavedPrompts.deleteWhere { SavedPrompts.id eq id } > 0
    }

    private fun findRowById(id: UUID): ResultRow? =
        SavedPrompts.selectAll()
            .where { SavedPrompts.id eq id }
            .singleOrNull()

    private fun ResultRow.toDomain(): SavedPrompt = SavedPrompt(
        id = this[SavedPrompts.id].value,
        name = this[SavedPrompts.name],
        content = this[SavedPrompts.content],
        category = this[SavedPrompts.category],
        scope = AgentScope.valueOf(this[SavedPrompts.scope]),
        projectId = this[SavedPrompts.projectId]?.value,
        createdAt = this[SavedPrompts.createdAt],
        updatedAt = this[SavedPrompts.updatedAt]
    )
}
