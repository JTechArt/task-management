package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object Tasks : UUIDTable("tasks") {
    val title = varchar("title", 500)
    val description = text("description").nullable()
    val taskType = varchar("task_type", 50)
    val status = varchar("status", 50).default("PENDING")
    val projectId = reference("project_id", Projects, onDelete = ReferenceOption.CASCADE)
    val workspacePath = varchar("workspace_path", 1000).nullable()
    val branchName = varchar("branch_name", 200).nullable()
    val workspaceCleanedAt = timestamp("workspace_cleaned_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    val completedAt = timestamp("completed_at").nullable()
}

