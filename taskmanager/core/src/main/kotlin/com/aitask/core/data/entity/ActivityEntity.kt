package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object ActivityLog : UUIDTable("activity_log") {
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val activityType = varchar("activity_type", 50)
    val entityType = varchar("entity_type", 50)
    val entityId = uuid("entity_id")
    val projectId = uuid("project_id").nullable()
    val description = text("description")
    val metadata = text("metadata").nullable()
    val status = varchar("status", 50).default("SUCCESS")
}
