package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object AgentDefinitions : UUIDTable("agent_definitions") {
    val name = varchar("name", 200)
    val description = text("description").nullable()
    val promptTemplate = text("prompt_template")
    val llmConfigurationId = reference("llm_configuration_id", LlmConfigurations, onDelete = ReferenceOption.SET_NULL).nullable()
    val scope = varchar("scope", 50)
    val projectId = reference("project_id", Projects, onDelete = ReferenceOption.CASCADE).nullable()
    val trigger = varchar("trigger", 50)
    val isEnabled = bool("is_enabled").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

