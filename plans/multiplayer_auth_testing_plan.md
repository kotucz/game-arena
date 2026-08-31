# Plan: Multiplayer Testing with Authenticated Player Sessions

## Overview
With `X-Debug-Username` header authentication removed from the backend, multi-player testing in `multiplayer.kt` requires authenticating as real users with session cookies.

Because `multiplayer.kt` renders multiple players side-by-side (e.g. `kotucz`, `llama`), each player must operate in an isolated HTTP / cookie context so that their session cookies do not overwrite each other.

---

## Key Requirements & Architecture

1. **Isolated Cookie Storage**:
   - The default desktop HTTP client uses `PreferencesCookieStorage` (persisting to global user preferences).
   - For multi-player debug testing, each player must use an isolated in-memory cookie storage (`AcceptAllCookiesStorage()`) so that cookies are scoped per player.

2. **Player Credentials & Automated Login/Registration**:
   - Provide a configured map or list of test player credentials (e.g. `username` -> `password`).
   - On screen load (or during player initialization), perform an authentication handshake (`/api/login` or `/api/register` if not found) on that player's isolated HTTP client / `AuthManager`.

3. **Per-Player Component / ViewModel Setup**:
   - Option A: Each player column instantiates its own `AppComponent` providing an isolated in-memory `HttpClient`.
   - Option B: `MultiPlayerViewModel` manages a map of isolated `HttpClient` / `AuthClient` instances keyed by username, ensuring they log in before creating `NetworkContactsGameFacade`.
   - **Recommended Approach**: Clean abstraction with an isolated client per player. If `ContactsPlayerScreen` is embedded per player, each player column can either:
     - Wrap an independent `AppComponent` (giving full parity with the real app architecture), OR
     - Pass an authenticated `ContactsGameFacade` / `ContactsPlayerViewModel` tied to the player's authenticated `HttpClient`.

---

## Detailed Implementation Steps

### 1. In-Memory Client Factory
Add a helper in `app/shared` (e.g., in `AuthClient.jvm.kt` or `HttpClientConfig.kt` / test tools) to create an in-memory authenticated HTTP client:
```kotlin
fun createInMemoryAuthHttpClient(onUnauthorized: () -> Unit = {}): HttpClient =
    HttpClient(OkHttp) {
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        engine {
            preconfigured = okhttp3.OkHttpClient.Builder()
                .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
                .build()
        }
        commonHttpClientConfig(onUnauthorized)
    }
```

### 2. Player Credential Configuration & Auto-Login Helper
In `cz.kotu.tools.MultiPlayerViewModel` (or `multiplayer.kt`):
- Define player credentials:
  ```kotlin
  data class PlayerCredentials(val username: String, val password: String = "password123")
  ```
- Before creating `NetworkContactsGameFacade` for remote play, authenticate the player's client via `authClient.login(username, password)`. If login fails with 401 / user not found, optionally attempt `authClient.register(username, "$username@example.com", password)`.

### 3. Update `MultiPlayerViewModel` & `MultiPlayerScreen`
- Update `MultiPlayerViewModel` to manage the authentication state for each player.
- Ensure `gameFacadeForPlayer(username)` uses the logged-in client with its active session cookie.
- Remove obsolete references to `DEBUG_USERNAME_HEADER` and `createDebugAuthHttpClient(username)`.

### 4. Update `multiplayer.kt`
- Clean up the entry point in `app/desktopApp/src/main/kotlin/cz/kotu/gamearena/multiplayer.kt`.
- Remove legacy debug client factory references.

---

## Verification Plan
1. Start the server backend (`.\gradlew.bat :server:run`).
2. Run `multiplayer.kt` desktop application.
3. Verify that players log in independently and game actions / SSE events work correctly across all player columns without authentication conflicts.
