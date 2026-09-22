package cz.kotu.gamearena

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

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
        var firebaseResult: Result<String> = Result.success("OK"),
    ) : AuthManager(
        authClient = AuthClient(createAuthHttpClient("http://localhost") {}),
        unauthorizedEvents = MutableSharedFlow(extraBufferCapacity = 1),
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    ) {
        var lastIdToken: String? = null
        var lastUsername: String? = null
        var lastEmail: String? = null

        override suspend fun loginWithFirebase(
            idToken: String,
            username: String?,
            email: String?,
        ): Result<String> {
            lastIdToken = idToken
            lastUsername = username
            lastEmail = email
            return firebaseResult
        }
    }

    @Test
    fun initialValuesAreDefault() {
        val authManager = FakeAuthManager()
        val viewModel = AuthViewModel(authManager)

        assertEquals(AuthMode.Login, viewModel.mode.value)
        assertEquals("", viewModel.email.value)
        assertEquals("", viewModel.password.value)
        assertNull(viewModel.message.value)
        assertFalse(viewModel.submitting.value)
    }

    @Test
    fun updateEmailUpdatesState() {
        val authManager = FakeAuthManager()
        val viewModel = AuthViewModel(authManager)

        viewModel.updateEmail("user@example.com")
        assertEquals("user@example.com", viewModel.email.value)
    }

    @Test
    fun updatePasswordUpdatesState() {
        val authManager = FakeAuthManager()
        val viewModel = AuthViewModel(authManager)

        viewModel.updatePassword("secret123")
        assertEquals("secret123", viewModel.password.value)
    }

    @Test
    fun toggleModeSwitchesModeAndClearsMessage() {
        val authManager = FakeAuthManager()
        val viewModel = AuthViewModel(authManager)

        viewModel.handleAuthResult(Result.failure(Exception("Initial failure"))) {}
        assertEquals("Initial failure", viewModel.message.value)

        viewModel.toggleMode()
        assertEquals(AuthMode.Register, viewModel.mode.value)
        assertNull(viewModel.message.value)

        viewModel.toggleMode()
        assertEquals(AuthMode.Login, viewModel.mode.value)
        assertNull(viewModel.message.value)
    }

    @Test
    fun handleAuthResultFailureSetsMessage() {
        val authManager = FakeAuthManager()
        val viewModel = AuthViewModel(authManager)

        viewModel.handleAuthResult(Result.failure(Exception("Google Sign-In failed"))) {}

        assertEquals("Google Sign-In failed", viewModel.message.value)
    }

    @Test
    fun clearMessageResetsMessage() {
        val authManager = FakeAuthManager()
        val viewModel = AuthViewModel(authManager)

        viewModel.handleAuthResult(Result.failure(Exception("Google Sign-In failed"))) {}
        assertEquals("Google Sign-In failed", viewModel.message.value)

        viewModel.clearMessage()
        assertNull(viewModel.message.value)
    }
}
