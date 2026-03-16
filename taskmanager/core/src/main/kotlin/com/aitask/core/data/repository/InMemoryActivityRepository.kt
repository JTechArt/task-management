package com.aitask.core.data.repository

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.repository.ActivityRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of ActivityRepository for MVP
 * TODO: Replace with database-backed implementation
 */
class InMemoryActivityRepository : ActivityRepository {
    
    private val activities = ConcurrentHashMap<UUID, Activity>()
    
    override suspend fun create(activity: Activity): Activity {
        activities[activity.id] = activity
        return activity
    }
    
    override suspend fun findById(id: UUID): Activity? {
        return activities[id]
    }
    
    override suspend fun findByEntity(entityType: String, entityId: UUID): List<Activity> {
        return activities.values
            .filter { it.entityType == entityType && it.entityId == entityId }
            .sortedByDescending { it.createdAt }
    }
    
    override suspend fun findByType(type: ActivityType): List<Activity> {
        return activities.values
            .filter { it.type == type }
            .sortedByDescending { it.createdAt }
    }
    
    override suspend fun findRecent(limit: Int): List<Activity> {
        return activities.values
            .sortedByDescending { it.createdAt }
            .take(limit)
    }
    
    override suspend fun findFiltered(
        projectId: UUID?,
        taskId: UUID?,
        type: com.aitask.core.domain.model.ActivityType?,
        limit: Int
    ): List<Activity> {
        return activities.values
            .filter { act ->
                (projectId == null || act.projectId == projectId) &&
                (taskId == null || (act.entityType == "task" && act.entityId == taskId)) &&
                (type == null || act.type == type)
            }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }
    
    override suspend fun deleteOlderThan(timestamp: Instant): Int {
        val toDelete = activities.values.filter { it.createdAt < timestamp }
        toDelete.forEach { activities.remove(it.id) }
        return toDelete.size
    }
}

