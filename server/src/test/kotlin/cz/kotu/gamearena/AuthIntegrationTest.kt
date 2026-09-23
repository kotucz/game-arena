package cz.kotu.gamearena

import cz.kotu.gamearena.model.RegisterUserRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
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
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun explicitUserRegistrationValidation() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }
        val client = createClient {}

        // Missing token
        val missingTokenResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterUserRequest.serializer(), RegisterUserRequest(username = "alice")))
        }
        assertEquals(HttpStatusCode.BadRequest, missingTokenResponse.status)

        // Missing username
        val missingUsernameResponse = client.post("/api/auth/register") {
            header(HttpHeaders.Authorization, "Bearer test-token:uid-alice")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterUserRequest.serializer(), RegisterUserRequest(username = "")))
        }
        assertEquals(HttpStatusCode.BadRequest, missingUsernameResponse.status)

        // Invalid token
        val invalidTokenResponse = client.post("/api/auth/register") {
            header(HttpHeaders.Authorization, "Bearer invalid-token-123")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterUserRequest.serializer(), RegisterUserRequest(username = "alice")))
        }
        assertEquals(HttpStatusCode.Unauthorized, invalidTokenResponse.status)

        // Valid registration
        val validRegistration = client.post("/api/auth/register") {
            header(HttpHeaders.Authorization, "Bearer test-token:uid-alice")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterUserRequest.serializer(), RegisterUserRequest(username = "alice", email = "alice@example.com")))
        }
        assertEquals(HttpStatusCode.Created, validRegistration.status)

        // Duplicate UID
        val duplicateUidResponse = client.post("/api/auth/register") {
            header(HttpHeaders.Authorization, "Bearer test-token:uid-alice")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterUserRequest.serializer(), RegisterUserRequest(username = "alice2")))
        }
        assertEquals(HttpStatusCode.Conflict, duplicateUidResponse.status)

        // Duplicate username with different UID
        val duplicateUsernameResponse = client.post("/api/auth/register") {
            header(HttpHeaders.Authorization, "Bearer test-token:uid-bob")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterUserRequest.serializer(), RegisterUserRequest(username = "alice")))
        }
        assertEquals(HttpStatusCode.Conflict, duplicateUsernameResponse.status)
    }

    @Test
    fun unprovisionedUserRejectedFromProtectedRoutes() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }
        val client = createClient {}

        // Valid token for unprovisioned user
        val meResponse = client.get("/api/me") {
            header(HttpHeaders.Authorization, "Bearer test-token:unprovisioned-uid")
        }
        assertEquals(HttpStatusCode.Unauthorized, meResponse.status)
    }

    @Test
    fun authenticatedBearerAllowsMeAndLogoutReturnsSuccess() = testApplication {
        val component = TestServerComponent::class.create()
        application { module(component) }

        val username = "testuser_${System.currentTimeMillis()}"
        val firebaseUid = "uid_$username"
        val user = User(username = username, email = "$username@example.com", firebaseUid = firebaseUid)
        component.database.userDao().insert(user)

        val client = createClient {}

        // Unauthenticated request to /api/me returns 401
        val unauthResponse = client.get("/api/me")
        assertEquals(HttpStatusCode.Unauthorized, unauthResponse.status)

        // Authenticated request to /api/me with valid Bearer token
        val meResponse: HttpResponse = client.get("/api/me") {
            header(HttpHeaders.Authorization, "Bearer test-token:$firebaseUid")
        }
        assertEquals(HttpStatusCode.OK, meResponse.status)
        assertEquals(username, meResponse.bodyAsText())

        // Logout returns 200 OK
        val logoutResponse: HttpResponse = client.post("/api/logout")
        assertEquals(HttpStatusCode.OK, logoutResponse.status)
        assertEquals("Logout successful", logoutResponse.bodyAsText())
    }
}
