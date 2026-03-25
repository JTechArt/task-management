package com.aitask.core.domain.plugin

import com.aitask.core.domain.model.HealthState
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PluginFrameworkTest {

    @Test
    fun `initialize registers declared extension points for compatible plugin`() = runTest {
        val host = InMemoryPluginHost(PluginContractVersion(1, 2))
        val plugin = TestPluginDefinition(
            manifest = PluginManifest(
                id = "com.example.analytics",
                name = "Analytics",
                version = "1.0.0",
                requiredHostContractVersion = PluginContractVersion(1, 0),
                extensionPoints = listOf(
                    PluginExtensionPointDeclaration(
                        type = PluginExtensionPointType.UI_SURFACE,
                        id = "analytics-dashboard",
                        label = "Analytics Dashboard"
                    ),
                    PluginExtensionPointDeclaration(
                        type = PluginExtensionPointType.BACKGROUND_JOB,
                        id = "analytics-sync",
                        label = "Analytics Sync"
                    )
                )
            )
        )

        val compatibility = host.register(plugin)
        assertTrue(compatibility.compatible)

        val discoveries = host.discover()
        assertEquals(listOf(plugin.manifest), discoveries)

        val report = host.initialize(plugin.manifest.id)

        assertEquals(PluginLifecycleState.INITIALIZED, report.state)
        assertEquals(2, report.extensionPoints.size)
        assertEquals(
            listOf("analytics-dashboard", "analytics-sync"),
            host.registeredExtensions(plugin.manifest.id).map { it.id }
        )
        assertEquals(PluginLifecycleState.INITIALIZED, host.lifecycleState(plugin.manifest.id))
    }

    @Test
    fun `initializeAll continues when an optional plugin fails`() = runTest {
        val host = InMemoryPluginHost(PluginContractVersion(1, 0))
        val healthyPlugin = TestPluginDefinition(
            manifest = PluginManifest(
                id = "com.example.healthy",
                name = "Healthy",
                version = "1.0.0",
                requiredHostContractVersion = PluginContractVersion(1, 0)
            )
        )
        val failingPlugin = TestPluginDefinition(
            manifest = PluginManifest(
                id = "com.example.optional",
                name = "Optional",
                version = "1.0.0",
                requiredHostContractVersion = PluginContractVersion(1, 0),
                optional = true
            ),
            initializeAction = {
                error("simulated init failure")
            }
        )

        host.register(healthyPlugin)
        host.register(failingPlugin)

        val reports = host.initializeAll()

        assertEquals(2, reports.size)
        assertEquals(PluginLifecycleState.INITIALIZED, host.lifecycleState("com.example.healthy"))
        assertEquals(PluginLifecycleState.FAILED, host.lifecycleState("com.example.optional"))
        assertTrue(reports.any { it.pluginId == "com.example.optional" && it.diagnostics.isNotEmpty() })
        assertEquals(
            "plugin.lifecycle.failed",
            reports.first { it.pluginId == "com.example.optional" }.diagnostics.first().code
        )
    }

    @Test
    fun `incompatible contract blocks activation before plugin code runs`() = runTest {
        var initializeCalled = false
        val host = InMemoryPluginHost(PluginContractVersion(1, 1))
        val plugin = TestPluginDefinition(
            manifest = PluginManifest(
                id = "com.example.future",
                name = "Future Plugin",
                version = "2.0.0",
                requiredHostContractVersion = PluginContractVersion(2, 0)
            ),
            initializeAction = {
                initializeCalled = true
                PluginLifecycleReport(
                    pluginId = "com.example.future",
                    state = PluginLifecycleState.INITIALIZED
                )
            }
        )

        val compatibility = host.register(plugin)
        assertFalse(compatibility.compatible)

        val report = host.initialize(plugin.manifest.id)

        assertEquals(PluginLifecycleState.BLOCKED, report.state)
        assertFalse(initializeCalled)
        assertEquals("plugin.contract.incompatible", report.diagnostics.first().code)
        assertNotNull(report.diagnostics.first().remediation)
    }

    @Test
    fun `health failures surface actionable diagnostics`() = runTest {
        val host = InMemoryPluginHost(PluginContractVersion(1, 0))
        val plugin = TestPluginDefinition(
            manifest = PluginManifest(
                id = "com.example.health",
                name = "Health Check",
                version = "1.0.0",
                requiredHostContractVersion = PluginContractVersion(1, 0)
            ),
            healthAction = {
                error("missing companion service")
            }
        )

        host.register(plugin)

        val report = host.health(plugin.manifest.id)

        assertEquals(HealthState.FAILED, report.state)
        assertEquals("plugin.health.failed", report.diagnostics.first().code)
        assertTrue(report.diagnostics.first().remediation?.contains("retry") == true)
    }

    @Test
    fun `enable disable and remove update lifecycle state`() = runTest {
        val host = InMemoryPluginHost(PluginContractVersion(1, 0))
        val plugin = TestPluginDefinition(
            manifest = PluginManifest(
                id = "com.example.toggle",
                name = "Toggle Plugin",
                version = "1.0.0",
                requiredHostContractVersion = PluginContractVersion(1, 0),
                extensionPoints = listOf(
                    PluginExtensionPointDeclaration(
                        type = PluginExtensionPointType.CONFIGURATION_SECTION,
                        id = "toggle-settings",
                        label = "Toggle Settings"
                    )
                )
            )
        )

        host.register(plugin)

        assertEquals(PluginLifecycleState.INITIALIZED, host.initialize(plugin.manifest.id).state)
        assertEquals(PluginLifecycleState.ENABLED, host.enable(plugin.manifest.id).state)
        assertEquals(PluginLifecycleState.DISABLED, host.disable(plugin.manifest.id).state)
        assertEquals(PluginLifecycleState.REMOVED, host.remove(plugin.manifest.id).state)
        assertFalse(host.discover().any { it.id == plugin.manifest.id })
        assertTrue(host.registeredExtensions(plugin.manifest.id).isEmpty())
    }
}

