package com.aitask.core.infrastructure.plugin

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

interface SlackWebApiClient {
    fun authTest(token: String): Boolean
    fun conversationInfoOk(token: String, channelId: String): Boolean
}

class DefaultSlackWebApiClient : SlackWebApiClient {
    override fun authTest(token: String): Boolean {
        return try {
            val connection = URI.create("https://slack.com/api/auth.test").toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 5000
            connection.readTimeout = 8000
            connection.connect()
            val responseCode = connection.responseCode
            val body = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader()
                .use { it.readText() }
            slackJsonResponseOk(body)
        } catch (e: Exception) {
            false
        }
    }

    override fun conversationInfoOk(token: String, channelId: String): Boolean {
        return try {
            val connection = URI.create("https://slack.com/api/conversations.info").toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 8000
            val body = "channel=${URLEncoder.encode(channelId, StandardCharsets.UTF_8)}"
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val responseCode = connection.responseCode
            val text = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                .bufferedReader()
                .use { it.readText() }
            slackJsonResponseOk(text)
        } catch (e: Exception) {
            false
        }
    }
}

internal fun slackJsonResponseOk(body: String): Boolean =
    body.contains("\"ok\":true") || body.contains("\"ok\": true")
