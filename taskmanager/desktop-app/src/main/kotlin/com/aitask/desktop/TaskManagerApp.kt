package com.aitask.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.aitask.core.config.AppConfig
import com.aitask.core.config.ConfigLoader
import com.aitask.core.config.EnvConfigLoader
import com.aitask.core.logging.logStartup
import com.aitask.desktop.StartupBootstrapper
import mu.KotlinLogging

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
        val bootstrapDatabase = environment["BOOTSTRAP_DATABASE"]?.equals("true", true) ?: false
        val bootstrapResult = bootstrapper.prepareInfrastructure(config, bootstrapDatabase)
        logger.info { "Bootstrap result databaseInitialized=${bootstrapResult.databaseInitialized}" }
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = config.appName
            ) {
                AppSurface(config)
            }
        }
    }

    fun runStartupChecks() {
        val config = configLoader.loadAppConfig()
        logStartup(config)
        val bootstrapDatabase = environment["BOOTSTRAP_DATABASE"]?.equals("true", true) ?: false
        val bootstrapResult = bootstrapper.prepareInfrastructure(config, bootstrapDatabase)
        logger.info { "Startup check bootstrap databaseInitialized=${bootstrapResult.databaseInitialized}" }
        logger.info { "Startup check completed" }
    }
}

@Composable
fun AppSurface(config: AppConfig) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            WelcomeCard(config)
        }
    }
}

@Composable
fun WelcomeCard(config: AppConfig) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = config.appName,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Compose Desktop scaffold is ready.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Database: ${config.database.redactSensitiveValues()}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

