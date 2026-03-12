package com.aitask.core.domain.service

import com.aitask.core.domain.model.IDEType
import com.aitask.core.domain.model.InstalledIDE
import com.aitask.core.domain.model.TaskContext

/**
 * Service for IDE integration and launching
 */
interface IDEService {
    /**
     * Launch an IDE with the specified workspace path and optional task context
     * 
     * @param ideType The type of IDE to launch
     * @param workspacePath The path to the workspace directory
     * @param taskContext Optional task context for IDE integration
     * @return Result indicating success or failure
     */
    suspend fun launchIDE(
        ideType: IDEType,
        workspacePath: String,
        taskContext: TaskContext? = null
    ): Result<Unit>
    
    /**
     * Detect all installed IDEs on the system
     * 
     * @return List of detected IDEs
     */
    suspend fun detectInstalledIDEs(): List<InstalledIDE>
    
    /**
     * Validate that an IDE is installed at the specified path
     * 
     * @param ideType The type of IDE to validate
     * @param path The path to validate
     * @return true if the IDE is valid at the path, false otherwise
     */
    suspend fun validateIDEPath(ideType: IDEType, path: String): Boolean
    
    /**
     * Get the default installation path for an IDE type on the current platform
     * 
     * @param ideType The type of IDE
     * @return The default path or null if not found
     */
    suspend fun getDefaultIDEPath(ideType: IDEType): String?
}

