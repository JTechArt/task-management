package com.aitask.core.data.repository

import com.aitask.core.data.entity.Projects
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.repository.ProjectRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

class ProjectRepositoryImpl : ProjectRepository {
    
    override suspend fun findById(id: UUID): Project? = newSuspendedTransaction {
        Projects.selectAll().where { Projects.id eq id }
            .map { it.toProject() }
            .singleOrNull()
    }
    
    override suspend fun findAll(): List<Project> = newSuspendedTransaction {
        Projects.selectAll()
            .orderBy(Projects.createdAt to SortOrder.DESC)
            .map { it.toProject() }
    }
    
    override suspend fun findAllActive(): List<Project> = newSuspendedTransaction {
        Projects.selectAll().where { Projects.archivedAt.isNull() }
            .orderBy(Projects.createdAt to SortOrder.DESC)
            .map { it.toProject() }
    }
    
    override suspend fun findByName(name: String): Project? = newSuspendedTransaction {
        Projects.selectAll().where { Projects.name eq name }
            .map { it.toProject() }
            .singleOrNull()
    }
    
    override suspend fun create(project: Project): Project = newSuspendedTransaction {
        Projects.insert {
            it[id] = project.id
            it[name] = project.name
            it[description] = project.description
            it[workspacePath] = project.workspacePath
            it[branchTemplate] = project.branchTemplate
            it[tags] = project.tags
            it[team] = project.team
            it[createdAt] = project.createdAt
            it[updatedAt] = project.updatedAt
            it[archivedAt] = project.archivedAt
        }
        project
    }
    
    override suspend fun update(project: Project): Project = newSuspendedTransaction {
        Projects.update({ Projects.id eq project.id }) {
            it[name] = project.name
            it[description] = project.description
            it[workspacePath] = project.workspacePath
            it[branchTemplate] = project.branchTemplate
            it[tags] = project.tags
            it[team] = project.team
            it[updatedAt] = Instant.now()
            it[archivedAt] = project.archivedAt
        }
        project.copy(updatedAt = Instant.now())
    }
    
    override suspend fun archive(id: UUID): Project? = newSuspendedTransaction {
        val now = Instant.now()
        Projects.update({ Projects.id eq id }) {
            it[archivedAt] = now
            it[updatedAt] = now
        }
        findById(id)
    }
    
    override suspend fun delete(id: UUID) = newSuspendedTransaction {
        Projects.deleteWhere { Projects.id eq id }
        Unit
    }
    
    private fun ResultRow.toProject() = Project(
        id = this[Projects.id].value,
        name = this[Projects.name],
        description = this[Projects.description],
        workspacePath = this[Projects.workspacePath],
        branchTemplate = this[Projects.branchTemplate],
        tags = this[Projects.tags] ?: emptyList(),
        team = this[Projects.team],
        createdAt = this[Projects.createdAt],
        updatedAt = this[Projects.updatedAt],
        archivedAt = this[Projects.archivedAt]
    )
}

