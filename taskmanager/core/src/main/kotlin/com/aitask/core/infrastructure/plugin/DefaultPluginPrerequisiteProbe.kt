package com.aitask.core.infrastructure.plugin

import com.aitask.core.domain.model.IDEType
import com.aitask.core.domain.plugin.PluginPrerequisite
import com.aitask.core.domain.plugin.PluginPrerequisiteResult
import com.aitask.core.domain.plugin.PluginPrerequisiteType
import com.aitask.core.domain.service.IDEService
import com.aitask.core.domain.service.PluginPrerequisiteProbe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

class DefaultPluginPrerequisiteProbe(
    private val ideService: IDEService,
    private val environment: Map<String, String> = System.getenv()
) : PluginPrerequisiteProbe {
    override suspend fun evaluate(
        prerequisite: PluginPrerequisite,
        pluginConfiguration: Map<String, String>
    ): PluginPrerequisiteResult = withContext(Dispatchers.IO) {
        when (prerequisite.type) {
            PluginPrerequisiteType.LOCAL_BINARY -> evaluateBinary(prerequisite)
            PluginPrerequisiteType.ENDPOINT -> evaluateEndpoint(prerequisite)
            PluginPrerequisiteType.CREDENTIAL -> evaluateCredential(prerequisite, pluginConfiguration)
            PluginPrerequisiteType.COMPANION_APP -> evaluateCompanionApp(prerequisite)
        }
    }

    private fun evaluateBinary(prerequisite: PluginPrerequisite): PluginPrerequisiteResult {
        val present = environment["PATH"]
            ?.split(File.pathSeparator)
            ?.any { path ->
                val base = File(path, prerequisite.value)
                base.exists() && base.canExecute()
            } == true || File(prerequisite.value).exists()

        return PluginPrerequisiteResult(
            prerequisite = prerequisite,
            satisfied = present,
            message = if (present) {
                "${prerequisite.name} is available"
            } else {
                "${prerequisite.name} was not found on PATH"
            },
            remediation = if (present) null else prerequisite.remediation
        )
    }

    private fun evaluateEndpoint(prerequisite: PluginPrerequisite): PluginPrerequisiteResult {
        return try {
            val connection = URI.create(prerequisite.value).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 1500
            connection.readTimeout = 1500
            connection.requestMethod = "HEAD"
            connection.connect()
            val reachable = connection.responseCode in 200..399
            PluginPrerequisiteResult(
                prerequisite = prerequisite,
                satisfied = reachable,
                message = if (reachable) {
                    "${prerequisite.name} is reachable"
                } else {
                    "${prerequisite.name} responded with ${connection.responseCode}"
                },
                remediation = if (reachable) null else prerequisite.remediation
            )
        } catch (e: Exception) {
            PluginPrerequisiteResult(
                prerequisite = prerequisite,
                satisfied = false,
                message = e.message ?: "${prerequisite.name} is unreachable",
                remediation = prerequisite.remediation
            )
        }
    }

    private fun evaluateCredential(
        prerequisite: PluginPrerequisite,
        pluginConfiguration: Map<String, String>
    ): PluginPrerequisiteResult {
        val present = pluginConfiguration[prerequisite.value].orEmpty().isNotBlank() || environment[prerequisite.value].orEmpty().isNotBlank()
        return PluginPrerequisiteResult(
            prerequisite = prerequisite,
            satisfied = present,
            message = if (present) {
                "${prerequisite.name} is configured"
            } else {
                "${prerequisite.name} is missing"
            },
            remediation = if (present) null else prerequisite.remediation
        )
    }

    private suspend fun evaluateCompanionApp(prerequisite: PluginPrerequisite): PluginPrerequisiteResult {
        val installed = ideService.detectInstalledIDEs().any { ide ->
            ide.type.name.equals(prerequisite.value, ignoreCase = true) ||
                ide.type.name.replace('_', ' ').equals(prerequisite.value, ignoreCase = true)
        } || environment["COMPANION_APP_${prerequisite.value.uppercase()}"].orEmpty().isNotBlank()
        return PluginPrerequisiteResult(
            prerequisite = prerequisite,
            satisfied = installed,
            message = if (installed) {
                "${prerequisite.name} is available"
            } else {
                "${prerequisite.name} is not installed"
            },
            remediation = if (installed) null else prerequisite.remediation
        )
    }
}
