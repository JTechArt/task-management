package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object Projects : UUIDTable("projects") {
    val name = varchar("name", 200).uniqueIndex()
    val description = text("description").nullable()
    val workspacePath = varchar("workspace_path", 1000)
    val branchTemplate = varchar("branch_template", 200).default("task-{taskId}")
    val retentionPolicy = varchar("retention_policy", 50).default("KEEP_ALL")
    val tags = array<String>("tags").nullable()
    val team = varchar("team", 200).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    val archivedAt = timestamp("archived_at").nullable()
}