private class TestPluginDefinition(
    override val manifest: PluginManifest,
    private val initializeAction: suspend (PluginHostContext) -> PluginLifecycleReport = { _ ->
        PluginLifecycleReport(
            pluginId = manifest.id,
            state = PluginLifecycleState.INITIALIZED,
            extensionPoints = manifest.extensionPoints
        )
    },
    private val enableAction: suspend (PluginHostContext) -> PluginLifecycleReport = { context ->
        PluginLifecycleReport(
            pluginId = manifest.id,
            state = PluginLifecycleState.ENABLED,
            extensionPoints = context.extensionRegistry.findByPluginId(manifest.id)
        )
    },
    private val disableAction: suspend (PluginHostContext) -> PluginLifecycleReport = {
        PluginLifecycleReport(
            pluginId = manifest.id,
            state = PluginLifecycleState.DISABLED
        )
    },
    private val removeAction: suspend (PluginHostContext) -> PluginLifecycleReport = {
        PluginLifecycleReport(
            pluginId = manifest.id,
            state = PluginLifecycleState.REMOVED
        )
    },
    private val healthAction: suspend (PluginHostContext) -> PluginHealthReport = {
        PluginHealthReport(
            pluginId = manifest.id,
            state = HealthState.HEALTHY,
            message = "Healthy"
        )
    }
) : PluginDefinition {
    override suspend fun initialize(context: PluginHostContext): PluginLifecycleReport = initializeAction(context)

    override suspend fun enable(context: PluginHostContext): PluginLifecycleReport = enableAction(context)

    override suspend fun disable(context: PluginHostContext): PluginLifecycleReport = disableAction(context)

    override suspend fun remove(context: PluginHostContext): PluginLifecycleReport = removeAction(context)

    override suspend fun health(context: PluginHostContext): PluginHealthReport = healthAction(context)
}
