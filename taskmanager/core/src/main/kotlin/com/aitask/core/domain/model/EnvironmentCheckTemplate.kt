package com.aitask.core.domain.model

enum class EnvironmentCheckTemplateId {
    NODE_VERSION,
    JAVA_VERSION,
    PYTHON_VERSION,
    ENVIRONMENT_VARIABLE,
    DEPENDENCY_PRESENT
}

data class EnvironmentCheckTemplate(
    val id: EnvironmentCheckTemplateId,
    val name: String,
    val description: String,
    val scriptType: PreRunScriptType,
    val parameterLabel: String,
    val parameterPlaceholder: String,
    val exampleValue: String
) {
    fun generateName(parameterValue: String): String = when (id) {
        EnvironmentCheckTemplateId.NODE_VERSION -> "Check Node.js $parameterValue"
        EnvironmentCheckTemplateId.JAVA_VERSION -> "Check Java $parameterValue"
        EnvironmentCheckTemplateId.PYTHON_VERSION -> "Check Python $parameterValue"
        EnvironmentCheckTemplateId.ENVIRONMENT_VARIABLE -> "Check env var $parameterValue"
        EnvironmentCheckTemplateId.DEPENDENCY_PRESENT -> "Check dependency $parameterValue"
    }

    fun generatePreview(parameterValue: String, isWindows: Boolean): String {
        return when (scriptType) {
            PreRunScriptType.NODE_VERSION -> if (isWindows) {
                "@echo off\r\nnode --version | findstr /R /C:\"^v\\{0,1}$parameterValue\\(\\..*\\)\\{0,1}$\" >nul || (echo Node.js $parameterValue is required. Install or switch Node versions. & exit /b 1)"
            } else {
                "node --version | grep -E '^v?$parameterValue([.]|$)' >/dev/null || { echo 'Node.js $parameterValue is required. Install it or switch versions.' >&2; exit 1; }"
            }

            PreRunScriptType.JAVA_VERSION -> if (isWindows) {
                "@echo off\r\njava -version 2>&1 | findstr /C:\"$parameterValue\" >nul || (echo Java $parameterValue is required. Verify JAVA_HOME and the installed JDK. & exit /b 1)"
            } else {
                "java -version 2>&1 | grep -F '$parameterValue' >/dev/null || { echo 'Java $parameterValue is required. Verify JAVA_HOME and the installed JDK.' >&2; exit 1; }"
            }

            PreRunScriptType.PYTHON_VERSION -> if (isWindows) {
                "@echo off\r\n(python --version 2>&1 || python3 --version 2>&1) | findstr /C:\"$parameterValue\" >nul || (echo Python $parameterValue is required. Install Python or update PATH. & exit /b 1)"
            } else {
                "(python --version 2>&1 || python3 --version 2>&1) | grep -F '$parameterValue' >/dev/null || { echo 'Python $parameterValue is required. Install Python or update PATH.' >&2; exit 1; }"
            }

            PreRunScriptType.ENVIRONMENT_VARIABLE -> if (isWindows) {
                "@echo off\r\nif defined $parameterValue (exit /b 0) else (echo Missing required environment variable: $parameterValue & exit /b 1)"
            } else {
                "if [ -n \"\${$parameterValue}\" ]; then exit 0; else echo 'Missing required environment variable: $parameterValue' >&2; exit 1; fi"
            }

            PreRunScriptType.DEPENDENCY_PRESENT -> if (isWindows) {
                "@echo off\r\nwhere $parameterValue >nul 2>nul || (echo Required dependency '$parameterValue' was not found on PATH. Install it before launching the IDE. & exit /b 1)"
            } else {
                "command -v $parameterValue >/dev/null 2>&1 || { echo \"Required dependency '$parameterValue' was not found on PATH. Install it before launching the IDE.\" >&2; exit 1; }"
            }

            else -> error("Template previews are only supported for environment check templates")
        }
    }
}

object EnvironmentCheckTemplateRegistry {
    val templates: List<EnvironmentCheckTemplate> = listOf(
        EnvironmentCheckTemplate(
            id = EnvironmentCheckTemplateId.NODE_VERSION,
            name = "Node.js Version",
            description = "Checks that the required Node.js version is available.",
            scriptType = PreRunScriptType.NODE_VERSION,
            parameterLabel = "Required Version",
            parameterPlaceholder = "20",
            exampleValue = "20"
        ),
        EnvironmentCheckTemplate(
            id = EnvironmentCheckTemplateId.JAVA_VERSION,
            name = "Java Version",
            description = "Checks that the required Java version is installed.",
            scriptType = PreRunScriptType.JAVA_VERSION,
            parameterLabel = "Required Version",
            parameterPlaceholder = "21",
            exampleValue = "21"
        ),
        EnvironmentCheckTemplate(
            id = EnvironmentCheckTemplateId.PYTHON_VERSION,
            name = "Python Version",
            description = "Checks that the required Python version is installed.",
            scriptType = PreRunScriptType.PYTHON_VERSION,
            parameterLabel = "Required Version",
            parameterPlaceholder = "3.11",
            exampleValue = "3.11"
        ),
        EnvironmentCheckTemplate(
            id = EnvironmentCheckTemplateId.ENVIRONMENT_VARIABLE,
            name = "Environment Variable",
            description = "Checks that a required environment variable is present.",
            scriptType = PreRunScriptType.ENVIRONMENT_VARIABLE,
            parameterLabel = "Variable Name",
            parameterPlaceholder = "JAVA_HOME",
            exampleValue = "JAVA_HOME"
        ),
        EnvironmentCheckTemplate(
            id = EnvironmentCheckTemplateId.DEPENDENCY_PRESENT,
            name = "Dependency Present",
            description = "Checks that a required CLI dependency is available on PATH.",
            scriptType = PreRunScriptType.DEPENDENCY_PRESENT,
            parameterLabel = "Dependency Name",
            parameterPlaceholder = "docker",
            exampleValue = "docker"
        )
    )

    fun all(): List<EnvironmentCheckTemplate> = templates

    fun findById(id: EnvironmentCheckTemplateId): EnvironmentCheckTemplate =
        templates.first { it.id == id }

    fun findByType(type: PreRunScriptType): EnvironmentCheckTemplate? =
        templates.find { it.scriptType == type }
}
