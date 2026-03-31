package com.aitask.core.infrastructure.plugin

import com.aitask.core.domain.plugin.PluginPrerequisite
import com.aitask.core.domain.plugin.PluginPrerequisiteType
import com.aitask.core.domain.service.IDEService
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class ProbeTestSlackWebApiClient(
    private val authTestFn: (String) -> Boolean,
    private val conversationInfoOkFn: (String, String) -> Boolean
) : SlackWebApiClient {
    override fun authTest(token: String): Boolean = authTestFn(token)
    override fun conversationInfoOk(token: String, channelId: String): Boolean =
        conversationInfoOkFn(token, channelId)
    override fun conversationHistory(
        token: String,
        channelId: String,
        oldestTs: String?,
        limit: Int,
        cursor: String?
    ): SlackConversationHistoryResult = SlackConversationHistoryResult.Error("unused in probe tests")
}

class DefaultPluginPrerequisiteProbeSlackTest {

    private val ideService: IDEService = mockk(relaxed = true)

    private fun slackPrerequisite(
        type: PluginPrerequisiteType,
        value: String = "slack_bot_token"
    ): PluginPrerequisite = PluginPrerequisite(
        type = type,
        name = "Slack check",
        value = value,
        remediation = "Configure Slack"
    )

    @Test
    fun `SLACK_API is not satisfied when token field is blank`() = runTest {
        val probe = DefaultPluginPrerequisiteProbe(
            ideService = ideService,
            slackWebApiClient = ProbeTestSlackWebApiClient(
                authTestFn = { true },
                conversationInfoOkFn = { _, _ -> true }
            )
        )
        val result = probe.evaluate(
            slackPrerequisite(PluginPrerequisiteType.SLACK_API),
            mapOf("slack_bot_token" to " ")
        )
        assertFalse(result.satisfied)
    }

    @Test
    fun `SLACK_API is satisfied when client accepts token`() = runTest {
        val probe = DefaultPluginPrerequisiteProbe(
            ideService = ideService,
            slackWebApiClient = ProbeTestSlackWebApiClient(
                authTestFn = { token -> token == "xoxb-valid" },
                conversationInfoOkFn = { _, _ -> false }
            )
        )
        val result = probe.evaluate(
            slackPrerequisite(PluginPrerequisiteType.SLACK_API),
            mapOf("slack_bot_token" to "xoxb-valid")
        )
        assertTrue(result.satisfied)
    }

    @Test
    fun `SLACK_API is not satisfied when client rejects token`() = runTest {
        val probe = DefaultPluginPrerequisiteProbe(
            ideService = ideService,
            slackWebApiClient = ProbeTestSlackWebApiClient(
                authTestFn = { false },
                conversationInfoOkFn = { _, _ -> false }
            )
        )
        val result = probe.evaluate(
            slackPrerequisite(PluginPrerequisiteType.SLACK_API),
            mapOf("slack_bot_token" to "xoxb-bad")
        )
        assertFalse(result.satisfied)
    }

    @Test
    fun `SLACK_CHANNELS fails when token missing`() = runTest {
        val probe = DefaultPluginPrerequisiteProbe(
            ideService = ideService,
            slackWebApiClient = ProbeTestSlackWebApiClient(
                authTestFn = { true },
                conversationInfoOkFn = { _, _ -> true }
            )
        )
        val result = probe.evaluate(
            slackPrerequisite(PluginPrerequisiteType.SLACK_CHANNELS, value = "analysis_channels"),
            mapOf("analysis_channels" to "C1")
        )
        assertFalse(result.satisfied)
    }

    @Test
    fun `SLACK_CHANNELS fails when channel list empty`() = runTest {
        val probe = DefaultPluginPrerequisiteProbe(
            ideService = ideService,
            slackWebApiClient = ProbeTestSlackWebApiClient(
                authTestFn = { true },
                conversationInfoOkFn = { _, _ -> true }
            )
        )
        val result = probe.evaluate(
            slackPrerequisite(PluginPrerequisiteType.SLACK_CHANNELS, value = "analysis_channels"),
            mapOf(
                "slack_bot_token" to "xoxb-good",
                "analysis_channels" to "  ,  "
            )
        )
        assertFalse(result.satisfied)
    }

    @Test
    fun `SLACK_CHANNELS satisfied when all channels reachable`() = runTest {
        val probe = DefaultPluginPrerequisiteProbe(
            ideService = ideService,
            slackWebApiClient = ProbeTestSlackWebApiClient(
                authTestFn = { token -> token == "xoxb-good" },
                conversationInfoOkFn = { token, channelId -> token == "xoxb-good" && channelId.startsWith("C") }
            )
        )
        val result = probe.evaluate(
            slackPrerequisite(PluginPrerequisiteType.SLACK_CHANNELS, value = "analysis_channels"),
            mapOf(
                "slack_bot_token" to "xoxb-good",
                "analysis_channels" to "C111, C222"
            )
        )
        assertTrue(result.satisfied)
    }

    @Test
    fun `SLACK_CHANNELS lists inaccessible channels`() = runTest {
        val probe = DefaultPluginPrerequisiteProbe(
            ideService = ideService,
            slackWebApiClient = ProbeTestSlackWebApiClient(
                authTestFn = { true },
                conversationInfoOkFn = { _, channelId -> channelId == "C111" }
            )
        )
        val result = probe.evaluate(
            slackPrerequisite(PluginPrerequisiteType.SLACK_CHANNELS, value = "analysis_channels"),
            mapOf(
                "slack_bot_token" to "xoxb-good",
                "analysis_channels" to "C111, C999"
            )
        )
        assertFalse(result.satisfied)
        assertTrue(result.message.contains("C999"))
    }
}
