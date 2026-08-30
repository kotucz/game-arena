package cz.kotu.gamearena

/**
 * Thrown by the Ktor HTTP response validator when any API call returns HTTP 401.
 *
 * This is an internal bridge type: it is thrown synchronously inside [commonHttpClientConfig]'s
 * `validateResponse` block so that coroutine callers (e.g. [GamesClient.observeGames]) can
 * detect an auth failure via `catch`/`retryWhen` without a timing race.
 *
 * The validator also calls [onUnauthorized] (which emits to [AuthManager.unauthorizedEvent])
 * to show the login modal, so both signals remain in sync.
 */
internal class UnauthorizedException : Exception("HTTP 401 Unauthorized")
