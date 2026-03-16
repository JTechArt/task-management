package com.aitask.core.domain.service

import com.aitask.core.domain.model.AuthType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class GitAuthConfigTest {

    @Test
    fun `toString masks password and never exposes it`() {
        val config = GitAuthConfig(
            authType = AuthType.HTTPS,
            username = "git",
            password = "secret123",
            token = null
        )
        val stringRep = config.toString()
        assertFalse(stringRep.contains("secret123"))
        assertFalse(stringRep.contains("password=") && stringRep.contains("secret"))
    }

    @Test
    fun `toString masks token and never exposes it`() {
        val config = GitAuthConfig(
            authType = AuthType.TOKEN,
            username = "oauth2",
            password = null,
            token = "ghp_abc123xyz"
        )
        val stringRep = config.toString()
        assertFalse(stringRep.contains("ghp_abc123xyz"))
    }

    @Test
    fun `toString includes authType and username for debugging`() {
        val config = GitAuthConfig(
            authType = AuthType.SSH,
            username = "git",
            sshKeyPath = "/home/user/.ssh/id_ed25519"
        )
        val stringRep = config.toString()
        assert(stringRep.contains("authType=SSH"))
        assert(stringRep.contains("username=git"))
        assert(stringRep.contains("sshKeyPath=/home/user/.ssh/id_ed25519"))
    }
}
