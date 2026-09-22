package cz.kotu.gamearena

import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthIntegrationTest {

    @Test
    fun legacyAuthEndpointsReturnNotFound() = testApplication {
        application { module(TestServerComponent::class.create()) }
        val client = createClient {}

        val loginResponse = client.post("/api/login") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("username" to "user", "password" to "pass").formUrlEncode())
        }
        assertEquals(HttpStatusCode.NotFound, loginResponse.status)

        val registerResponse = client.post("/api/register") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("username" to "user", "email" to "user@example.com", "password" to "pass").formUrlEncode())
        }
        assertEquals(HttpStatusCode.NotFound, registerResponse.status)
    }

    @Test
    fun firebaseAuthRejectsInvalidRequests() = testApplication {
        application { module(TestServerComponent::class.create()) }
        val client = createClient {}

        val missingTokenResponse = client.post("/api/auth/firebase")
        assertEquals(HttpStatusCode.BadRequest, missingTokenResponse.status)

        val invalidTokenResponse = client.post("/api/auth/firebase") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("idToken" to "invalid-token-123", "username" to "user").formUrlEncode())
        }
        assertEquals(HttpStatusCode.Unauthorized, invalidTokenResponse.status)
    }

    @Test
    fun authenticatedSessionAllowsMeAndLogoutClearsSession() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val username = "testuser_${System.currentTimeMillis()}"
        val user = User(username = username, email = "$username@example.com")
        component.database.userDao().insert(user)

        val token = SessionTokens.create()
        val tokenHash = SessionTokens.hash(token)
        component.database.sessionDao().insert(
            Session(
                tokenHash = tokenHash,
                username = username,
                expiresAt = Instant.now().epochSecond + SessionTokens.lifetimeSeconds,
                userId = username,
            )
        )

        val storage = AcceptAllCookiesStorage()
        storage.addCookie(
            Url("http://localhost/"),
            Cookie(name = SessionTokens.cookieName, value = token, path = "/")
        )

        val client = createClient {
            install(HttpCookies) {
                this.storage = storage
            }
        }

        // Authenticated request to /api/me
        val meResponse: HttpResponse = client.get("/api/me")
        assertEquals(HttpStatusCode.OK, meResponse.status)
        assertEquals(username, meResponse.bodyAsText())

        // Logout
        val logoutResponse: HttpResponse = client.post("/api/logout")
        assertEquals(HttpStatusCode.OK, logoutResponse.status)
        assertEquals("Logout successful", logoutResponse.bodyAsText())

        // Database session is removed
        assertNull(component.database.sessionDao().findByTokenHash(tokenHash))

        // Subsequent /api/me fails with 401 Unauthorized
        val unauthResponse: HttpResponse = client.get("/api/me")
        assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status)
    }
}
