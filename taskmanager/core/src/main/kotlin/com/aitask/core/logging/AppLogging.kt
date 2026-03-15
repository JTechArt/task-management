package com.aitask.core.logging

import com.aitask.core.config.AppConfig
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun logStartup(config: AppConfig) {
    logger.info { "Starting ${config.appName} with database=${config.database.redactSensitiveValues()}" }
}

