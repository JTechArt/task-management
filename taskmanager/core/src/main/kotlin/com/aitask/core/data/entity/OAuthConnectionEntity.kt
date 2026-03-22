package com.aitask.core.data.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object OAuthConnections : UUIDTable("oauth_connections") {
    val provider = varchar("provider", 50).uniqueIndex()
    val userId = varchar("user_id", 200).nullable()
    val teamId = varchar("team_id", 200).nullable()
    val scope = varchar("scope", 500).nullable()
    val accessTokenEncrypted = text("access_token_encrypted")
    val refreshTokenEncrypted = text("refresh_token_encrypted").nullable()
    val expiresAt = timestamp("expires_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}
