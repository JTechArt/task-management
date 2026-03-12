package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object Repositories : UUIDTable("repositories") {
    val projectId = reference("project_id", Projects, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 200)
    val cloneUrl = varchar("clone_url", 500)
    val provider = varchar("provider", 50)
    val authType = varchar("auth_type", 50)
    val preferredIDEs = array<String>("preferred_ides").nullable()
    val isPrimary = bool("is_primary").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

