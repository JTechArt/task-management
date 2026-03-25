package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object PluginConfigurations : UUIDTable("plugin_configurations") {
    val pluginId = varchar("plugin_id", 200)
    val scope = varchar("scope", 20)
    val scopeKey = varchar("scope_key", 200).nullable()
    val activeConfigurationJson = text("active_configuration_json")
    val lastKnownGoodConfigurationJson = text("last_known_good_configuration_json").nullable()
    val validationStatus = varchar("validation_status", 20).default("UNKNOWN")
    val validationMessage = text("validation_message").nullable()
    val lastValidatedAt = long("last_validated_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    init {
        uniqueIndex(pluginId, scope, scopeKey)
    }
}
