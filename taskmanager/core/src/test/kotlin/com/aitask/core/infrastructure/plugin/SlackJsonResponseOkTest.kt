package com.aitask.core.infrastructure.plugin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlackJsonResponseOkTest {

    @Test
    fun `slackJsonResponseOk accepts ok true variants`() {
        assertTrue(slackJsonResponseOk("{\"ok\":true}"))
        assertTrue(slackJsonResponseOk("{\"ok\": true}"))
    }

    @Test
    fun `slackJsonResponseOk rejects ok false`() {
        assertFalse(slackJsonResponseOk("{\"ok\":false,\"error\":\"invalid_auth\"}"))
    }
}
