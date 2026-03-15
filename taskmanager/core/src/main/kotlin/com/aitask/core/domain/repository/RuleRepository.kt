package com.aitask.core.domain.repository

import com.aitask.core.domain.model.ProjectRule
import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleScope
import java.util.UUID

interface RuleRepository {
    suspend fun findById(id: UUID): Rule?
    suspend fun findAll(): List<Rule>
    suspend fun findByScope(scope: RuleScope): List<Rule>
    suspend fun findByProject(projectId: UUID): List<Rule>
    suspend fun create(rule: Rule): Rule
    suspend fun update(rule: Rule): Rule
    suspend fun delete(id: UUID)
    
    // Project rule associations
    suspend fun attachToProject(projectId: UUID, ruleId: UUID): ProjectRule
    suspend fun detachFromProject(projectId: UUID, ruleId: UUID)
    suspend fun findProjectRules(projectId: UUID): List<ProjectRule>
}

