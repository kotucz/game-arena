# Authentication Refactor — Implementation Plan

## Implementation status

Implemented in the working tree. The shared JVM target compiles successfully with
`gradlew.bat :app:shared:compileKotlinJvm`.

The implementation uses `HttpResponseValidator` for Ktor 3.x response
inspection. `AuthManager` owns the auth operations and delegates transport to
`AuthClient`; this keeps the UI independent of the transport while avoiding a
dependency cycle.

## Summary of agreed design

| Decision | Choice |
|---|---|
| AuthManager scope | `@AppScope` singleton in `AppComponent` via kotlin-inject |
| AuthManager state | `currentUsername: StateFlow<String?>` — null = unauthenticated |
| Startup session restore | Removed — no proactive check; auth triggered lazily by 401 |
| 401 interception | Ktor plugin on the shared `HttpClient` |
| Signal to UI | `SharedFlow<Unit> unauthorizedEvent` on `AuthManager` |
| Auth modal | Root `App` observes `unauthorizedEvent`, presents `AuthScreen` as overlay; no dedicated route |
| Post-login | Modal dismisses, original screen retries naturally |
| Username in VMs | `AuthManager` injected into VMs; `ContactsGameViewModel` factory drops `username: String` param |

---

## Step 1 — Create `AuthManager`

**File:** [NEW] `app/shared/.../gamearena/AuthManager.kt`

```kotlin
@AppScope
@Inject
class AuthManager(private val authClient: AuthClient) {
    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    // Emitted by the Ktor 401 interceptor; observed by root App to show auth modal
    val unauthorizedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    suspend fun login(username: String, password: String): Result<String> =
        authClient.login(username, password).onSuccess { _currentUsername.value = username }

    suspend fun register(username: String, email: String, password: String): Result<String> =
        authClient.register(username, email, password).onSuccess { _currentUsername.value = username }

    fun notifyUnauthorized() { unauthorizedEvent.tryEmit(Unit) }

    fun logout() { _currentUsername.value = null }
}
```

Expose from `AppComponent`:
```kotlin
abstract val authManager: AuthManager
```

> [!NOTE]
> `AppScope` ensures a single instance across the app. The kotlin-inject compiler will wire it automatically through `AppComponent`.

---

## Step 2 — Ktor 401 interceptor

**File:** [MODIFY] `app/shared/.../gamearena/AuthClient.kt` (or a new `HttpClientFactory.kt`)

Add a Ktor `ResponseInterceptor` (or `HttpSend` plugin) to the shared `HttpClient`. Because `AuthManager` must be available when the client is constructed, pass it into `createAuthHttpClient()`:

```kotlin
// Before: expect fun createAuthHttpClient(): HttpClient
// After:
expect fun createAuthHttpClient(onUnauthorized: () -> Unit): HttpClient
```

Each platform's `actual` implementation installs the plugin:
```kotlin
install(HttpSend) {
    intercept { request ->
        val call = execute(request)
        if (call.response.status == HttpStatusCode.Unauthorized) {
            onUnauthorized()
        }
        call
    }
}
```

`AppComponent` wires this:
```kotlin
@Provides
@AppScope
fun provideHttpClient(authManager: AuthManager): HttpClient =
    createAuthHttpClient(onUnauthorized = authManager::notifyUnauthorized)
```

> [!WARNING]
> `authManager` and `httpClient` now have a dependency order: `AuthManager` must be created before `HttpClient`. kotlin-inject handles this via the constructor graph, but ensure no circular dependency exists.

---

## Step 3 — Auth modal in `App.kt`

**File:** [MODIFY] `app/shared/.../gamearena/App.kt`

- Remove `username`, `authenticationChecked` mutableState fields.
- Remove the `LaunchedEffect(Unit)` session-restore block.
- Add a `showAuthModal` boolean state, driven by `authManager.unauthorizedEvent`.
- Present `AuthScreen` as a modal overlay (e.g. `Dialog` or `BottomSheet`) when `showAuthModal` is true.

