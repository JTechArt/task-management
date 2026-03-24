package com.aitask.core.data.repository

import com.aitask.core.domain.plugin.PluginConfigurationScope
import com.aitask.core.domain.plugin.PluginConfigurationSnapshot
import com.aitask.core.domain.repository.PluginConfigurationRepository

class InMemoryPluginConfigurationRepository : PluginConfigurationRepository {
    private data class Key(
        val pluginId: String,
        val scope: PluginConfigurationScope,
        val scopeKey: String?
    )

    private val snapshots = mutableMapOf<Key, PluginConfigurationSnapshot>()

    override suspend fun find(
        pluginId: String,
        scope: PluginConfigurationScope,
        scopeKey: String?
    ): PluginConfigurationSnapshot? = snapshots[Key(pluginId, scope, scopeKey)]

    override suspend fun findByPluginId(pluginId: String): List<PluginConfigurationSnapshot> =
        snapshots.values.filter { it.pluginId == pluginId }

    override suspend fun save(snapshot: PluginConfigurationSnapshot): PluginConfigurationSnapshot {
        snapshots[Key(snapshot.pluginId, snapshot.scope, snapshot.scopeKey)] = snapshot
        return snapshot
    }
}
