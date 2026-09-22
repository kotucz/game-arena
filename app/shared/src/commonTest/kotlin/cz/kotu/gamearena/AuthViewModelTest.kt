package cz.kotu.gamearena

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAuthManager(
        var loginResult: Result<String> = Result.success("OK"),
        var registerResult: Result<String> = Result.success("OK"),
    ) : AuthManager(
        authClient = AuthClient(createAuthHttpClient("http://localhost") {}),
        unauthorizedEvents = MutableSharedFlow(extraBufferCapacity = 1),
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    ) {
        var lastLoginUsername: String? = null
        var lastLoginPassword: String? = null
        var lastRegisterUsername: String? = null
        var lastRegisterEmail: String? = null
        var lastRegisterPassword: String? = null

        override suspend fun login(username: String, password: String): Result<String> {
            lastLoginUsername = username
            lastLoginPassword = password
            return loginResult
        }

        override suspend fun register(username: String, email: String, password: String): Result<String> {
            lastRegisterUsername = username
            lastRegisterEmail = email
            lastRegisterPassword = password
            return registerResult
        }
    }

    @Test
    fun initialValuesAreDefault() {
        val authManager = FakeAuthManager()
        val viewModel = AuthViewModel(authManager)

        assertEquals(AuthMode.Login, viewModel.mode.value)
        assertEquals("", viewModel.username.value)
        assertEquals("", viewModel.email.value)
        assertEquals("", viewModel.password.value)
        assertNull(viewModel.message.value)
        assertFalse(viewModel.submitting.value)
    }

    @Test
    fun updateFieldsModifiesState() {
        val authManager = FakeAuthManager()
        val viewModel = AuthViewModel(authManager)

        viewModel.updateUsername("user1")
        viewModel.updateEmail("user1@example.com")
        viewModel.updatePassword("secret123")

        assertEquals("user1", viewModel.username.value)
        assertEquals("user1@example.com", viewModel.email.value)
        assertEquals("secret123", viewModel.password.value)
    }

    @Test
    fun toggleModeSwitchesModeAndClearsMessage() {
        val authManager = FakeAuthManager(loginResult = Result.failure(Exception("Error")))
        val viewModel = AuthViewModel(authManager)

        viewModel.submit {}
        assertEquals("Error", viewModel.message.value)

        viewModel.toggleMode()
        assertEquals(AuthMode.Register, viewModel.mode.value)
        assertNull(viewModel.message.value)

        viewModel.toggleMode()
        assertEquals(AuthMode.Login, viewModel.mode.value)
        assertNull(viewModel.message.value)
    }

    @Test
    fun submitLoginSuccess() = runTest {
        val authManager = FakeAuthManager(loginResult = Result.success("Welcome"))
        val viewModel = AuthViewModel(authManager)
        viewModel.updateUsername("alice")
        viewModel.updatePassword("pwd")

        var authenticated = false
        viewModel.submit { authenticated = true }

        assertEquals("alice", authManager.lastLoginUsername)
        assertEquals("pwd", authManager.lastLoginPassword)
        assertTrue(authenticated)
        assertNull(viewModel.message.value)
        assertFalse(viewModel.submitting.value)
    }

    @Test
    fun submitLoginFailure() = runTest {
        val authManager = FakeAuthManager(loginResult = Result.failure(Exception("Invalid credentials")))
        val viewModel = AuthViewModel(authManager)
        viewModel.updateUsername("alice")
        viewModel.updatePassword("wrong")

        var authenticated = false
        viewModel.submit { authenticated = true }

        assertFalse(authenticated)
        assertEquals("Invalid credentials", viewModel.message.value)
        assertFalse(viewModel.submitting.value)
    }

    @Test
    fun submitRegisterSuccess() = runTest {
        val authManager = FakeAuthManager(registerResult = Result.success("Registered"))
        val viewModel = AuthViewModel(authManager)
        viewModel.updateMode(AuthMode.Register)
        viewModel.updateUsername("bob")
        viewModel.updateEmail("bob@example.com")
        viewModel.updatePassword("pwd123")

        var authenticated = false
        viewModel.submit { authenticated = true }

        assertEquals("bob", authManager.lastRegisterUsername)
        assertEquals("bob@example.com", authManager.lastRegisterEmail)
        assertEquals("pwd123", authManager.lastRegisterPassword)
        assertTrue(authenticated)
        assertNull(viewModel.message.value)
        assertFalse(viewModel.submitting.value)
    }

    @Test
    fun submitRegisterFailure() = runTest {
        val authManager = FakeAuthManager(registerResult = Result.failure(Exception("Email taken")))
        val viewModel = AuthViewModel(authManager)
        viewModel.updateMode(AuthMode.Register)
        viewModel.updateUsername("bob")
        viewModel.updateEmail("bob@example.com")
        viewModel.updatePassword("pwd123")

        var authenticated = false
        viewModel.submit { authenticated = true }

        assertFalse(authenticated)
        assertEquals("Email taken", viewModel.message.value)
        assertFalse(viewModel.submitting.value)
    }
}
