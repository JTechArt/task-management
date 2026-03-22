package com.aitask.core.data.repository

import com.aitask.core.data.entity.PreRunScripts
import com.aitask.core.domain.model.PreRunScript
import com.aitask.core.domain.model.PreRunScriptType
import com.aitask.core.domain.repository.PreRunScriptRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class PreRunScriptRepositoryImpl : PreRunScriptRepository {

    override suspend fun findById(id: UUID): PreRunScript? = newSuspendedTransaction {
        PreRunScripts.selectAll()
            .where { PreRunScripts.id eq id }
            .map { it.toPreRunScript() }
            .singleOrNull()
    }

    override suspend fun findByProject(projectId: UUID): List<PreRunScript> = newSuspendedTransaction {
        PreRunScripts.selectAll()
            .where { PreRunScripts.projectId eq projectId }
            .orderBy(
                PreRunScripts.executionOrder to SortOrder.ASC,
                PreRunScripts.createdAt to SortOrder.ASC
            )
            .map { it.toPreRunScript() }
    }

    override suspend fun findByRepository(repositoryId: UUID): List<PreRunScript> = newSuspendedTransaction {
        PreRunScripts.selectAll()
            .where { PreRunScripts.repositoryId eq repositoryId }
            .orderBy(
                PreRunScripts.executionOrder to SortOrder.ASC,
                PreRunScripts.createdAt to SortOrder.ASC
            )
            .map { it.toPreRunScript() }
    }

    override suspend fun create(script: PreRunScript): PreRunScript = newSuspendedTransaction {
        PreRunScripts.insert {
            it[id] = script.id
            it[projectId] = script.projectId
            it[repositoryId] = script.repositoryId
            it[name] = script.name
            it[type] = script.type.name
            it[scriptPath] = script.scriptPath
            it[inlineScript] = script.inlineScript
            it[requiredValue] = script.requiredValue
            it[executionOrder] = script.executionOrder
            it[createdAt] = script.createdAt
            it[updatedAt] = script.updatedAt
        }
        script
    }

    override suspend fun update(script: PreRunScript): PreRunScript = newSuspendedTransaction {
        val updatedAt = Instant.now()
        PreRunScripts.update({ PreRunScripts.id eq script.id }) {
            it[projectId] = script.projectId
            it[repositoryId] = script.repositoryId
            it[name] = script.name
            it[type] = script.type.name
            it[scriptPath] = script.scriptPath
            it[inlineScript] = script.inlineScript
            it[requiredValue] = script.requiredValue
            it[executionOrder] = script.executionOrder
            it[PreRunScripts.updatedAt] = updatedAt
        }
        script.copy(updatedAt = updatedAt)
    }

    override suspend fun delete(id: UUID) = newSuspendedTransaction {
        PreRunScripts.deleteWhere { PreRunScripts.id eq id }
        Unit
    }

    private fun ResultRow.toPreRunScript() = PreRunScript(
        id = this[PreRunScripts.id].value,
        projectId = this[PreRunScripts.projectId].value,
        repositoryId = this[PreRunScripts.repositoryId]?.value,
        name = this[PreRunScripts.name],
        type = PreRunScriptType.valueOf(this[PreRunScripts.type]),
        scriptPath = this[PreRunScripts.scriptPath],
        inlineScript = this[PreRunScripts.inlineScript],
        requiredValue = this[PreRunScripts.requiredValue],
        executionOrder = this[PreRunScripts.executionOrder],
        createdAt = this[PreRunScripts.createdAt],
        updatedAt = this[PreRunScripts.updatedAt]
    )
}
