package com.aitask.core.data.entity

import com.aitask.core.domain.model.*
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.UUID

object Rules : UUIDTable("rules") {
    val name = varchar("name", 200)
    val content = text("content")
    val category = varchar("category", 100).nullable()
    val scope = varchar("scope", 50)
    val targetIDE = varchar("target_ide", 50).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    val archivedAt = timestamp("archived_at").nullable()
}

object ProjectRules : Table("project_rules") {
    val projectId = reference("project_id", Projects, onDelete = ReferenceOption.CASCADE)
    val ruleId = reference("rule_id", Rules, onDelete = ReferenceOption.CASCADE)
    val appliedAt = timestamp("applied_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(projectId, ruleId)
}

fun ResultRow.toRule(): Rule = Rule(
    id = this[Rules.id].value,
    name = this[Rules.name],
    content = this[Rules.content],
    category = this[Rules.category]?.let { RuleCategory.valueOf(it) },
    scope = RuleScope.valueOf(this[Rules.scope]),
    targetIDE = this[Rules.targetIDE]?.let { IDEType.valueOf(it) },
    createdAt = this[Rules.createdAt],
    updatedAt = this[Rules.updatedAt],
    archivedAt = this[Rules.archivedAt]
)

fun ResultRow.toProjectRule(): ProjectRule = ProjectRule(
    projectId = this[ProjectRules.projectId].value,
    ruleId = this[ProjectRules.ruleId].value,
    appliedAt = this[ProjectRules.appliedAt]
)