```kotlin
@Composable
fun App() {
    MaterialTheme {
        val appComponent = remember { AppComponent::class.create() }
        val authManager = appComponent.authManager
        val navController = rememberNavController()

        var showAuthModal by remember { mutableStateOf(false) }

        // Show auth modal whenever any API call returns 401
        LaunchedEffect(authManager) {
            authManager.unauthorizedEvent.collect { showAuthModal = true }
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NavHost(navController, startDestination = GAMES_ROUTE) {
                composable(GAMES_ROUTE) {
                    GamesScreen(
                        gamesClient = appComponent.gamesClient,
                        onStartGotFive = { navController.navigate(GOT_FIVE_ROUTE) },
                        onGameClick = { game -> navController.navigate("game/${game.id}") },
                    )
                }
                composable(GOT_FIVE_ROUTE) {
                    GotFiveScreen(appComponent = appComponent, onBack = { navController.popBackStack() })
                }
                composable(
                    route = CONTACTS_GAME_ROUTE,
                    arguments = listOf(navArgument(CONTACTS_GAME_ID_ARGUMENT) { type = NavType.StringType })
                ) { entry ->
                    val gameId = entry.arguments?.read { getString(CONTACTS_GAME_ID_ARGUMENT) }
                    if (gameId != null) {
                        ContactsGameScreen(appComponent, gameId, onBack = { navController.popBackStack() })
                    }
                }
            }
            BrowserNavigationEffect(navController)
        }

        // Auth modal — shown on 401 or could also be triggered for explicit logout/login flows
        if (showAuthModal) {
            Dialog(onDismissRequest = {}) { // non-dismissable until authenticated
                AuthScreen(
                    authManager = authManager,
                    onAuthenticated = { showAuthModal = false },
                )
            }
        }
    }
}
```

> [!IMPORTANT]
> `startDestination = GAMES_ROUTE` — the app no longer starts on `AUTH_ROUTE`. Routes are always accessible; auth is enforced lazily via 401. If the user is unauthenticated and navigates to a screen that calls an API, the 401 interceptor will emit `unauthorizedEvent`, showing the modal.

---

## Step 4 — Refactor `AuthScreen`

**File:** [MODIFY] `app/shared/.../gamearena/AuthScreen.kt`

- Replace `authClient: AuthClient` parameter with `authManager: AuthManager`.
- Call `authManager.login(...)` / `authManager.register(...)` instead of calling `authClient` directly.
- Remove `onAuthenticated: (String) -> Unit` (username no longer passed out); keep `onAuthenticated: () -> Unit`.

```kotlin
@Composable
fun AuthScreen(
    authManager: AuthManager,
    onAuthenticated: () -> Unit,
)
```

---

## Step 5 — Remove `username` from `GamesScreen`

**File:** [MODIFY] `app/shared/.../gamearena/GamesScreen.kt`

- Remove `username: String` parameter.
- Inject `AuthManager` and collect `authManager.currentUsername.collectAsState()` locally.
- Use `username ?: ""` as a sensible default for the players pre-fill text field.

```kotlin
@Composable
fun GamesScreen(
    gamesClient: GamesClient,
    authManager: AuthManager,
    onStartGotFive: () -> Unit,
    onGameClick: (RunningGame) -> Unit,
)
```

---

## Step 6 — Refactor `ContactsGameViewModel` factory

**File:** [MODIFY] `core/.../contacts/ContactsGameViewModel.kt` and `AppComponent.kt`

- Inject `AuthManager` into `ContactsGameViewModel` (or into `NetworkContactsGameFacade`).
- Remove `username: String` from the factory lambda.

```kotlin
// Before:
abstract val contactsGameViewModelFactory: (String, String) -> ContactsGameViewModel  // (gameId, username)
// After:
abstract val contactsGameViewModelFactory: (String) -> ContactsGameViewModel           // (gameId)
```

The ViewModel reads `authManager.currentUsername.value` when it needs the username (e.g., when identifying the local player).

---

## Step 7 — Remove `AUTH_ROUTE` and related dead code

**File:** [MODIFY] `App.kt`

- Delete `AUTH_ROUTE` constant.
- Remove the old `composable(AUTH_ROUTE)` block.
- Remove leftover `LaunchedEffect` navigation hacks.

---

## Verification Plan

### Automated tests
```
gradlew.bat :core:jvmTest
gradlew.bat :app:shared:compileKotlinJvm
gradlew.bat :server:test
```

### Manual scenarios
1. **Fresh start (not logged in):** Open GamesScreen → API returns 401 → auth modal appears → log in → modal closes → GamesScreen loads games.
2. **Deep link to game while not logged in:** Navigate to `game/abc123` → API returns 401 → auth modal → log in → game loads.
3. **Session still valid (cookie persists):** Reopen app → navigate directly to GamesScreen → games load without modal.
4. **Back navigation:** Open game → auth modal → log in → game loads → press Back → returns to GamesScreen.
5. **No duplicate AppComponent creation:** Add a log in `AppComponent` constructor; verify it fires once per app lifecycle.

---
*Ready to implement — approve to begin.*
Implementation is complete. The manual scenarios above still require an
authenticated server/session to verify interactively.
