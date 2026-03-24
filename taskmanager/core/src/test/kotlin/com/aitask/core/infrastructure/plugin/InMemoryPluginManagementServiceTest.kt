package com.aitask.core.infrastructure.plugin

import com.aitask.core.data.repository.InMemoryActivityRepository
import com.aitask.core.domain.model.ActivityType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
}
