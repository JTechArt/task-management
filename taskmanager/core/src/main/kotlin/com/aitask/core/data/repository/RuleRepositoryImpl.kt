package com.aitask.core.data.repository

import com.aitask.core.data.entity.*
import com.aitask.core.domain.model.ProjectRule
import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleScope
import com.aitask.core.domain.repository.RuleRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class RuleRepositoryImpl : RuleRepository {
    
    override suspend fun findById(id: UUID): Rule? = newSuspendedTransaction {
        Rules.selectAll().where { Rules.id eq id }
            .map { it.toRule() }
            .singleOrNull()
    }
    
    override suspend fun findAll(): List<Rule> = newSuspendedTransaction {
        Rules.selectAll()
            .orderBy(Rules.createdAt to SortOrder.DESC)
            .map { it.toRule() }
    }
    
    override suspend fun findByScope(scope: RuleScope): List<Rule> = newSuspendedTransaction {
        Rules.selectAll().where { Rules.scope eq scope.name }
            .orderBy(Rules.createdAt to SortOrder.DESC)
            .map { it.toRule() }
    }
    
    override suspend fun findByProject(projectId: UUID): List<Rule> = newSuspendedTransaction {
        (Rules innerJoin ProjectRules)
            .selectAll()
            .where { ProjectRules.projectId eq projectId }
            .orderBy(ProjectRules.appliedAt to SortOrder.DESC)
            .map { it.toRule() }
    }
    
    override suspend fun create(rule: Rule): Rule = newSuspendedTransaction {
        Rules.insert {
            it[id] = rule.id
            it[name] = rule.name
            it[content] = rule.content
            it[category] = rule.category?.name
            it[scope] = rule.scope.name
            it[targetIDE] = rule.targetIDE?.name
            it[createdAt] = rule.createdAt
            it[updatedAt] = rule.updatedAt
            it[archivedAt] = rule.archivedAt
        }
        rule
    }
    
    override suspend fun update(rule: Rule): Rule = newSuspendedTransaction {
        Rules.update({ Rules.id eq rule.id }) {
            it[name] = rule.name
            it[content] = rule.content
            it[category] = rule.category?.name
            it[scope] = rule.scope.name
            it[targetIDE] = rule.targetIDE?.name
            it[updatedAt] = rule.updatedAt
            it[archivedAt] = rule.archivedAt
        }
        rule
    }
    
    override suspend fun delete(id: UUID) = newSuspendedTransaction {
        Rules.deleteWhere { Rules.id eq id }
        Unit
    }
    
    override suspend fun attachToProject(projectId: UUID, ruleId: UUID): ProjectRule = newSuspendedTransaction {
        val now = java.time.Instant.now()
        ProjectRules.insert {
            it[ProjectRules.projectId] = projectId
            it[ProjectRules.ruleId] = ruleId
            it[appliedAt] = now
        }
        ProjectRule(projectId, ruleId, now)
    }
    
    override suspend fun detachFromProject(projectId: UUID, ruleId: UUID) = newSuspendedTransaction {
        ProjectRules.deleteWhere {
            (ProjectRules.projectId eq projectId) and (ProjectRules.ruleId eq ruleId)
        }
        Unit
    }
    
    override suspend fun findProjectRules(projectId: UUID): List<ProjectRule> = newSuspendedTransaction {
        ProjectRules.selectAll()
            .where { ProjectRules.projectId eq projectId }
            .orderBy(ProjectRules.appliedAt to SortOrder.DESC)
            .map { it.toProjectRule() }
    }
}

