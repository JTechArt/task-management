package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object CodexConfigurations : UUIDTable("codex_configurations") {
    val isEnabled = bool("is_enabled").default(false)
    val cliPath = varchar("cli_path", 2000).default("")
    val apiKeyEncrypted = text("api_key_encrypted").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
