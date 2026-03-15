package com.aitask.core.infrastructure.git

import com.aitask.core.domain.model.AuthType
import com.aitask.core.domain.service.GitAuthConfig
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.errors.NotSupportedException
import org.eclipse.jgit.transport.FetchConnection
import org.eclipse.jgit.transport.PushConnection
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.URIish
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class JGitServiceTest {
    @Test
    fun `should attach SSH transport callback for clone commands`() {
        val command = configureTransportAuthentication(
            Git.cloneRepository(),
            GitAuthConfig(
                authType = AuthType.SSH,
                sshKeyPath = "/tmp/test_id_ed25519"
            )
        )

        val callback = transportConfigCallback(command)
        val transport = TestSshTransport()

        callback.configure(transport)

        assertNotNull(callback)
        assertNotNull(transport.sshSessionFactory)
    }

    private fun transportConfigCallback(command: TransportCommand<*, *>): TransportConfigCallback {
        val field = TransportCommand::class.java.getDeclaredField("transportConfigCallback")
        field.isAccessible = true
        return field.get(command) as TransportConfigCallback
    }
}

private class TestSshTransport : SshTransport(URIish("git@github-personal:JTechArt/grqaser-app.git")) {
    override fun openFetch(): FetchConnection {
        throw NotSupportedException("Not needed for unit test")
    }

    override fun openPush(): PushConnection {
        throw NotSupportedException("Not needed for unit test")
    }

    override fun close() {
    }
}
