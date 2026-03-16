package com.aitask.core.data.repository

import com.aitask.core.data.entity.ActivityLog
import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.repository.ActivityRepository
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

private val json = Json { ignoreUnknownKeys = true }

class ActivityRepositoryImpl : ActivityRepository {

    override suspend fun create(activity: Activity): Activity = newSuspendedTransaction {
        ActivityLog.insert {
            it[id] = activity.id
            it[createdAt] = activity.createdAt
            it[activityType] = activity.type.name
            it[entityType] = activity.entityType
            it[entityId] = activity.entityId
            it[projectId] = activity.projectId
            it[description] = activity.description
            it[metadata] = if (activity.metadata.isEmpty()) null else json.encodeToString(
                MapSerializer(String.serializer(), String.serializer()),
                activity.metadata
            )
            it[status] = activity.status.name
        }
        activity
    }

    override suspend fun findById(id: UUID): Activity? = newSuspendedTransaction {
        ActivityLog.selectAll().where { ActivityLog.id eq id }
            .map { it.toActivity() }
            .singleOrNull()
    }

    override suspend fun findByEntity(entityType: String, entityId: UUID): List<Activity> =
        newSuspendedTransaction {
            ActivityLog.selectAll().where {
                (ActivityLog.entityType eq entityType) and (ActivityLog.entityId eq entityId)
            }
                .orderBy(ActivityLog.createdAt to SortOrder.DESC)
                .map { it.toActivity() }
        }

    override suspend fun findByType(type: ActivityType): List<Activity> = newSuspendedTransaction {
        ActivityLog.selectAll().where { ActivityLog.activityType eq type.name }
            .orderBy(ActivityLog.createdAt to SortOrder.DESC)
            .map { it.toActivity() }
    }

    override suspend fun findRecent(limit: Int): List<Activity> = newSuspendedTransaction {
        ActivityLog.selectAll()
            .orderBy(ActivityLog.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toActivity() }
    }

    override suspend fun findFiltered(
        projectId: UUID?,
        taskId: UUID?,
        type: ActivityType?,
        limit: Int
    ): List<Activity> = newSuspendedTransaction {
        val conditions = listOfNotNull(
            projectId?.let { ActivityLog.projectId eq it },
            taskId?.let { (ActivityLog.entityType eq "task") and (ActivityLog.entityId eq it) },
            type?.let { ActivityLog.activityType eq it.name }
        )
        val query = if (conditions.isEmpty()) {
            ActivityLog.selectAll()
        } else {
            ActivityLog.selectAll().where { conditions.reduce { a, b -> a and b } }
        }
        query
            .orderBy(ActivityLog.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toActivity() }
    }

    override suspend fun deleteOlderThan(timestamp: Instant): Int = newSuspendedTransaction {
        val toDelete = ActivityLog.selectAll()
            .where { ActivityLog.createdAt less timestamp }
            .map { it[ActivityLog.id].value }
        toDelete.forEach { id -> ActivityLog.deleteWhere { ActivityLog.id eq id } }
        toDelete.size
    }

    private fun ResultRow.toActivity(): Activity {
        val metadataString = this[ActivityLog.metadata]
        val metadataMap = when {
            metadataString.isNullOrBlank() -> emptyMap()
            else -> try {
                json.decodeFromString(
                    MapSerializer(String.serializer(), String.serializer()),
                    metadataString
                )
            } catch (_: Exception) {
                emptyMap()
            }
        }
        return Activity(
            id = this[ActivityLog.id].value,
            type = ActivityType.valueOf(this[ActivityLog.activityType]),
            entityType = this[ActivityLog.entityType],
            entityId = this[ActivityLog.entityId],
            description = this[ActivityLog.description],
            metadata = metadataMap,
            createdAt = this[ActivityLog.createdAt],
            status = ActivityStatus.valueOf(this[ActivityLog.status]),
            projectId = this[ActivityLog.projectId]
        )
    }
}
