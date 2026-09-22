package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.time.Instant
import kotlin.test.*

class ApplicationTest {

    private suspend fun ensureTestUser(database: AppDatabase, username: String = "test-user") {
        if (database.userDao().findByUsername(username) == null) {
            database.userDao().insert(
                User(
                    username = username,
                    email = "$username@example.com",
                )
            )
        }
    }

    private suspend fun ApplicationTestBuilder.createAuthenticatedClient(
        component: ServerBindings,
        username: String = "test-user",
    ): HttpClient {
        ensureTestUser(component.database, username)

        val token = SessionTokens.create()
        component.database.sessionDao().insert(
            Session(
                tokenHash = SessionTokens.hash(token),
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

        return createClient {
            install(HttpCookies) {
                this.storage = storage
            }
        }
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

    @Test
    fun gameSpecificSseRoutesReturnNotFoundForUnknownGame() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val authClient = createAuthenticatedClient(component, "test-user")

        val eventsResponse = authClient.get("/api/games/missing/contacts/events")
        assertEquals(HttpStatusCode.NotFound, eventsResponse.status)
        assertEquals("Game not found", eventsResponse.bodyAsText())

        val logsResponse = authClient.get("/api/games/missing/logs")
        assertEquals(HttpStatusCode.NotFound, logsResponse.status)
        assertEquals("Game not found", logsResponse.bodyAsText())
    }
}
