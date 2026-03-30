package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object ClaudeConfigurations : UUIDTable("claude_configurations") {
    val isEnabled = bool("is_enabled").default(false)
    val integrationMode = varchar("integration_mode", 16).default("CLI")
    val cliPath = varchar("cli_path", 2000).default("")
    val apiBaseUrl = varchar("api_base_url", 2000).nullable()
    val apiKeyEncrypted = text("api_key_encrypted").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
