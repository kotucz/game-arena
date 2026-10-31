package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    private suspend fun ensureTestUser(
        database: AppDatabase,
        username: String = "test-user",
        firebaseUid: String = "uid-$username",
    ) {
        if (database.userDao().findByFirebaseUid(firebaseUid) == null) {
            database.userDao().insert(
                User(
                    firebaseUid = firebaseUid,
                    username = username,
                    usernameLower = username.lowercase(),
                    email = "$username@example.com",
                )
            )
        }
    }

    private suspend fun ApplicationTestBuilder.createAuthenticatedClient(
        component: ServerBindings,
        username: String = "test-user",
        firebaseUid: String = "uid-$username",
    ): HttpClient {
        ensureTestUser(component.database, username, firebaseUid)
        val token = "test-token-$firebaseUid"
        return createClient {
            defaultRequest {
                header(HttpHeaders.Authorization, "Bearer $token")
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
        val body = response.bodyAsText()
        assertTrue(body.contains("\"uid\":\"uid-test-user\""))
        assertTrue(body.contains("\"username\":\"test-user\""))
    }

    @Test
    fun availablePlayersIncludeProfilePictureUrl() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val authClient = createAuthenticatedClient(component)
        val pictureUrl = "https://example.com/avatar.png"
        component.database.userDao().insert(
            User(
                firebaseUid = "uid-avatar-user",
                username = "avatar-user",
                usernameLower = "avatar-user",
                pictureUrl = pictureUrl,
            )
        )

        val response = authClient.get("/api/players")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"uid\":\"uid-avatar-user\""))
        assertTrue(body.contains("\"username\":\"avatar-user\""))
        assertTrue(body.contains("\"pictureUrl\":\"$pictureUrl\""))
    }

    @Test
    fun authenticatedRequestsRefreshProfilePictureForBothEndpoints() = testApplication {
        val firebaseUid = "uid-test-user"
        val pictureUrl = "https://example.com/new-avatar.png"
        val tokenVerifier = FakeTokenVerifier().apply {
            setClaims(
                "test-token-$firebaseUid",
                FirebaseUserClaims(
                    uid = firebaseUid,
                    email = "test-user@example.com",
                    name = "test-user",
                    picture = pictureUrl,
                ),
            )
        }
        val component = TestServerComponent::class.create(TestFakes(tokenVerifier = tokenVerifier))
        application { module(component) }
        component.database.userDao().insert(
            User(
                firebaseUid = firebaseUid,
                username = "test-user",
                usernameLower = "test-user",
                pictureUrl = "https://example.com/old-avatar.png",
            )
        )
        val authClient = createAuthenticatedClient(component)

        val profileResponse = authClient.get("/api/me")
        assertEquals(HttpStatusCode.OK, profileResponse.status)
        assertTrue(profileResponse.bodyAsText().contains("\"pictureUrl\":\"$pictureUrl\""))

        val playersResponse = authClient.get("/api/players")
        assertEquals(HttpStatusCode.OK, playersResponse.status)
        assertTrue(playersResponse.bodyAsText().contains("\"pictureUrl\":\"$pictureUrl\""))
        assertEquals(pictureUrl, component.database.userDao().findByFirebaseUid(firebaseUid)?.pictureUrl)
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
        ensureTestUser(component.database, "alice")
        ensureTestUser(component.database, "bob")
        val alicePictureUrl = "https://example.com/alice.png"
        component.database.userDao().updatePictureUrl("uid-alice", alicePictureUrl)

        val response = authClient.post("/api/games") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"type":"contacts","players":[{"uid":"uid-alice"},{"uid":"uid-bob"}],"config":"{}"}"""
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(ContentType.Application.Json, response.contentType())
        assertTrue(response.bodyAsText().contains("\"type\":\"contacts\""))
        assertTrue(
            response.bodyAsText().contains(
                "\"uid\":\"uid-alice\",\"username\":\"alice\",\"pictureUrl\":\"$alicePictureUrl\""
            )
        )

        val response2 = authClient.get("/api/games")

        assertEquals(HttpStatusCode.OK, response2.status)
        val body = response2.bodyAsText()
        assertTrue(body.startsWith("["))
        assertTrue(body.contains("\"id\":"))
        assertTrue(body.contains("\"type\":\"contacts\""))
        assertTrue(
            body.contains("\"uid\":\"uid-alice\",\"username\":\"alice\",\"pictureUrl\":\"$alicePictureUrl\"")
        )
        assertTrue(body.contains("\"uid\":\"uid-bob\",\"username\":\"bob\""))
        assertTrue(body.contains("\"createdAt\":"))

        val gameId = body.substringAfter("\"id\":\"").substringBefore('"')
        val detailsResponse = authClient.get("/api/games/$gameId/contacts")
        assertEquals(HttpStatusCode.OK, detailsResponse.status)
        val detailsBody = detailsResponse.bodyAsText()
        assertTrue(detailsBody.contains("\"gameId\":\"$gameId\""))
        assertTrue(
            detailsBody.contains(
                "\"uid\":\"uid-alice\",\"username\":\"alice\",\"pictureUrl\":\"$alicePictureUrl\""
            )
        )
        assertTrue(detailsBody.contains("\"uid\":\"uid-bob\",\"username\":\"bob\""))
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

        val detailsResponse = authClient.get("/api/games/missing/contacts")
        assertEquals(HttpStatusCode.NotFound, detailsResponse.status)
        assertEquals("Game not found", detailsResponse.bodyAsText())

        val eventsResponse = authClient.get("/api/games/missing/contacts/events")
        assertEquals(HttpStatusCode.NotFound, eventsResponse.status)
        assertEquals("Game not found", eventsResponse.bodyAsText())

        val logsResponse = authClient.get("/api/games/missing/logs")
        assertEquals(HttpStatusCode.NotFound, logsResponse.status)
        assertEquals("Game not found", logsResponse.bodyAsText())
    }
}
