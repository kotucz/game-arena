package cz.kotu.gamearena

import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import java.net.HttpURLConnection

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
                val sub = Json.parseToJsonElement(token.token).jsonObject
                val endpoint = sub["endpoint"]!!.jsonPrimitive.content
                val keys = sub["keys"]!!.jsonObject
                val p256dh = keys["p256dh"]!!.jsonPrimitive.content
                val auth = keys["auth"]!!.jsonPrimitive.content

                val notification = Notification(endpoint, p256dh, auth, payload.toByteArray(Charsets.UTF_8))
                pushService.send(notification)
                sent += 1
            } catch (e: Exception) {
                Napier.w(e) { "webpush send failed for ${token.tokenId}" }
                // Best-effort stale detection: if cause is HTTP 410/404, delete token
                val code = (e.cause as? java.net.HttpURLConnection)?.responseCode ?: (e as? java.io.IOException)?.let { null }
                if (e.message?.contains("410") == true || e.message?.contains("404") == true || code == HttpURLConnection.HTTP_GONE || code == HttpURLConnection.HTTP_NOT_FOUND) {
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
        // Minimal payload: include title/body and data as JSON string
        val map = mutableMapOf<String, Any>("title" to title, "body" to body)
        if (data.isNotEmpty()) map["data"] = data
        return Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), Json.parseToJsonElement(Json.encodeToString(map)).jsonObject)
    }
}
