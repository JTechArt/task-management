package com.aitask.core.infrastructure.plugin

import com.aitask.core.domain.plugin.PluginPrerequisite
import com.aitask.core.domain.plugin.PluginPrerequisiteType
import com.aitask.core.domain.service.IDEService
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
            slackWebApiClient = object : SlackWebApiClient {
                override fun authTest(token: String): Boolean = true
                override fun conversationInfoOk(token: String, channelId: String): Boolean = true
            }
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
            slackWebApiClient = object : SlackWebApiClient {
                override fun authTest(token: String): Boolean = token == "xoxb-valid"
                override fun conversationInfoOk(token: String, channelId: String): Boolean = false
            }
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
            slackWebApiClient = object : SlackWebApiClient {
                override fun authTest(token: String): Boolean = false
                override fun conversationInfoOk(token: String, channelId: String): Boolean = false
            }
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
            slackWebApiClient = object : SlackWebApiClient {
                override fun authTest(token: String): Boolean = true
                override fun conversationInfoOk(token: String, channelId: String): Boolean = true
            }
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
            slackWebApiClient = object : SlackWebApiClient {
                override fun authTest(token: String): Boolean = true
                override fun conversationInfoOk(token: String, channelId: String): Boolean = true
            }
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
            slackWebApiClient = object : SlackWebApiClient {
                override fun authTest(token: String): Boolean = true
                override fun conversationInfoOk(token: String, channelId: String): Boolean =
                    token == "xoxb-good" && channelId.startsWith("C")
            }
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
            slackWebApiClient = object : SlackWebApiClient {
                override fun authTest(token: String): Boolean = true
                override fun conversationInfoOk(token: String, channelId: String): Boolean = channelId == "C111"
            }
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
