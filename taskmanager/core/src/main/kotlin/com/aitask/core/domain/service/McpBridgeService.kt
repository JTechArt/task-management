package com.aitask.core.domain.service

import kotlinx.serialization.Serializable

@Serializable
data class McpToolDescriptor(
    val name: String,
    val description: String
)

@Serializable
data class McpResourceDescriptor(
    val uri: String,
    val name: String,
    val description: String
)

@Serializable
data class McpServerManifest(
    val serverName: String,
    val endpointUrl: String,
    val capabilities: List<String>,
    val tools: List<McpToolDescriptor>,
    val resources: List<McpResourceDescriptor>,
    val safetyNotes: List<String>
)

interface McpBridgeService {
    suspend fun sync(): McpServerManifest
    suspend fun stop(): McpServerManifest
    fun currentManifest(): McpServerManifest
}
