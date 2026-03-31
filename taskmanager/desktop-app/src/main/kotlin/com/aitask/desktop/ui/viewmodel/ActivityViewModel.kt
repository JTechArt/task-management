package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.desktop.di.DependencyContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private const val ACTIVITY_LIMIT = 100

data class ActivityFilters(
    val projectId: UUID? = null,
    val taskId: UUID? = null,
    val type: ActivityType? = null,
    val status: ActivityStatus? = null,
    /** Inclusive start date as yyyy-MM-dd in system default zone, or blank. */
    val dateFrom: String = "",
    /** Inclusive end date as yyyy-MM-dd in system default zone, or blank. */
    val dateTo: String = "",
    /** Case-insensitive filter on description and metadata values (client-side). */
    val searchQuery: String = ""
)

data class ActivityUiState(
    val activities: List<Activity> = emptyList(),
    val projects: List<Project> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val filters: ActivityFilters = ActivityFilters(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ActivityViewModel(
    private val activityRepository: ActivityRepository = DependencyContainer.activityRepository,
    private val projectRepository: ProjectRepository = DependencyContainer.projectRepository,
    private val taskRepository: TaskRepository = DependencyContainer.taskRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    var uiState by mutableStateOf(ActivityUiState())
        private set

    fun loadActivities() {
        val filters = uiState.filters
        uiState = uiState.copy(isLoading = true, error = null)
        scope.launch {
            try {
                val projects = projectRepository.findAllActive()
                val tasks = filters.projectId?.let { taskRepository.findByProject(it) }
                    ?: taskRepository.findAll()
                val zoneId: ZoneId = ZoneId.systemDefault()
                val afterInclusive: Instant? = parseDate(filters.dateFrom)?.atStartOfDay(zoneId)?.toInstant()
                val beforeExclusive: Instant? = parseDate(filters.dateTo)?.plusDays(1)?.atStartOfDay(zoneId)?.toInstant()
                val loaded = activityRepository.findFiltered(
                    projectId = filters.projectId,
                    taskId = filters.taskId,
                    type = filters.type,
                    status = filters.status,
                    createdAfterInclusive = afterInclusive,
                    createdBeforeExclusive = beforeExclusive,
                    limit = ACTIVITY_LIMIT
                )
                val q = filters.searchQuery.trim().lowercase()
                val activities: List<Activity> = if (q.isEmpty()) {
                    loaded
                } else {
                    loaded.filter { act -> activityMatchesSearch(act, q) }
                }
                uiState = uiState.copy(
                    activities = activities,
                    projects = projects,
                    tasks = tasks,
                    isLoading = false
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load activity"
                )
            }
        }
    }

    fun applyFilters(filters: ActivityFilters) {
        uiState = uiState.copy(filters = filters)
        loadActivities()
    }

    fun setStatusFilter(status: ActivityStatus?) {
        uiState = uiState.copy(filters = uiState.filters.copy(status = status))
        loadActivities()
    }

    fun setDateFrom(raw: String) {
        uiState = uiState.copy(filters = uiState.filters.copy(dateFrom = raw))
        loadActivities()
    }

    fun setDateTo(raw: String) {
        uiState = uiState.copy(filters = uiState.filters.copy(dateTo = raw))
        loadActivities()
    }

    fun setSearchQuery(raw: String) {
        uiState = uiState.copy(filters = uiState.filters.copy(searchQuery = raw))
        loadActivities()
    }

    fun setProjectFilter(projectId: UUID?) {
        uiState = uiState.copy(
            filters = uiState.filters.copy(projectId = projectId, taskId = null)
        )
        loadActivities()
    }

    fun setTaskFilter(taskId: UUID?) {
        uiState = uiState.copy(
            filters = uiState.filters.copy(taskId = taskId)
        )
        loadActivities()
    }

    fun setTypeFilter(type: ActivityType?) {
        uiState = uiState.copy(
            filters = uiState.filters.copy(type = type)
        )
        loadActivities()
    }

    fun clearFilters() {
        uiState = uiState.copy(
            filters = ActivityFilters()
        )
        loadActivities()
    }
}

private fun parseDate(raw: String): LocalDate? {
    val s: String = raw.trim()
    if (s.isEmpty()) {
        return null
    }
    return runCatching { LocalDate.parse(s) }.getOrNull()
}

private fun activityMatchesSearch(activity: Activity, q: String): Boolean {
    if (activity.description.lowercase().contains(q)) {
        return true
    }
    return activity.metadata.values.any { v -> v.lowercase().contains(q) }
}
