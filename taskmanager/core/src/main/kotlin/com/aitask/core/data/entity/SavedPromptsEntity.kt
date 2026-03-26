package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object SavedPrompts : UUIDTable("saved_prompts") {
    val name = varchar("name", 200)
    val content = text("content")
    val category = varchar("category", 100).nullable()
    val scope = varchar("scope", 50)
    val projectId = reference("project_id", Projects, onDelete = ReferenceOption.CASCADE).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
