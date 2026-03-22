package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object PreRunScripts : UUIDTable("pre_run_scripts") {
    val projectId = reference("project_id", Projects, onDelete = ReferenceOption.CASCADE)
    val repositoryId = reference("repository_id", Repositories, onDelete = ReferenceOption.CASCADE).nullable()
    val name = varchar("name", 200)
    val type = varchar("type", 50)
    val scriptPath = varchar("script_path", 1000).nullable()
    val inlineScript = text("inline_script").nullable()
    val requiredValue = varchar("required_value", 200).nullable()
    val executionOrder = integer("execution_order").default(0)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
