package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.desktop.di.DependencyContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

private const val RECENT_TASKS_LIMIT = 10
private const val RECENT_ACTIVITY_LIMIT = 10

data class DashboardMetrics(
    val openTasksCount: Int,
    val totalTasksCount: Int,
    val projectsCount: Int,
    val rulesCount: Int,
    val inProgressCount: Int,
    val pendingCount: Int
)

data class DashboardUiState(
    val metrics: DashboardMetrics = DashboardMetrics(0, 0, 0, 0, 0, 0),
    val recentTasks: List<Task> = emptyList(),
    val projects: List<Project> = emptyList(),
    val recentActivity: List<Activity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val databaseConnected: Boolean = false
)

class DashboardViewModel(
    private val taskRepository: TaskRepository = DependencyContainer.taskRepository,
    private val projectRepository: ProjectRepository = DependencyContainer.projectRepository,
    private val activityRepository: ActivityRepository = DependencyContainer.activityRepository,
    private val ruleRepository: com.aitask.core.domain.repository.RuleRepository = DependencyContainer.ruleRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    var uiState by mutableStateOf(DashboardUiState())
        private set

    fun loadDashboard(databaseConnected: Boolean) {
        uiState = uiState.copy(isLoading = true, error = null, databaseConnected = databaseConnected)
        scope.launch {
            try {
                val projects = projectRepository.findAllActive()
                val allTasks = taskRepository.findAll()
                val inProgressTasks = taskRepository.findByStatus(TaskStatus.IN_PROGRESS)
                val pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING)
                val openTasks = (inProgressTasks + pendingTasks).distinctBy { it.id }
                    .sortedByDescending { it.updatedAt }
                    .take(RECENT_TASKS_LIMIT)
                val recentActivity = activityRepository.findRecent(RECENT_ACTIVITY_LIMIT)
                val rulesCount = ruleRepository.findAll().count { !it.isArchived }
                val metrics = DashboardMetrics(
                    openTasksCount = inProgressTasks.size + pendingTasks.size,
                    totalTasksCount = allTasks.size,
                    projectsCount = projects.size,
                    rulesCount = rulesCount,
                    inProgressCount = inProgressTasks.size,
                    pendingCount = pendingTasks.size
                )
                uiState = uiState.copy(
                    metrics = metrics,
                    recentTasks = openTasks,
                    projects = projects,
                    recentActivity = recentActivity,
                    isLoading = false
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard"
                )
            }
        }
    }

    fun findProjectName(projectId: UUID): String {
        return uiState.projects.find { it.id == projectId }?.name ?: "Unknown project"
    }
}
