package com.aitask.core.data.repository

import com.aitask.core.data.entity.Repositories
import com.aitask.core.domain.model.AuthType
import com.aitask.core.domain.model.GitProvider
import com.aitask.core.domain.model.IDEType
import com.aitask.core.domain.model.Repository
import com.aitask.core.domain.repository.RepositoryRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

class RepositoryRepositoryImpl : RepositoryRepository {
    
    override suspend fun findById(id: UUID): Repository? = newSuspendedTransaction {
        Repositories.selectAll().where { Repositories.id eq id }
            .map { it.toRepository() }
            .singleOrNull()
    }
    
    override suspend fun findByProject(projectId: UUID): List<Repository> = newSuspendedTransaction {
        Repositories.selectAll().where { Repositories.projectId eq projectId }
            .orderBy(Repositories.isPrimary to SortOrder.DESC, Repositories.createdAt to SortOrder.ASC)
            .map { it.toRepository() }
    }
    
    override suspend fun findPrimaryByProject(projectId: UUID): Repository? = newSuspendedTransaction {
        Repositories.selectAll().where {
            (Repositories.projectId eq projectId) and (Repositories.isPrimary eq true)
        }
            .map { it.toRepository() }
            .singleOrNull()
    }
    
    override suspend fun create(repository: Repository): Repository = newSuspendedTransaction {
        Repositories.insert {
            it[id] = repository.id
            it[projectId] = repository.projectId
            it[name] = repository.name
            it[cloneUrl] = repository.cloneUrl
            it[provider] = repository.provider.name
            it[authType] = repository.authType.name
            it[preferredIDEs] = repository.preferredIDEs.map { ide -> ide.name }.toTypedArray()
            it[isPrimary] = repository.isPrimary
            it[createdAt] = repository.createdAt
            it[updatedAt] = repository.updatedAt
        }
        repository
    }
    
    override suspend fun update(repository: Repository): Repository = newSuspendedTransaction {
        Repositories.update({ Repositories.id eq repository.id }) {
            it[name] = repository.name
            it[cloneUrl] = repository.cloneUrl
            it[provider] = repository.provider.name
            it[authType] = repository.authType.name
            it[preferredIDEs] = repository.preferredIDEs.map { ide -> ide.name }.toTypedArray()
            it[isPrimary] = repository.isPrimary
            it[updatedAt] = Instant.now()
        }
        repository.copy(updatedAt = Instant.now())
    }
    
    override suspend fun delete(id: UUID) = newSuspendedTransaction {
        Repositories.deleteWhere { Repositories.id eq id }
        Unit
    }
    
    private fun ResultRow.toRepository() = Repository(
        id = this[Repositories.id].value,
        projectId = this[Repositories.projectId].value,
        name = this[Repositories.name],
        cloneUrl = this[Repositories.cloneUrl],
        provider = GitProvider.valueOf(this[Repositories.provider]),
        authType = AuthType.valueOf(this[Repositories.authType]),
        preferredIDEs = this[Repositories.preferredIDEs]?.map { IDEType.valueOf(it) } ?: emptyList(),
        isPrimary = this[Repositories.isPrimary],
        createdAt = this[Repositories.createdAt],
        updatedAt = this[Repositories.updatedAt]
    )
}

