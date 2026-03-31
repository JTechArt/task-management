package com.aitask.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.aitask.core.config.AppConfig
import com.aitask.core.config.ConfigLoader
import com.aitask.core.config.EnvConfigLoader
import com.aitask.core.logging.logStartup
import com.aitask.desktop.di.DependencyContainer
import com.aitask.desktop.ui.*
import com.aitask.core.domain.model.ActivityType
import com.aitask.desktop.ui.activity.ActivityView
import com.aitask.desktop.ui.viewmodel.ActivityFilters
import com.aitask.desktop.ui.dashboard.DashboardView
import com.aitask.desktop.ui.integrations.IntegrationsView
import com.aitask.desktop.ui.plugins.PluginManagementView
import com.aitask.desktop.ui.slack.SlackAnalyzerView
import com.aitask.desktop.ui.projects.ProjectsView
import com.aitask.desktop.ui.tasks.TasksView
import com.aitask.desktop.ui.rules.RulesView
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

fun main() = AppLauncher().launch()

class AppLauncher(
    private val configLoader: ConfigLoader = EnvConfigLoader(),
    private val bootstrapper: StartupBootstrapper = StartupBootstrapper(),
    private val environment: Map<String, String> = System.getenv()
) {
    fun launch() {
        val config = configLoader.loadAppConfig()
        logStartup(config)
        val bootstrapDatabase = environment["BOOTSTRAP_DATABASE"]?.let { it.equals("false", true) } != true
        val bootstrapResult = bootstrapper.prepareInfrastructure(config, bootstrapDatabase)
        logger.info { "Bootstrap result databaseInitialized=${bootstrapResult.databaseInitialized}, migrationsApplied=${bootstrapResult.migrationsApplied}" }
        if (bootstrapResult.databaseError != null) {
            logger.warn { "Database initialization issue: ${bootstrapResult.databaseError}" }
        }
        if (bootstrapResult.databaseInitialized) {
            runBlocking {
                DependencyContainer.mcpBridgeService.sync()
            }
        }
        application {
            Window(
                onCloseRequest = {
                    runBlocking { DependencyContainer.mcpBridgeService.stop() }
                    exitApplication()
                },
                title = config.appName
            ) {
                AppSurface(config, bootstrapResult)
            }
        }
    }

    fun runStartupChecks() {
        val config = configLoader.loadAppConfig()
        logStartup(config)
        val bootstrapDatabase = environment["BOOTSTRAP_DATABASE"]?.let { it.equals("false", true) } != true
        val bootstrapResult = bootstrapper.prepareInfrastructure(config, bootstrapDatabase)
        logger.info { "Startup check bootstrap databaseInitialized=${bootstrapResult.databaseInitialized}" }
        logger.info { "Startup check completed" }
    }
}

@Composable
fun AppSurface(config: AppConfig, bootstrapResult: BootstrapResult) {
    var selectedNavItem by remember { mutableStateOf(NavigationItem.DASHBOARD) }
    var taskIdToSelectWhenShowingTasks by remember { mutableStateOf<java.util.UUID?>(null) }
    var pendingActivityFilters by remember { mutableStateOf<ActivityFilters?>(null) }

    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Navigation sidebar
                NavigationSidebar(
                    selectedItem = selectedNavItem,
                    onItemSelected = { selectedNavItem = it }
                )

                // Main content area
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedNavItem) {
                        NavigationItem.DASHBOARD -> DashboardView(
                            databaseConnected = bootstrapResult.databaseInitialized,
                            onNavigate = { selectedNavItem = it },
                            onNavigateToTask = { taskId ->
                                taskIdToSelectWhenShowingTasks = taskId
                                selectedNavItem = NavigationItem.TASKS
                            }
                        )
                        NavigationItem.PROJECTS -> ProjectsView(
                            onOpenAutomationHistoryForProject = { projectId ->
                                pendingActivityFilters = ActivityFilters(
                                    projectId = projectId,
                                    type = ActivityType.AGENT_EXECUTED
                                )
                                selectedNavItem = NavigationItem.ACTIVITY
                            }
                        )
                        NavigationItem.ACTIVITY -> ActivityView(
                            databaseConnected = bootstrapResult.databaseInitialized,
                            onNavigateToTask = { taskId ->
                                taskIdToSelectWhenShowingTasks = taskId
                                selectedNavItem = NavigationItem.TASKS
                            },
                            pendingFilters = pendingActivityFilters,
                            onConsumePendingFilters = { pendingActivityFilters = null }
                        )
                        NavigationItem.TASKS -> TasksView(
                            taskIdToSelectOnMount = taskIdToSelectWhenShowingTasks,
                            onMountedAfterSelection = { taskIdToSelectWhenShowingTasks = null },
                            onOpenAutomationHistoryForTask = { taskId ->
                                pendingActivityFilters = ActivityFilters(
                                    taskId = taskId,
                                    type = ActivityType.AGENT_EXECUTED
                                )
                                selectedNavItem = NavigationItem.ACTIVITY
                            }
                        )
                        NavigationItem.RULES -> RulesView()
                        NavigationItem.INTEGRATIONS -> IntegrationsView()
                        NavigationItem.PLUGINS -> PluginManagementView()
                        NavigationItem.SLACK_ANALYZER -> SlackAnalyzerView()
                        NavigationItem.SETTINGS -> SettingsView()
                    }
                }
            }
        }
    }
}
