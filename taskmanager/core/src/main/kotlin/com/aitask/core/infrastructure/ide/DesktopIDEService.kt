package com.aitask.core.infrastructure.ide

import com.aitask.core.domain.model.*
import com.aitask.core.domain.service.IDEService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Desktop implementation of IDEService for launching IDEs
 */
class DesktopIDEService : IDEService {
    
    private val osName = System.getProperty("os.name").lowercase()
    private val isMacOS = osName.contains("mac")
    private val isWindows = osName.contains("win")
    private val isLinux = osName.contains("nux")
    
    override suspend fun launchIDE(
        ideType: IDEType,
        workspacePath: String,
        taskContext: TaskContext?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logger.info { "Launching IDE: $ideType at workspace: $workspacePath" }
            
            // Validate workspace exists
            val workspaceDir = File(workspacePath)
            if (!workspaceDir.exists() || !workspaceDir.isDirectory) {
                return@withContext Result.failure(
                    IDELaunchException("Workspace directory does not exist: $workspacePath")
                )
            }
            
            // Detect IDE path
            val idePath = getDefaultIDEPath(ideType)
                ?: return@withContext Result.failure(IDENotFoundException(ideType))
            
            // Generate task context file if provided
            taskContext?.let {
                TaskContextFileWriter.write(workspacePath, it)
            }
            
            // Build and execute launch command
            val command = buildLaunchCommand(ideType, idePath, workspacePath)
            logger.info { "Executing command: ${command.joinToString(" ")}" }
            
            val processBuilder = ProcessBuilder(command)
                .directory(workspaceDir)
            
            // Start the process detached
            processBuilder.start()
            
            logger.info { "Successfully launched $ideType" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to launch IDE: $ideType" }
            Result.failure(IDELaunchException("Failed to launch IDE: ${e.message}", e))
        }
    }
    
    override suspend fun detectInstalledIDEs(): List<InstalledIDE> = withContext(Dispatchers.IO) {
        val installedIDEs = mutableListOf<InstalledIDE>()
        
        for (ideType in IDEType.values()) {
            val path = getDefaultIDEPath(ideType)
            if (path != null && validateIDEPath(ideType, path)) {
                installedIDEs.add(
                    InstalledIDE(
                        type = ideType,
                        version = "unknown", // Version detection can be added later
                        executablePath = path
                    )
                )
            }
        }
        
        logger.info { "Detected ${installedIDEs.size} installed IDEs: ${installedIDEs.map { it.type }}" }
        installedIDEs
    }
    
