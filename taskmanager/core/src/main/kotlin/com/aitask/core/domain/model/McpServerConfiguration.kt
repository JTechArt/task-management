package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

data class McpServerConfigurationRequest(
    val id: UUID? = null,
    val isEnabled: Boolean = false,
    val port: Int = 3333
) {
    init {
        require(port in 1..65535) { "MCP server port must be between 1 and 65535" }
    }
}

data class McpServerConfiguration(
    val id: UUID,
    val isEnabled: Boolean,
    val port: Int,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(port in 1..65535) { "MCP server port must be between 1 and 65535" }
    }

    val host: String = "127.0.0.1"
    val endpointUrl: String = "http://$host:$port/mcp"
}

data class McpServerStatus(
    val isEnabled: Boolean,
    val isRunning: Boolean,
    val host: String,
    val port: Int,
    val endpointUrl: String,
    val message: String? = null
)
