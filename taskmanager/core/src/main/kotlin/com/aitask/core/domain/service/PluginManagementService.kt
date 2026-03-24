package com.aitask.core.domain.service

import com.aitask.core.domain.plugin.PluginCatalogItem

interface PluginManagementService {
    suspend fun catalog(): List<PluginCatalogItem>
    suspend fun install(pluginId: String): Result<PluginCatalogItem>
    suspend fun attach(pluginId: String): Result<PluginCatalogItem>
    suspend fun enable(pluginId: String): Result<PluginCatalogItem>
    suspend fun disable(pluginId: String): Result<PluginCatalogItem>
    suspend fun detach(pluginId: String): Result<PluginCatalogItem>
    suspend fun remove(pluginId: String): Result<PluginCatalogItem>
}
