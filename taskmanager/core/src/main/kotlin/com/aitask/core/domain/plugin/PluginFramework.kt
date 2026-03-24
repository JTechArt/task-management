package com.aitask.core.domain.plugin

import com.aitask.core.domain.model.HealthState
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class PluginContractVersion(
    val major: Int,
    val minor: Int
) {
    init {
        require(major >= 0) { "major must be non-negative" }
        require(minor >= 0) { "minor must be non-negative" }
    }

    fun supports(requiredVersion: PluginContractVersion): Boolean =
        major == requiredVersion.major && minor >= requiredVersion.minor
}

@Serializable
enum class PluginExtensionPointType {
    UI_SURFACE,
    BACKGROUND_JOB,
    CONFIGURATION_SECTION,
    INTEGRATION_HOOK
}

@Serializable
data class PluginExtensionPointDeclaration(
    val type: PluginExtensionPointType,
    val id: String,
    val label: String,
    val description: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "extension point id must not be blank" }
        require(label.isNotBlank()) { "extension point label must not be blank" }
    }
}

@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val requiredHostContractVersion: PluginContractVersion,
    val optional: Boolean = true,
    val extensionPoints: List<PluginExtensionPointDeclaration> = emptyList()
) {
    init {
        require(id.isNotBlank()) { "plugin id must not be blank" }
        require(name.isNotBlank()) { "plugin name must not be blank" }
        require(version.isNotBlank()) { "plugin version must not be blank" }
    }
}

@Serializable
data class PluginDiagnostic(
    val code: String,
    val message: String,
    val detail: String? = null,
    val remediation: String? = null
) {
    init {
        require(code.isNotBlank()) { "diagnostic code must not be blank" }
        require(message.isNotBlank()) { "diagnostic message must not be blank" }
    }
}

enum class PluginLifecycleState {
    DISCOVERED,
    INITIALIZED,
    ENABLED,
    DISABLED,
    REMOVED,
    FAILED,
    BLOCKED
}

data class PluginCompatibilityReport(
    val pluginId: String,
    val hostContractVersion: PluginContractVersion,
    val requiredHostContractVersion: PluginContractVersion,
    val compatible: Boolean,
    val diagnostics: List<PluginDiagnostic> = emptyList()
)

data class PluginLifecycleReport(
    val pluginId: String,
    val state: PluginLifecycleState,
    val diagnostics: List<PluginDiagnostic> = emptyList(),
    val extensionPoints: List<PluginExtensionPointDeclaration> = emptyList(),
    val completedAt: Instant = Instant.now()
)

data class PluginHealthReport(
    val pluginId: String,
    val state: HealthState,
    val message: String,
    val detail: String? = null,
    val diagnostics: List<PluginDiagnostic> = emptyList(),
    val checkedAt: Instant = Instant.now()
)

data class RegisteredPluginExtension(
    val pluginId: String,
    val declaration: PluginExtensionPointDeclaration
)

interface PluginExtensionRegistry {
    fun register(pluginId: String, declaration: PluginExtensionPointDeclaration)
    fun registerAll(pluginId: String, declarations: List<PluginExtensionPointDeclaration>) {
        declarations.forEach { register(pluginId, it) }
    }

    fun clear(pluginId: String)

    fun findByPluginId(pluginId: String): List<PluginExtensionPointDeclaration>

    fun all(): List<RegisteredPluginExtension>
}

class InMemoryPluginExtensionRegistry : PluginExtensionRegistry {
    private val extensions = mutableListOf<RegisteredPluginExtension>()

    override fun register(pluginId: String, declaration: PluginExtensionPointDeclaration) {
        extensions.add(RegisteredPluginExtension(pluginId, declaration))
    }

    override fun clear(pluginId: String) {
        extensions.removeAll { it.pluginId == pluginId }
    }

    override fun findByPluginId(pluginId: String): List<PluginExtensionPointDeclaration> =
        extensions.filter { it.pluginId == pluginId }.map { it.declaration }

    override fun all(): List<RegisteredPluginExtension> = extensions.toList()
}

