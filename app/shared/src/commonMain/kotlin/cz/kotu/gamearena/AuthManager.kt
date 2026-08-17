package cz.kotu.gamearena

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

/**
 * Central authentication state holder.
 *
 * - [currentUsername] is null when unauthenticated and non-null once the user has logged in.
 * - [unauthorizedEvent] is emitted by the Ktor 401 interceptor whenever any API call returns
 *   HTTP 401. The root [App] composable observes this flow to show the auth modal.
 *
 * [unauthorizedEvent] is the same [MutableSharedFlow] instance provided by [AppComponent]
 * and also passed to the Ktor 401 interceptor — no circular dependency between
 * [AuthManager] and [HttpClient].
 */
@AppScope
@Inject
class AuthManager(
    private val authClient: AuthClient,
    unauthorizedEvents: MutableSharedFlow<Unit>,
) {
    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    /** Emitted (via the shared flow) by the Ktor interceptor on HTTP 401. */
    val unauthorizedEvent: SharedFlow<Unit> = unauthorizedEvents.asSharedFlow()

    suspend fun login(username: String, password: String): Result<String> =
        authClient.login(username, password).onSuccess {
            _currentUsername.value = username.trim()
        }

    suspend fun register(username: String, email: String, password: String): Result<String> =
        authClient.register(username, email, password).onSuccess {
            _currentUsername.value = username.trim()
        }

    fun onLoginSuccess(username: String) {
        _currentUsername.value = username
    }

    fun logout() {
        _currentUsername.value = null
    }
}
