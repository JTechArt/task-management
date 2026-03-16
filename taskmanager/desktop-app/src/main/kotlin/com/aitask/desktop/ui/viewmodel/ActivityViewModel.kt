package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.model.Activity
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
import java.util.UUID

private const val ACTIVITY_LIMIT = 100

data class ActivityFilters(
    val projectId: UUID? = null,
    val taskId: UUID? = null,
    val type: ActivityType? = null
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
                val activities = activityRepository.findFiltered(
                    projectId = filters.projectId,
                    taskId = filters.taskId,
                    type = filters.type,
                    limit = ACTIVITY_LIMIT
                )
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
