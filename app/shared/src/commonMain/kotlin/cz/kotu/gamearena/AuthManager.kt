package cz.kotu.gamearena

import cz.kotu.gamearena.model.LoggedUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject

/**
 * Authentication state visible to the rest of the app.
 *
 * - [Pending] — the one-time `/me` check is in flight.
 * - [Unauthorized] — no active session; user must log in.
 * - [Authorized] — session is valid and contains the current user's profile.
 */
sealed interface AuthState {
    data object Pending : AuthState
    data object Unauthorized : AuthState
    data class Authorized(val user: LoggedUser) : AuthState
}

/**
 * Central authentication state holder.
 *
 * ### Lazy `/me` fetch
 * The server is not contacted until [ensureLoaded] is called for the first time.
 * A [Mutex] guarantees at most one in-flight `/me` request even if multiple coroutines
 * call [ensureLoaded] concurrently.
 *
 * ### 401 handling
 * [unauthorizedEvents] (emitted by the Ktor 401 interceptor) is observed in [appScope];
 * each emission resets [authState] to [AuthState.Unauthorized] so that callers waiting in
 * [awaitLogin] will not return until the user actively logs in again.
 *
 * ### Decoupling from [io.ktor.client.HttpClient]
 * [unauthorizedEvents] is the same [MutableSharedFlow] instance provided by [AppComponent]
 * and also passed to the Ktor 401 interceptor — no circular dependency between
 * [AuthManager] and [HttpClient].
 */
@AppScope
@Inject
open class AuthManager(
    private val authClient: AuthClient,
    private val unauthorizedEvents: MutableSharedFlow<Unit>,
    private val appScope: CoroutineScope,
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthorized)

    /** Observable authentication state. */
    open val authState: StateFlow<AuthState> = _authState

    /**
     * Current profile from `/api/me`, or `null` when pending or unauthorized.
     */
    open val currentUser: StateFlow<LoggedUser?> = authState
        .map { (it as? AuthState.Authorized)?.user }
        .stateIn(appScope, SharingStarted.Eagerly, null)

    /** Convenience view for game and notification flows that only need a username. */
    open val currentUsername: StateFlow<String?> = currentUser
        .map { it?.username }
        .stateIn(appScope, SharingStarted.Eagerly, null)

    /** Emitted (via the shared flow) by the Ktor interceptor on HTTP 401. */
    open val unauthorizedEvent: SharedFlow<Unit> = unauthorizedEvents.asSharedFlow()

    init {
        // Reset auth state whenever the Ktor interceptor fires a 401.
        appScope.launch {
            unauthorizedEvents.collect {
                _authState.value = AuthState.Unauthorized
            }
        }
    }

    // ── Lazy /me fetch ──────────────────────────────────────────────────────

    private val loadMutex = Mutex()
    private var initialFetchDone = false

    /**
     * Triggers the initial `/me` check on the first call; subsequent calls are no-ops.
     * Safe to call from multiple coroutines concurrently — only one HTTP request is issued.
     */
    open suspend fun ensureLoaded() {
        if (initialFetchDone) return           // fast path, no lock needed
        loadMutex.withLock {
            if (initialFetchDone) return       // double-check inside the lock
            initialFetchDone = true
            _authState.value = AuthState.Pending
            val user = authClient.currentUser().getOrNull()
            _authState.value = if (user != null) {
                AuthState.Authorized(user)
            } else {
                AuthState.Unauthorized
            }
        }
    }

    /**
     * Suspends until [authState] is [AuthState.Authorized].
     *
     * Calls [ensureLoaded] first so the initial `/me` check is triggered if it has not
     * been done yet. After a 401 logout, [initialFetchDone] is already `true`, so this
     * just waits for the user to log in explicitly via the auth modal.
     */
    open suspend fun awaitLogin() {
        ensureLoaded()
        authState.first { it is AuthState.Authorized }
    }

    // ── Auth actions ────────────────────────────────────────────────────────

    open suspend fun checkExistingUser(): Result<LoggedUser> = authClient.currentUser().onSuccess { user ->
        _authState.value = AuthState.Authorized(user)
    }

    open suspend fun registerUser(
        idToken: String,
        username: String,
        email: String? = null,
    ): Result<LoggedUser> = authClient.registerUser(idToken, username, email).onSuccess { user ->
        _authState.value = AuthState.Authorized(user)
    }

    open suspend fun logout() {
        authClient.logout()
        _authState.value = AuthState.Unauthorized
        unauthorizedEvents.tryEmit(Unit)
    }
}
