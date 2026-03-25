package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object LlmConfigurations : UUIDTable("llm_configurations") {
    val name = varchar("name", 200)
    val endpointUrl = varchar("endpoint_url", 1000)
    val modelIdentifier = varchar("model_identifier", 200)
    val apiKeyEncrypted = text("api_key_encrypted").nullable()
    val isDefault = bool("is_default").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