data class PluginHostContext(
    val hostContractVersion: PluginContractVersion,
    val extensionRegistry: PluginExtensionRegistry
)

interface PluginDefinition {
    val manifest: PluginManifest

    suspend fun initialize(context: PluginHostContext): PluginLifecycleReport

    suspend fun enable(context: PluginHostContext): PluginLifecycleReport

    suspend fun disable(context: PluginHostContext): PluginLifecycleReport

    suspend fun remove(context: PluginHostContext): PluginLifecycleReport

    suspend fun health(context: PluginHostContext): PluginHealthReport
}

interface PluginHost {
    val hostContractVersion: PluginContractVersion

    fun register(plugin: PluginDefinition): PluginCompatibilityReport

    fun discover(): List<PluginManifest>

    suspend fun initialize(pluginId: String): PluginLifecycleReport

    suspend fun enable(pluginId: String): PluginLifecycleReport

    suspend fun disable(pluginId: String): PluginLifecycleReport

    suspend fun remove(pluginId: String): PluginLifecycleReport

    suspend fun health(pluginId: String): PluginHealthReport

    suspend fun initializeAll(): List<PluginLifecycleReport>

    fun lifecycleState(pluginId: String): PluginLifecycleState?

    fun compatibilityReport(pluginId: String): PluginCompatibilityReport?

    fun registeredExtensions(pluginId: String): List<PluginExtensionPointDeclaration>
}