    override suspend fun validateIDEPath(ideType: IDEType, path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.exists() && (file.isFile || file.isDirectory)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to validate IDE path: $path" }
            false
        }
    }
    
    override suspend fun getDefaultIDEPath(ideType: IDEType): String? = withContext(Dispatchers.IO) {
        val possiblePaths = when (ideType) {
            IDEType.CURSOR -> getCursorPaths()
            IDEType.VS_CODE -> getVSCodePaths()
            IDEType.INTELLIJ_IDEA -> getIntelliJPaths()
            IDEType.WEBSTORM -> getWebStormPaths()
            IDEType.PYCHARM -> getPyCharmPaths()
            IDEType.GOLAND -> getGoLandPaths()
        }
        
        possiblePaths.firstOrNull { path ->
            File(path).exists()
        }
    }
    
    private fun buildLaunchCommand(ideType: IDEType, idePath: String, workspacePath: String): List<String> {
        return when {
            isMacOS && idePath.endsWith(".app") -> {
                // macOS app bundle
                listOf("open", "-a", idePath, workspacePath)
            }
            isMacOS -> {
                // macOS executable
                listOf(idePath, workspacePath)
            }
            isWindows -> {
                // Windows
                listOf("cmd", "/c", "start", "", idePath, workspacePath)
            }
            else -> {
                // Linux
                listOf(idePath, workspacePath)
            }
        }
    }
    
    // Platform-specific IDE path detection methods

    private fun getCursorPaths(): List<String> = when {
        isMacOS -> listOf(
            "/Applications/Cursor.app"
        )
        isWindows -> {
            val username = System.getenv("USERNAME") ?: "User"
            listOf(
                "C:\\Users\\$username\\AppData\\Local\\Programs\\cursor\\Cursor.exe",
                "C:\\Program Files\\Cursor\\Cursor.exe"
            )
        }
        else -> listOf(
            "/usr/bin/cursor",
            "/usr/local/bin/cursor",
            "${System.getProperty("user.home")}/.local/bin/cursor"
        )
    }

    private fun getVSCodePaths(): List<String> = when {
        isMacOS -> listOf(
            "/Applications/Visual Studio Code.app"
        )
        isWindows -> {
            val username = System.getenv("USERNAME") ?: "User"
            listOf(
                "C:\\Users\\$username\\AppData\\Local\\Programs\\Microsoft VS Code\\Code.exe",
                "C:\\Program Files\\Microsoft VS Code\\Code.exe"
            )
        }
        else -> listOf(
            "/usr/bin/code",
            "/usr/local/bin/code",
            "${System.getProperty("user.home")}/.local/bin/code"
        )
    }

    private val userApplicationsDir: String
        get() = "${System.getProperty("user.home")}/Applications"

    private fun getIntelliJPaths(): List<String> = when {
        isMacOS -> listOf(
            "/Applications/IntelliJ IDEA.app",
            "/Applications/IntelliJ IDEA CE.app",
            "/Applications/IntelliJ IDEA Ultimate.app",
            "$userApplicationsDir/IntelliJ IDEA.app",
            "$userApplicationsDir/IntelliJ IDEA CE.app",
            "$userApplicationsDir/IntelliJ IDEA Ultimate.app"
        )
        isWindows -> listOf(
            "C:\\Program Files\\JetBrains\\IntelliJ IDEA\\bin\\idea64.exe",
            "C:\\Program Files (x86)\\JetBrains\\IntelliJ IDEA\\bin\\idea64.exe"
        )
        else -> listOf(
            "/usr/bin/idea",
            "/usr/local/bin/idea",
            "${System.getProperty("user.home")}/.local/bin/idea"
        )
    }

    private fun getWebStormPaths(): List<String> = when {
        isMacOS -> listOf(
            "/Applications/WebStorm.app",
            "$userApplicationsDir/WebStorm.app"
        )
        isWindows -> listOf(
            "C:\\Program Files\\JetBrains\\WebStorm\\bin\\webstorm64.exe",
            "C:\\Program Files (x86)\\JetBrains\\WebStorm\\bin\\webstorm64.exe"
        )
        else -> listOf(
            "/usr/bin/webstorm",
            "/usr/local/bin/webstorm",
            "${System.getProperty("user.home")}/.local/bin/webstorm"
        )
    }

    private fun getPyCharmPaths(): List<String> = when {
        isMacOS -> listOf(
            "/Applications/PyCharm.app",
            "/Applications/PyCharm CE.app",
            "$userApplicationsDir/PyCharm.app",
            "$userApplicationsDir/PyCharm CE.app"
        )
        isWindows -> listOf(
            "C:\\Program Files\\JetBrains\\PyCharm\\bin\\pycharm64.exe",
            "C:\\Program Files (x86)\\JetBrains\\PyCharm\\bin\\pycharm64.exe"
        )
        else -> listOf(
            "/usr/bin/pycharm",
            "/usr/local/bin/pycharm",
            "${System.getProperty("user.home")}/.local/bin/pycharm"
        )
    }

    private fun getGoLandPaths(): List<String> = when {
        isMacOS -> listOf(
            "/Applications/GoLand.app",
            "$userApplicationsDir/GoLand.app"
        )
        isWindows -> listOf(
            "C:\\Program Files\\JetBrains\\GoLand\\bin\\goland64.exe",
            "C:\\Program Files (x86)\\JetBrains\\GoLand\\bin\\goland64.exe"
        )
        else -> listOf(
            "/usr/bin/goland",
            "/usr/local/bin/goland",
            "${System.getProperty("user.home")}/.local/bin/goland"
        )
    }
}
