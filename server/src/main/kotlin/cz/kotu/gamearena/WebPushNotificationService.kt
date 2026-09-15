package cz.kotu.gamearena

import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService

class WebPushNotificationService(
    private val database: AppDatabase,
    private val serverConfig: ServerConfig,
) : PushNotificationService {

    override suspend fun sendToUser(
        username: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ): Int = sendToUsers(listOf(username), title, body, data)

    override suspend fun sendToUsers(
        usernames: Collection<String>,
        title: String,
        body: String,
        data: Map<String, String>,
    ): Int {
        val uniqueUsernames = usernames.filter { it.isNotBlank() }.distinct()
        if (uniqueUsernames.isEmpty()) return 0

        val tokens = database.pushTokenDao().findByUsernames(uniqueUsernames)
            .filter { it.service == "webpush" }

        if (tokens.isEmpty()) return 0

        val vapidPublic = serverConfig.vapidPublicKey
        val vapidPrivate = serverConfig.vapidPrivateKey
        if (vapidPublic.isNullOrBlank() || vapidPrivate.isNullOrBlank()) {
            Napier.w("WebPushNotificationService: VAPID keys not configured; skipping webpush send")
            return 0
        }

        val pushService = PushService().apply {
            setPublicKey(vapidPublic)
            setPrivateKey(vapidPrivate)
            setSubject("mailto:${serverConfig.adminUsername}")
        }

        var sent = 0
        val payload = buildPayload(title, body, data)

        for (token in tokens) {
            try {
                val element = Json.parseToJsonElement(token.token)
                val sub = element.jsonObject
                val endpoint = sub["endpoint"]?.jsonPrimitive?.contentOrNull
                val keys = sub["keys"]?.jsonObject
                val p256dh = keys?.get("p256dh")?.jsonPrimitive?.contentOrNull
                val auth = keys?.get("auth")?.jsonPrimitive?.contentOrNull

                if (endpoint.isNullOrBlank() || p256dh.isNullOrBlank() || auth.isNullOrBlank()) {
                    Napier.w("WebPushNotificationService: invalid subscription for ${token.tokenId}")
                    database.pushTokenDao().delete(token.tokenId, token.username)
                    continue
                }

                val notification = Notification(endpoint, p256dh, auth, payload.toByteArray(Charsets.UTF_8))
                pushService.send(notification)
                sent += 1
            } catch (e: Exception) {
                Napier.w(e) { "webpush send failed for ${token.tokenId}" }
                // Best-effort stale detection: look for 404/410 in message or cause
                val message = (e.message ?: "") + (e.cause?.message ?: "")
                if (message.contains("410") || message.contains("404") || message.contains("gone", ignoreCase = true)) {
                    try {
                        database.pushTokenDao().delete(token.tokenId, token.username)
                    } catch (dbEx: Exception) {
                        Napier.w(dbEx) { "Failed to delete stale token ${token.tokenId}" }
                    }
                }
            }
        }

        return sent
    }

    private fun buildPayload(title: String, body: String, data: Map<String, String>): String {
        val obj = buildJsonObject {
            put("title", title)
            put("body", body)
            if (data.isNotEmpty()) {
                val dataObj = buildJsonObject {
                    for ((k, v) in data) put(k, v)
                }
                put("data", dataObj)
            }
        }
        return Json.encodeToString(JsonObject.serializer(), obj)
    }
}
