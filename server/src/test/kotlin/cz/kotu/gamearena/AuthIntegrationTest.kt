package cz.kotu.gamearena

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthIntegrationTest {
    @Test
    fun registerSetsSessionCookieAndAllowsAuthenticatedMe() = testApplication {
        application { module() }

        val username = "testuser_${System.currentTimeMillis()}"
        val email = "$username@example.com"
        val password = "password123"

        val registerResponse: HttpResponse = client.post("/api/register") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("username" to username, "email" to email, "password" to password).formUrlEncode())
        }

        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val setCookie = registerResponse.headers[HttpHeaders.SetCookie]
        assertNotNull(setCookie, "Expected Set-Cookie header on register response")
        assertTrue(setCookie.contains(SessionTokens.cookieName), "Set-Cookie should include session cookie name")

        // Extract the cookie value from the Set-Cookie header and send it explicitly on the next request.
        val sessionValue = setCookie.substringAfter("${SessionTokens.cookieName}=").substringBefore(';')
        val meResponse: HttpResponse = client.get("/api/me") {
            header(HttpHeaders.Cookie, "${SessionTokens.cookieName}=${sessionValue}")
        }

        assertEquals(HttpStatusCode.OK, meResponse.status)
        val body = meResponse.bodyAsText()
        assertEquals(username, body)
    }
}
