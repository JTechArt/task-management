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
import com.aitask.desktop.ui.*
import com.aitask.desktop.ui.projects.ProjectsView
import com.aitask.desktop.ui.tasks.TasksView
import com.aitask.desktop.ui.rules.RulesView
import io.github.oshai.kotlinlogging.KotlinLogging

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
        application {
            Window(
                onCloseRequest = ::exitApplication,
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
                        NavigationItem.DASHBOARD -> {
                            val status = SystemStatus(
                                databaseConnected = bootstrapResult.databaseInitialized,
                                databaseMessage = if (bootstrapResult.databaseInitialized) {
                                    "Successfully connected • ${bootstrapResult.migrationsApplied} migrations applied"
                                } else {
                                    bootstrapResult.databaseError ?: "Not connected"
                                }
                            )
                            StatusView(config = config, status = status)
                        }
                        NavigationItem.PROJECTS -> ProjectsView()
                        NavigationItem.TASKS -> TasksView()
                        NavigationItem.RULES -> RulesView()
                        NavigationItem.INTEGRATIONS -> IntegrationsView()
                        NavigationItem.SETTINGS -> SettingsView()
                    }
                }
            }
        }
    }
}

