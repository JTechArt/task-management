package com.aitask.core.domain.repository

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityType
import java.util.UUID

/**
 * Repository for activity log entries
 */
interface ActivityRepository {
    /**
     * Create a new activity log entry
     */
    suspend fun create(activity: Activity): Activity
    
    /**
     * Find activity by ID
     */
    suspend fun findById(id: UUID): Activity?
    
    /**
     * Find all activities for a specific entity
     */
    suspend fun findByEntity(entityType: String, entityId: UUID): List<Activity>
    
    /**
     * Find all activities of a specific type
     */
    suspend fun findByType(type: ActivityType): List<Activity>
    
    /**
     * Find recent activities with optional limit
     */
    suspend fun findRecent(limit: Int = 50): List<Activity>
    
    /**
     * Delete activities older than the specified timestamp
     */
    suspend fun deleteOlderThan(timestamp: java.time.Instant): Int
}

