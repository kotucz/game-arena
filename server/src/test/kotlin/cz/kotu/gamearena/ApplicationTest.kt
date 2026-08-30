package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    private suspend fun ensureTestUser(database: AppDatabase, username: String = "test-user") {
        if (database.userDao().findByUsername(username) == null) {
            database.userDao().insert(
                User(
                    username = username,
                    passwordHash = PasswordHasher.hash("password123"),
                    email = "$username@example.com",
                )
            )
        }
    }

    private suspend fun ApplicationTestBuilder.createAuthenticatedClient(
        component: ServerBindings,
        username: String = "test-user",
        password: String = "password123",
    ): HttpClient {
        ensureTestUser(component.database, username)

        val authenticatedClient = createClient {
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
        }

        val loginResponse = authenticatedClient.post("/api/login") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(listOf("username" to username, "password" to password).formUrlEncode())
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status, "Failed to log in test user")
        return authenticatedClient
    }

    @Test
    fun healthCheck() = testApplication {
        val component = TestServerComponent::class.create()
        application {
            module(component)
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun registrationCreatesPersistentSession() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val username = "user_${System.currentTimeMillis()}"
        val registration = client.post("/api/register") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("username=$username&email=$username%40example.com&password=correctPassword123")
        }

        assertEquals(HttpStatusCode.Created, registration.status)
        assertTrue(registration.headers[HttpHeaders.SetCookie]?.startsWith("gamearena_session=") == true)
    }

    @Test
    fun authenticatedUserCanGetProfile() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val authClient = createAuthenticatedClient(component, "test-user")
        val response = authClient.get("/api/me")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("test-user", response.bodyAsText())
    }

    @Test
    fun listingGamesRequiresAuthentication() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val response = client.get("/api/games")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun createsContactsGameForAuthenticatedUser() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val authClient = createAuthenticatedClient(component, "test-user")

        val response = authClient.post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody("{\"type\":\"contacts\",\"players\":[\"alice\",\"bob\"], \"config\":\"{}\"}")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(ContentType.Application.Json, response.contentType())
        assertTrue(response.bodyAsText().contains("\"type\":\"contacts\""))
        assertTrue(response.bodyAsText().contains("\"players\":[\"alice\",\"bob\"]"))

        // listsRunningGamesForAuthenticatedUser

        val response2 = authClient.get("/api/games")

        assertEquals(HttpStatusCode.OK, response2.status)
        val body = response2.bodyAsText()
        assertTrue(body.startsWith("["))
        assertTrue(body.contains("\"id\":"))
        assertTrue(body.contains("\"type\":\"contacts\""))
        assertTrue(body.contains("\"players\":[\"alice\",\"bob\"]"))
        assertTrue(body.contains("\"createdAt\":"))
    }

    @Test
    fun creatingUnsupportedGameTypeReturnsBadRequest() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val authClient = createAuthenticatedClient(component, "test-user")

        val response = authClient.post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody("{\"type\":\"future-game\",\"players\":[\"alice\"]}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun gameSpecificActionRouteReturnsNotFoundForUnknownGame() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val authClient = createAuthenticatedClient(component, "test-user")

        val response = authClient.post("/api/games/missing/contacts/actions") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("Game not found", response.bodyAsText())
    }
}

