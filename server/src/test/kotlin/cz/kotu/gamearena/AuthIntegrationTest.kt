package cz.kotu.gamearena

import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthIntegrationTest {
    @Test
    fun registerSetsSessionCookieAndAllowsAuthenticatedMe() = testApplication {
        application { module(TestServerComponent::class.create()) }

        val username = "testuser_${System.currentTimeMillis()}"
        val email = "$username@example.com"
        val password = "password123"

        // Create a test client that stores cookies automatically
        val client = createClient {
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
        }

        val registerResponse: HttpResponse = client.post("/api/register") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("username" to username, "email" to email, "password" to password).formUrlEncode())
        }

        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val setCookie = registerResponse.headers[HttpHeaders.SetCookie]
        assertNotNull(setCookie, "Expected Set-Cookie header on register response")
        assertTrue(setCookie.contains(SessionTokens.cookieName), "Set-Cookie should include session cookie name")

        // Subsequent requests use stored cookie automatically
        val meResponse: HttpResponse = client.get("/api/me")

        assertEquals(HttpStatusCode.OK, meResponse.status)
        val body = meResponse.bodyAsText()
        assertEquals(username, body)
    }
}
