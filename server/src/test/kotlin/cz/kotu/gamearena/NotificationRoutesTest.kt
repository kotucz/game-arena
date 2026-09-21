package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.delete
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationRoutesTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private suspend fun ApplicationTestBuilder.registeredClient(
        component: TestServerComponent,
        username: String = "notif-user",
        password: String = "password123",
    ): HttpClient {
        if (component.database.userDao().findByUsername(username) == null) {
            component.database.userDao().insert(
                User(username, PasswordHasher.hash(password), "$username@example.com")
            )
        }
        val client = createClient { install(HttpCookies) { storage = AcceptAllCookiesStorage() } }
        val login = client.post("/api/login") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("username" to username, "password" to password).formUrlEncode())
        }
        assertEquals(HttpStatusCode.OK, login.status, "Login failed for $username")
        return client
    }

    private fun tokenBody(service: String = "fcm", token: String = "tok-abc") =
        """{"service":"$service","token":"$token"}"""

    // ── registration ─────────────────────────────────────────────────────────

    @Test
    fun registerTokenReturns204() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }
        val client = registeredClient(component)

        val response = client.put("/api/notifications/tokens/device-1") {
            contentType(ContentType.Application.Json)
            setBody(tokenBody())
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun registerTokenIsIdempotent() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }
        val client = registeredClient(component)

        // First registration
        client.put("/api/notifications/tokens/device-1") {
            contentType(ContentType.Application.Json)
            setBody(tokenBody(token = "old-token"))
        }

        // Refresh with new token value — same tokenId
        val second = client.put("/api/notifications/tokens/device-1") {
            contentType(ContentType.Application.Json)
            setBody(tokenBody(token = "new-token"))
        }
        assertEquals(HttpStatusCode.NoContent, second.status)

        // Only one row exists and it has the latest token
        val stored = component.database.pushTokenDao().findByUsername("notif-user")
        assertEquals(1, stored.size)
        assertEquals("new-token", stored.single().token)
    }

    @Test
    fun multipleDevicesPerServiceStoredSeparately() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }
        val client = registeredClient(component)

        client.put("/api/notifications/tokens/phone") {
            contentType(ContentType.Application.Json)
            setBody(tokenBody(service = "fcm", token = "tok-phone"))
        }
        client.put("/api/notifications/tokens/tablet") {
            contentType(ContentType.Application.Json)
            setBody(tokenBody(service = "fcm", token = "tok-tablet"))
        }

        val stored = component.database.pushTokenDao().findByUsername("notif-user")
        assertEquals(2, stored.size)
        assertTrue(stored.any { it.tokenId == "phone" && it.token == "tok-phone" })
        assertTrue(stored.any { it.tokenId == "tablet" && it.token == "tok-tablet" })
    }

    @Test
    fun registerTokenRejectsUnknownService() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }
        val client = registeredClient(component)

        val response = client.put("/api/notifications/tokens/device-1") {
            contentType(ContentType.Application.Json)
            setBody(tokenBody(service = "carrier-pigeon"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Unknown service"))
    }

    @Test
    fun registerTokenRejectsWebPush() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }
        val client = registeredClient(component)

        // "webpush" was removed when the web client switched to FCM.
        val response = client.put("/api/notifications/tokens/web-1") {
            contentType(ContentType.Application.Json)
            setBody(tokenBody(service = "webpush"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Unknown service"))
    }

    @Test
    fun registerTokenRequiresAuthentication() = testApplication {
        application { module(TestServerComponent::class.create()) }

        val response = client.put("/api/notifications/tokens/device-1") {
            contentType(ContentType.Application.Json)
            setBody(tokenBody())
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ── deletion ─────────────────────────────────────────────────────────────

    @Test
    fun deleteTokenRemovesIt() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }
        val client = registeredClient(component)

        client.put("/api/notifications/tokens/device-1") {
            contentType(ContentType.Application.Json)
            setBody(tokenBody())
        }

        val del = client.delete("/api/notifications/tokens/device-1")
        assertEquals(HttpStatusCode.NoContent, del.status)

        val stored = component.database.pushTokenDao().findByUsername("notif-user")
        assertTrue(stored.isEmpty())
    }

    @Test
    fun deleteTokenIsIdempotent() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }
        val client = registeredClient(component)

        // Delete a token that was never registered — should silently succeed
        val response = client.delete("/api/notifications/tokens/nonexistent")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun deleteTokenDoesNotAffectOtherUsersToken() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val alice = registeredClient(component, username = "alice-notif")
        val bob = registeredClient(component, username = "bob-notif")

        // Alice registers her own token
        alice.put("/api/notifications/tokens/alice-device") {
            contentType(ContentType.Application.Json)
            setBody(tokenBody(token = "tok-alice"))
        }

        // Bob knows Alice's tokenId and tries to delete it — ownership scoping must block this
        bob.delete("/api/notifications/tokens/alice-device")

        // Alice's token must still be present
        assertEquals(1, component.database.pushTokenDao().findByUsername("alice-notif").size)
    }

    @Test
    fun deleteTokenRequiresAuthentication() = testApplication {
        application { module(TestServerComponent::class.create()) }

        val response = client.delete("/api/notifications/tokens/device-1")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
