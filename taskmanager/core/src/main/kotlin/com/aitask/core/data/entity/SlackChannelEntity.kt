package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object SlackChannels : UUIDTable("slack_channels") {
    val webhookUrl = varchar("webhook_url", 500)
    val channelDisplayName = varchar("channel_display_name", 200)
    val projectId = reference("project_id", Projects, onDelete = ReferenceOption.CASCADE)
    val enabledEvents = text("enabled_events").default("")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