class InMemoryPluginHost(
    override val hostContractVersion: PluginContractVersion
) : PluginHost {
    private data class ManagedPlugin(
        val plugin: PluginDefinition,
        val compatibilityReport: PluginCompatibilityReport,
        var lifecycleState: PluginLifecycleState = PluginLifecycleState.DISCOVERED,
        var lastLifecycleReport: PluginLifecycleReport? = null,
        var lastHealthReport: PluginHealthReport? = null
    )

    private val plugins = ConcurrentHashMap<String, ManagedPlugin>()
    private val extensionRegistry = InMemoryPluginExtensionRegistry()

    override fun register(plugin: PluginDefinition): PluginCompatibilityReport {
        val manifest = plugin.manifest
        require(!plugins.containsKey(manifest.id)) { "Plugin '${manifest.id}' is already registered" }
        val compatibilityReport = buildCompatibilityReport(manifest)
        plugins[manifest.id] = ManagedPlugin(plugin = plugin, compatibilityReport = compatibilityReport)
        return compatibilityReport
    }

    override fun discover(): List<PluginManifest> =
        plugins.values.map { it.plugin.manifest }.sortedBy { it.id }

    override suspend fun initialize(pluginId: String): PluginLifecycleReport =
        activate(pluginId) { plugin, context ->
            plugin.initialize(context)
        }

    override suspend fun enable(pluginId: String): PluginLifecycleReport =
        activate(pluginId) { plugin, context ->
            plugin.enable(context)
        }

    override suspend fun disable(pluginId: String): PluginLifecycleReport =
        activate(pluginId) { plugin, context ->
            plugin.disable(context)
        }

    override suspend fun remove(pluginId: String): PluginLifecycleReport =
        activate(pluginId) { plugin, context ->
            plugin.remove(context)
        }.also { report ->
            if (report.state == PluginLifecycleState.REMOVED) {
                extensionRegistry.clear(pluginId)
                plugins.remove(pluginId)
            }
        }

    override suspend fun health(pluginId: String): PluginHealthReport {
        val managedPlugin = plugin(pluginId)
        val context = context()
        val report = runCatching { managedPlugin.plugin.health(context) }
            .getOrElse { throwable ->
                PluginHealthReport(
                    pluginId = pluginId,
                    state = HealthState.FAILED,
                    message = "Health check failed",
                    detail = throwable.message ?: "Unknown plugin health failure",
                    diagnostics = listOf(
                        PluginDiagnostic(
                            code = "plugin.health.failed",
                            message = "Plugin health check threw an exception",
                            detail = throwable.message,
                            remediation = "Inspect the plugin's runtime dependencies and retry the health check."
                        )
                    )
                )
            }
        managedPlugin.lastHealthReport = report
        return report
    }

    override suspend fun initializeAll(): List<PluginLifecycleReport> =
        plugins.keys.sorted().map { initialize(it) }

    override fun lifecycleState(pluginId: String): PluginLifecycleState? = plugins[pluginId]?.lifecycleState

    override fun compatibilityReport(pluginId: String): PluginCompatibilityReport? = plugins[pluginId]?.compatibilityReport

    override fun registeredExtensions(pluginId: String): List<PluginExtensionPointDeclaration> =
        extensionRegistry.findByPluginId(pluginId)

    private suspend fun activate(
        pluginId: String,
        invocation: suspend (PluginDefinition, PluginHostContext) -> PluginLifecycleReport
    ): PluginLifecycleReport {
        val managedPlugin = plugin(pluginId)
        if (!managedPlugin.compatibilityReport.compatible) {
            val report = PluginLifecycleReport(
                pluginId = pluginId,
                state = PluginLifecycleState.BLOCKED,
                diagnostics = managedPlugin.compatibilityReport.diagnostics.ifEmpty {
                    listOf(
                        PluginDiagnostic(
                            code = "plugin.contract.incompatible",
                            message = "Plugin requires a newer or incompatible contract version",
                            detail = "Host=${hostContractVersion.major}.${hostContractVersion.minor}, " +
                                "Required=${managedPlugin.compatibilityReport.requiredHostContractVersion.major}.${managedPlugin.compatibilityReport.requiredHostContractVersion.minor}",
                            remediation = "Upgrade the host or install a plugin built for this contract version."
                        )
                    )
                }
            )
            managedPlugin.lifecycleState = report.state
            managedPlugin.lastLifecycleReport = report
            return report
        }

        val context = context()
        val report = runCatching { invocation(managedPlugin.plugin, context) }
            .getOrElse { throwable ->
                PluginLifecycleReport(
                    pluginId = pluginId,
                    state = PluginLifecycleState.FAILED,
                    diagnostics = listOf(
                        PluginDiagnostic(
                            code = "plugin.lifecycle.failed",
                            message = "Plugin lifecycle step failed",
                            detail = throwable.message,
                            remediation = "Review the plugin logs and retry the lifecycle action after fixing the failure."
                        )
                    )
                )
            }

        val normalizedReport = if (report.extensionPoints.isEmpty() && report.state == PluginLifecycleState.INITIALIZED) {
            report.copy(extensionPoints = managedPlugin.plugin.manifest.extensionPoints)
        } else {
            report
        }

        if (normalizedReport.state == PluginLifecycleState.INITIALIZED) {
            context.extensionRegistry.clear(pluginId)
            normalizedReport.extensionPoints.forEach { context.extensionRegistry.register(pluginId, it) }
        }

        managedPlugin.lifecycleState = normalizedReport.state
        managedPlugin.lastLifecycleReport = normalizedReport
        return normalizedReport
    }

    private fun plugin(pluginId: String): ManagedPlugin =
        plugins[pluginId] ?: error("Unknown plugin '$pluginId'")

    private fun context(): PluginHostContext =
        PluginHostContext(
            hostContractVersion = hostContractVersion,
            extensionRegistry = extensionRegistry
        )

    private fun buildCompatibilityReport(manifest: PluginManifest): PluginCompatibilityReport {
        val compatible = hostContractVersion.supports(manifest.requiredHostContractVersion)
        val diagnostics = if (compatible) {
            emptyList()
        } else {
            listOf(
                PluginDiagnostic(
                    code = "plugin.contract.incompatible",
                    message = "Plugin contract version is not compatible with the host",
                    detail = "Host=${hostContractVersion.major}.${hostContractVersion.minor}, " +
                        "Required=${manifest.requiredHostContractVersion.major}.${manifest.requiredHostContractVersion.minor}",
                    remediation = "Upgrade the host or use a plugin compiled for the current contract version."
                )
            )
        }
        return PluginCompatibilityReport(
            pluginId = manifest.id,
            hostContractVersion = hostContractVersion,
            requiredHostContractVersion = manifest.requiredHostContractVersion,
            compatible = compatible,
            diagnostics = diagnostics
        )
    }
}
