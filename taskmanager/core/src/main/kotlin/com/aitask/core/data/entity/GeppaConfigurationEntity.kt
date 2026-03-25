package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object GeppaConfigurations : UUIDTable("geppa_configurations") {
    val isEnabled = bool("is_enabled").default(false)
    val endpointUrl = varchar("endpoint_url", 1000).default("")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
