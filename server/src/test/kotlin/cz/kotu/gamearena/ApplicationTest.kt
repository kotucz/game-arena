package cz.kotu.gamearena

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    @Test
    fun healthCheck() = testApplication {
        application {
            module()
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun registrationCreatesPersistentSession() = testApplication {
        application { module() }

        val username = "user_${System.currentTimeMillis()}"
        val registration = client.post("/api/register") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("username=$username&email=$username%40example.com&password=correctPassword123")
        }

        assertEquals(HttpStatusCode.Created, registration.status)
        assertTrue(registration.headers[HttpHeaders.SetCookie]?.startsWith("gamearena_session=") == true)
    }

    @Test
    fun debugUsernameAuthenticatesWithoutSessionCookie() = testApplication {
        application { module() }

        val response = client.get("/api/me") {
            header("X-Debug-Username", "test-user")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("test-user", response.bodyAsText())
    }
}
