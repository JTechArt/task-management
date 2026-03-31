package com.aitask.core.domain.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PluginConfigurationSnapshotSupportTest {

    @Test
    fun `split separates schema fields from internal persisted keys`() {
        val plugin = PluginCatalogItem(
            id = "test.plugin",
            name = "Test",
            description = "Test",
            version = "1",
            configurationScope = PluginConfigurationScope.APP,
            configurationSchema = PluginConfigurationSchema(
                fields = listOf(
                    PluginConfigurationField(
                        id = "f1",
                        label = "F1",
                        type = PluginConfigurationFieldType.TEXT,
                        required = true
                    ),
                    PluginConfigurationField(
                        id = "f2",
                        label = "F2",
                        type = PluginConfigurationFieldType.TEXT,
                        required = true
                    )
                )
            ),
            optional = true,
            installed = true,
            attached = false,
            enabled = false,
            lifecycleState = PluginLifecycleState.ENABLED
        )
        val split = splitPluginConfigurationFields(
            plugin = plugin,
            snapshotFields = mapOf(
                "f1" to "a",
                "f2" to "b",
                SlackChannelAnalyzerPluginIds.FIELD_CHANNEL_CHECKPOINTS_JSON to """{"channels":{"C1":"99.0"}}"""
            )
        )
        assertEquals(mapOf("f1" to "a", "f2" to "b"), split.visible)
        assertEquals(
            mapOf(SlackChannelAnalyzerPluginIds.FIELD_CHANNEL_CHECKPOINTS_JSON to """{"channels":{"C1":"99.0"}}"""),
            split.internal
        )
        assertEquals(
            mapOf(
                "f1" to "a",
                "f2" to "b",
                SlackChannelAnalyzerPluginIds.FIELD_CHANNEL_CHECKPOINTS_JSON to """{"channels":{"C1":"99.0"}}"""
            ),
            mergePluginConfigurationFields(visible = split.visible, internal = split.internal)
        )
    }
}
