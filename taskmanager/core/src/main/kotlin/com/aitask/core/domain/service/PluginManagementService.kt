package com.aitask.core.domain.service

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.plugin.*

interface PluginManagementService {
    suspend fun catalog(): List<PluginCatalogItem>
    suspend fun install(pluginId: String): Result<PluginCatalogItem>
    suspend fun attach(pluginId: String): Result<PluginCatalogItem>
    suspend fun enable(pluginId: String): Result<PluginCatalogItem>
    suspend fun disable(pluginId: String): Result<PluginCatalogItem>
    suspend fun detach(pluginId: String): Result<PluginCatalogItem>
    suspend fun remove(pluginId: String): Result<PluginCatalogItem>
    suspend fun getConfiguration(pluginId: String, scope: PluginConfigurationScope, scopeKey: String? = null): PluginConfigurationSnapshot?
    suspend fun saveConfiguration(snapshot: PluginConfigurationSnapshot): Result<PluginConfigurationSnapshot>
    suspend fun validateConfiguration(snapshot: PluginConfigurationSnapshot): Result<PluginConfigurationValidationResult>
    suspend fun validateConfiguration(pluginId: String, scope: PluginConfigurationScope, scopeKey: String? = null): Result<PluginConfigurationValidationResult>
    suspend fun health(pluginId: String): Result<PluginHealthReport>
    suspend fun recentActivity(pluginId: String, limit: Int = 5): List<Activity>
}
