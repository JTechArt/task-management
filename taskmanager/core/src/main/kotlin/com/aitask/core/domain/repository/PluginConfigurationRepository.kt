package com.aitask.core.domain.repository

import com.aitask.core.domain.plugin.PluginConfigurationScope
import com.aitask.core.domain.plugin.PluginConfigurationSnapshot

interface PluginConfigurationRepository {
    suspend fun find(pluginId: String, scope: PluginConfigurationScope, scopeKey: String? = null): PluginConfigurationSnapshot?
    suspend fun findByPluginId(pluginId: String): List<PluginConfigurationSnapshot>
    suspend fun save(snapshot: PluginConfigurationSnapshot): PluginConfigurationSnapshot
}
