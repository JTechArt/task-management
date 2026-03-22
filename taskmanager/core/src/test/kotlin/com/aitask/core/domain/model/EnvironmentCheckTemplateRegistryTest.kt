package com.aitask.core.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EnvironmentCheckTemplateRegistryTest {

    @Test
    fun `should expose all expected environment check templates`() {
        val templates = EnvironmentCheckTemplateRegistry.all()

        assertEquals(
            setOf(
                EnvironmentCheckTemplateId.NODE_VERSION,
                EnvironmentCheckTemplateId.JAVA_VERSION,
                EnvironmentCheckTemplateId.PYTHON_VERSION,
                EnvironmentCheckTemplateId.ENVIRONMENT_VARIABLE,
                EnvironmentCheckTemplateId.DEPENDENCY_PRESENT
            ),
            templates.map { it.id }.toSet()
        )
    }

    @Test
    fun `should generate actionable unix preview for dependency template`() {
        val template = EnvironmentCheckTemplateRegistry.findById(EnvironmentCheckTemplateId.DEPENDENCY_PRESENT)

        val preview = template.generatePreview("docker", isWindows = false)

        assertTrue(preview.contains("command -v docker"))
        assertTrue(preview.contains("not found on PATH"))
    }

    @Test
    fun `should generate friendly default name from template`() {
        val template = EnvironmentCheckTemplateRegistry.findById(EnvironmentCheckTemplateId.ENVIRONMENT_VARIABLE)

        assertEquals("Check env var JAVA_HOME", template.generateName("JAVA_HOME"))
    }
}
