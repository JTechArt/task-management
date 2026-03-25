package com.aitask.core.infrastructure.mcp

import com.aitask.core.domain.model.McpServerConfiguration
import com.aitask.core.domain.repository.McpServerConfigurationRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.McpBridgeService
import com.aitask.core.domain.service.McpResourceDescriptor
import com.aitask.core.domain.service.McpServerManifest
import com.aitask.core.domain.service.McpToolDescriptor
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

private const val MCP_HOST = "127.0.0.1"
private const val MCP_PATH = "/mcp"

private val mcpJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

@Serializable
private data class McpJsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: JsonElement? = null,
    val method: String,
    val params: JsonObject? = null
)

@Serializable
private data class McpJsonRpcError(
    val code: Int,
    val message: String
)

@Serializable
private data class McpJsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: JsonElement? = null,
    val result: JsonElement? = null,
    val error: McpJsonRpcError? = null
)

@Serializable
private data class McpContextSnapshot(
    val generatedAt: String,
    val projectCount: Int,
    val taskCount: Int,
    val repositoryCount: Int,
    val projects: List<McpProjectContext>,
    val tasks: List<McpTaskContext>,
    val repositories: List<McpRepositoryContext>,
    val safetyNotes: List<String>
)

@Serializable
private data class McpProjectContext(
    val id: String,
    val name: String,
    val description: String?,
    val workspacePath: String,
    val methodology: String,
    val archived: Boolean
)

@Serializable
private data class McpTaskContext(
    val id: String,
    val title: String,
    val description: String?,
    val status: String,
    val projectId: String,
    val projectName: String,
    val workspacePath: String?,
    val branchName: String?
)

@Serializable
private data class McpRepositoryContext(
    val id: String,
    val projectId: String,
    val projectName: String,
    val name: String,
    val provider: String,
    val workspacePath: String
)

