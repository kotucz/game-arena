package cz.kotu.gamearena

import cz.kotu.gamearena.model.RegisterTokenRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class WebPushTest {

    private val sampleSubscription =
        """{"endpoint":"https://fcm.googleapis.com/fcm/send/fake-endpoint","keys":{"p256dh":"BEl_fake_p256dh","auth":"fake_auth"}}"""

    private suspend fun ApplicationTestBuilder.registeredClient(
        component: TestServerComponent,
        username: String = "notif-user",
        password: String = "password123",
    ): HttpClient {
        if (component.database.userDao().findByUsername(username) == null) {
            component.database.userDao().insert(
                User(username, PasswordHasher.hash(password), "$username@example.com"),
            )
        }
        val client = createClient { install(HttpCookies) { storage = AcceptAllCookiesStorage() } }
        val login = client.post("/api/login") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("username" to username, "password" to password).formUrlEncode())
        }
        if (login.status != HttpStatusCode.OK) error("Login failed for $username: ${login.bodyAsText()}")
        return client
    }

    @Test
    fun registerWebPushSubscriptionStoresToken() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val client = registeredClient(component)

        val response = client.put("/api/notifications/tokens/web-1") {
            contentType(ContentType.Application.Json)
            setBody(
                Json.encodeToString(
                    RegisterTokenRequest.serializer(),
                    RegisterTokenRequest(service = "webpush", token = sampleSubscription),
                ),
            )
        }
        assertEquals(HttpStatusCode.NoContent, response.status)

        val stored = component.database.pushTokenDao().findByUsername("notif-user")
        assertEquals(1, stored.size)
        assertEquals("webpush", stored.single().service)
        // token should contain the endpoint value
        assert(stored.single().token.contains("fake-endpoint"))
    }

    @Test
    fun webPushServiceReturnsZeroWhenVapidMissing() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        // Insert a webpush token directly
        component.database.pushTokenDao().upsert(
            PushToken(
                tokenId = "web-1",
                username = "notif-user",
                service = "webpush",
                token = sampleSubscription,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )

        val service = WebPushNotificationService(component.database, component.serverConfig)
        val sent = service.sendToUser("notif-user", "title", "body")
        assertEquals(0, sent)
    }
}
