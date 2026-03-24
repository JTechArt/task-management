package com.aitask.core.infrastructure.plugin

import com.aitask.core.data.repository.InMemoryActivityRepository
import com.aitask.core.data.repository.InMemoryPluginConfigurationRepository
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.plugin.PluginConfigurationScope
import com.aitask.core.domain.plugin.PluginConfigurationSnapshot
import com.aitask.core.domain.plugin.PluginConfigurationValidationStatus
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class InMemoryPluginManagementServiceTest {

    @Test
    fun `catalog exposes installed and available plugins`() = runTest {
        val service = InMemoryPluginManagementService(activityRepository = InMemoryActivityRepository())

        val catalog = service.catalog()

        assertEquals(3, catalog.size)
        assertTrue(catalog.any { it.id == "plugin.code-review-assistant" && it.installed })
        assertTrue(catalog.any { it.id == "plugin.release-notes" && !it.installed })
    }

    @Test
    fun `plugin lifecycle actions update status and record history`() = runTest {
        val activityRepository = InMemoryActivityRepository()
        val service = InMemoryPluginManagementService(activityRepository = activityRepository)
        val pluginId = "plugin.release-notes"
        val entityId = UUID.nameUUIDFromBytes(pluginId.toByteArray())

        assertTrue(service.install(pluginId).isSuccess)
        assertTrue(service.attach(pluginId).isSuccess)
        assertTrue(
            service.saveConfiguration(
                PluginConfigurationSnapshot(
                    pluginId = pluginId,
                    scope = PluginConfigurationScope.APP,
                    fields = mapOf("release_prefix" to "Release"),
                    secrets = mapOf("release_token" to "secret-token"),
                    validationStatus = PluginConfigurationValidationStatus.VALID
                )
            ).isSuccess
        )
        assertTrue(service.enable(pluginId).isSuccess)
        assertTrue(service.disable(pluginId).isSuccess)
        assertTrue(service.detach(pluginId).isSuccess)
        assertTrue(service.remove(pluginId).isSuccess)

        val catalog = service.catalog()
        val item = catalog.first { it.id == pluginId }
        assertFalse(item.installed)
        assertEquals("Available", item.statusLabel)

        val activities = activityRepository.findByEntity("plugin", entityId)
        assertEquals(
            listOf(
                ActivityType.PLUGIN_REMOVED,
                ActivityType.PLUGIN_DETACHED,
                ActivityType.PLUGIN_DISABLED,
                ActivityType.PLUGIN_ENABLED,
                ActivityType.PLUGIN_ATTACHED,
                ActivityType.PLUGIN_INSTALLED
            ),
            activities.map { it.type }
        )
    }

    @Test
    fun `unsupported plugin actions fail cleanly`() = runTest {
        val activityRepository = InMemoryActivityRepository()
        val service = InMemoryPluginManagementService(activityRepository = activityRepository)

        val result = service.attach("plugin.does-not-exist")

        assertTrue(result.isFailure)
        val activities = activityRepository.findByType(ActivityType.PLUGIN_ATTACHED)
        assertEquals(1, activities.size)
        assertEquals("FAILED", activities.first().status.name)
    }

    @Test
    fun `invalid configuration does not replace the last valid snapshot`() = runTest {
        val configRepository = InMemoryPluginConfigurationRepository()
        val service = InMemoryPluginManagementService(
            activityRepository = InMemoryActivityRepository(),
            configurationRepository = configRepository
        )

        val validSnapshot = PluginConfigurationSnapshot(
            pluginId = "plugin.code-review-assistant",
            scope = PluginConfigurationScope.APP,
            fields = mapOf("review_summary_prefix" to "Review"),
            secrets = mapOf("review_token" to "secret-token"),
            validationStatus = PluginConfigurationValidationStatus.VALID
        )

        assertTrue(service.saveConfiguration(validSnapshot).isSuccess)

        val invalidResult = service.saveConfiguration(
            validSnapshot.copy(fields = emptyMap())
        )

        assertTrue(invalidResult.isFailure)
        val persisted = service.getConfiguration(
            pluginId = "plugin.code-review-assistant",
            scope = PluginConfigurationScope.APP
        )
        assertNotNull(persisted)
        assertEquals("Review", persisted?.fields?.get("review_summary_prefix"))
        assertEquals("secret-token", persisted?.secrets?.get("review_token"))
    }

    @Test
    fun `validation surfaces missing prerequisites before enablement`() = runTest {
        val configRepository = InMemoryPluginConfigurationRepository()
        val serviceWithConfig = InMemoryPluginManagementService(
            activityRepository = InMemoryActivityRepository(),
            configurationRepository = configRepository
        )

        val pluginId = "plugin.code-review-assistant"
        assertTrue(
            serviceWithConfig.saveConfiguration(
                PluginConfigurationSnapshot(
                    pluginId = pluginId,
                    scope = PluginConfigurationScope.APP,
                    fields = mapOf("review_summary_prefix" to "Review"),
                    secrets = mapOf("review_token" to "secret-token"),
                    validationStatus = PluginConfigurationValidationStatus.VALID
                )
            ).isSuccess
        )

        val failingService = InMemoryPluginManagementService(
            activityRepository = InMemoryActivityRepository(),
            configurationRepository = configRepository,
            prerequisiteProbe = object : com.aitask.core.domain.service.PluginPrerequisiteProbe {
                override suspend fun evaluate(
                    prerequisite: com.aitask.core.domain.plugin.PluginPrerequisite,
                    pluginConfiguration: Map<String, String>
                ) = com.aitask.core.domain.plugin.PluginPrerequisiteResult(
                    prerequisite = prerequisite,
                    satisfied = false,
                    message = "${prerequisite.name} is missing",
                    remediation = prerequisite.remediation
                )
            }
        )

        assertTrue(failingService.install(pluginId).isSuccess)
        assertTrue(failingService.attach(pluginId).isSuccess)

        val validation = failingService.validateConfiguration(
            pluginId = pluginId,
            scope = PluginConfigurationScope.APP
        ).getOrNull()

        assertNotNull(validation)
        assertTrue(validation?.valid == false)
        assertTrue(validation?.prerequisiteResults?.isNotEmpty() == true)

        val enableResult = failingService.enable(pluginId)
        assertTrue(enableResult.isFailure)
    }
}