class DefaultMcpBridgeService(
    private val settingsRepository: McpServerConfigurationRepository,
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val repositoryRepository: RepositoryRepository
) : McpBridgeService {
    private val mutex = Mutex()
    @Volatile private var server: ApplicationEngine? = null
    @Volatile private var manifest: McpServerManifest = buildDisabledManifest("MCP bridge disabled")

    override suspend fun sync(): McpServerManifest = mutex.withLock {
        val configuration = settingsRepository.find()
        if (configuration == null || !configuration.isEnabled) {
            stopServerLocked("MCP bridge disabled")
            return manifest
        }

        if (server != null && manifest.endpointUrl == configuration.endpointUrl) {
            manifest = buildManifest(configuration, "MCP bridge running")
            return manifest
        }

        stopServerLocked("MCP bridge restarting")
        startServerLocked(configuration)
        manifest
    }

    override suspend fun stop(): McpServerManifest = mutex.withLock {
        stopServerLocked("MCP bridge stopped")
        manifest
    }

    override fun currentManifest(): McpServerManifest = manifest

    private fun startServerLocked(configuration: McpServerConfiguration) {
        try {
            val engine = embeddedServer(Netty, host = MCP_HOST, port = configuration.port) {
                install(ContentNegotiation) {
                    json(mcpJson)
                }
                routing {
                    get("$MCP_PATH/health") {
                        call.respond(
                            mapOf(
                                "enabled" to true,
                                "running" to true,
                                "endpointUrl" to configuration.endpointUrl
                            )
                        )
                    }
                    get("$MCP_PATH/manifest") {
                        call.respond(buildManifest(configuration, "MCP bridge running"))
                    }
                    get("$MCP_PATH/context") {
                        val projectId = call.request.queryParameters["projectId"]?.let { UUID.fromString(it) }
                        val taskId = call.request.queryParameters["taskId"]?.let { UUID.fromString(it) }
                        call.respond(buildContextSnapshot(projectId, taskId))
                    }
                    post(MCP_PATH) {
                        val request = runCatching {
                            mcpJson.decodeFromString(McpJsonRpcRequest.serializer(), call.receiveText())
                        }.getOrElse {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                McpJsonRpcResponse(
                                    id = null,
                                    error = McpJsonRpcError(-32700, "Invalid JSON-RPC payload")
                                )
                            )
                            return@post
                        }
                        call.respondText(
                            mcpJson.encodeToString(
                                McpJsonRpcResponse.serializer(),
                                handleJsonRpc(request, configuration)
                            ),
                            contentType = ContentType.Application.Json
                        )
                    }
                }
            }
            engine.start(wait = false)
            server = engine
            manifest = buildManifest(configuration, "MCP bridge running")
        } catch (error: Exception) {
            manifest = buildManifest(configuration, "MCP bridge failed: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun stopServerLocked(message: String) {
        server?.stop(1000, 2000)
        server = null
        manifest = buildDisabledManifest(message)
    }

    private fun buildDisabledManifest(message: String): McpServerManifest = McpServerManifest(
        serverName = "AiTask MCP bridge",
        endpointUrl = "http://$MCP_HOST:3333$MCP_PATH",
        capabilities = listOf("task-context", "project-context", "repository-context"),
        tools = listOf(
            McpToolDescriptor("get_context", "Return sanitized task, project, and repository context"),
            McpToolDescriptor("get_task", "Return a single task and its related project context"),
            McpToolDescriptor("get_project", "Return a single project and its repositories")
        ),
        resources = listOf(
            McpResourceDescriptor("aitask://context", "Workspace context", "Sanitized context snapshot with no secrets"),
            McpResourceDescriptor("aitask://projects", "Projects", "Project summaries and workspace paths"),
            McpResourceDescriptor("aitask://tasks", "Tasks", "Task summaries with branch and workspace context")
        ),
        safetyNotes = listOf(
            message,
            "Only local loopback access is supported.",
            "No credential or API key fields are exposed."
        )
    )

    private fun buildManifest(configuration: McpServerConfiguration, message: String): McpServerManifest = McpServerManifest(
        serverName = "AiTask MCP bridge",
        endpointUrl = configuration.endpointUrl,
        capabilities = listOf("task-context", "project-context", "repository-context"),
        tools = listOf(
            McpToolDescriptor("get_context", "Return sanitized task, project, and repository context"),
            McpToolDescriptor("get_task", "Return a single task and its related project context"),
            McpToolDescriptor("get_project", "Return a single project and its repositories")
        ),
        resources = listOf(
            McpResourceDescriptor("aitask://context", "Workspace context", "Sanitized context snapshot with no secrets"),
            McpResourceDescriptor("aitask://projects", "Projects", "Project summaries and workspace paths"),
            McpResourceDescriptor("aitask://tasks", "Tasks", "Task summaries with branch and workspace context")
        ),
        safetyNotes = listOf(
            message,
            "Only local loopback access is supported.",
            "No credential or API key fields are exposed."
        )
    )

    private suspend fun buildContextSnapshot(projectId: UUID? = null, taskId: UUID? = null): McpContextSnapshot {
        val projects = when {
            taskId != null -> {
                val task = taskRepository.findById(taskId)
                if (task == null) emptyList() else projectRepository.findById(task.projectId)?.let { listOf(it) } ?: emptyList()
            }
            projectId != null -> projectRepository.findById(projectId)?.let { listOf(it) } ?: emptyList()
            else -> projectRepository.findAllActive()
        }

        val projectIds = projects.map { it.id }.toSet()
        val tasks = when {
            taskId != null -> taskRepository.findById(taskId)?.let { listOf(it) } ?: emptyList()
            projectId != null -> taskRepository.findByProject(projectId)
            else -> projectRepository.findAllActive().flatMap { taskRepository.findByProject(it.id) }
        }

        val repositories = when {
            taskId != null -> {
                val task = taskRepository.findById(taskId)
                if (task == null) emptyList() else repositoryRepository.findByProject(task.projectId)
            }
            projectId != null -> repositoryRepository.findByProject(projectId)
            else -> projectIds.flatMap { repositoryRepository.findByProject(it) }
        }

        val projectLookup = projects.associateBy { it.id }

        return McpContextSnapshot(
            generatedAt = java.time.Instant.now().toString(),
            projectCount = projects.size,
            taskCount = tasks.size,
            repositoryCount = repositories.size,
            projects = projects.map {
                McpProjectContext(
                    id = it.id.toString(),
                    name = it.name,
                    description = it.description,
                    workspacePath = it.workspacePath,
                    methodology = it.methodology.name,
                    archived = it.isArchived
                )
            },
            tasks = tasks.map {
                McpTaskContext(
                    id = it.id.toString(),
                    title = it.title,
                    description = it.description,
                    status = it.status.name,
                    projectId = it.projectId.toString(),
                    projectName = projectLookup[it.projectId]?.name ?: "Unknown project",
                    workspacePath = it.workspacePath,
                    branchName = it.branchName
                )
            },
            repositories = repositories.map {
                McpRepositoryContext(
                    id = it.id.toString(),
                    projectId = it.projectId.toString(),
                    projectName = projectLookup[it.projectId]?.name ?: "Unknown project",
                    name = it.name,
                    provider = it.provider.name,
                    workspacePath = projectLookup[it.projectId]?.workspacePath ?: ""
                )
            },
            safetyNotes = listOf(
                "Credentials, clone URLs, and API keys are intentionally omitted.",
                "The bridge is bound to localhost only."
            )
        )
    }

    private suspend fun handleJsonRpc(
        request: McpJsonRpcRequest,
        configuration: McpServerConfiguration
    ): McpJsonRpcResponse {
        fun success(result: JsonElement) = McpJsonRpcResponse(id = request.id, result = result)
        return when (request.method) {
            "initialize" -> success(
                mcpJson.encodeToJsonElement(
                    mapOf(
                        "serverInfo" to mapOf("name" to "AiTask", "version" to "1.0"),
                        "capabilities" to mapOf("tools" to true, "resources" to true),
                        "endpointUrl" to configuration.endpointUrl
                    )
                )
            )

            "tools/list" -> success(
                mcpJson.encodeToJsonElement(
                    mapOf(
                        "tools" to buildManifest(configuration, "MCP bridge running").tools
                    )
                )
            )

            "tools/call" -> {
                val name = request.params?.stringValue("name")
                val arguments = request.params?.objectValue("arguments")
                when (name) {
                    "get_context" -> success(mcpJson.encodeToJsonElement(buildContextSnapshot(
                        projectId = arguments?.uuidValue("projectId"),
                        taskId = arguments?.uuidValue("taskId")
                    )))
                    "get_project" -> {
                        val projectId = arguments?.stringValue("projectId")
                            ?: return McpJsonRpcResponse(id = request.id, error = McpJsonRpcError(-32602, "projectId is required"))
                        val project = projectRepository.findById(UUID.fromString(projectId))
                            ?: return McpJsonRpcResponse(id = request.id, error = McpJsonRpcError(-32004, "Project not found"))
                        val context = buildContextSnapshot(project.id, null)
                        success(mcpJson.encodeToJsonElement(context))
                    }
                    "get_task" -> {
                        val taskId = arguments?.stringValue("taskId")
                            ?: return McpJsonRpcResponse(id = request.id, error = McpJsonRpcError(-32602, "taskId is required"))
                        val task = taskRepository.findById(UUID.fromString(taskId))
                            ?: return McpJsonRpcResponse(id = request.id, error = McpJsonRpcError(-32004, "Task not found"))
                        success(mcpJson.encodeToJsonElement(buildContextSnapshot(task.projectId, task.id)))
                    }
                    else -> McpJsonRpcResponse(id = request.id, error = McpJsonRpcError(-32601, "Unknown tool: $name"))
                }
            }

            "resources/list" -> success(
                mcpJson.encodeToJsonElement(
                    mapOf("resources" to buildManifest(configuration, "MCP bridge running").resources)
                )
            )

            "resources/read" -> {
                val uri = request.params?.stringValue("uri")
                when (uri) {
                    "aitask://context" -> success(mcpJson.encodeToJsonElement(buildContextSnapshot()))
                    "aitask://projects" -> success(mcpJson.encodeToJsonElement(buildContextSnapshot().projects))
                    "aitask://tasks" -> success(mcpJson.encodeToJsonElement(buildContextSnapshot().tasks))
                    else -> McpJsonRpcResponse(id = request.id, error = McpJsonRpcError(-32601, "Unknown resource: $uri"))
                }
            }

            else -> McpJsonRpcResponse(id = request.id, error = McpJsonRpcError(-32601, "Unknown method: ${request.method}"))
        }
    }

    private fun JsonObject.stringValue(name: String): String? =
        this[name]?.jsonPrimitive?.content

    private fun JsonObject.objectValue(name: String): JsonObject? =
        this[name]?.jsonObject

    private fun JsonObject.uuidValue(name: String): UUID? =
        stringValue(name)?.let(UUID::fromString)
}
