package cz.kotu.gamearena

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.client.plugins.cookies.CookiesStorage
import java.util.prefs.Preferences

/** Persists the Game Arena session cookie between desktop app launches. */
internal class PreferencesCookieStorage : CookiesStorage {
    private val preferences = Preferences.userNodeForPackage(PreferencesCookieStorage::class.java)

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val value = preferences.get(COOKIE_VALUE_KEY, null) ?: return emptyList()
        val expiresAt = preferences.getLong(COOKIE_EXPIRES_AT_KEY, 0L)
        if (expiresAt != 0L && expiresAt <= System.currentTimeMillis()) {
            clear()
            return emptyList()
        }
        return listOf(Cookie(name = COOKIE_NAME, value = value, path = "/"))
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name != COOKIE_NAME || cookie.value.isEmpty() || cookie.maxAge == 0) {
            clear()
            return
        }

        preferences.put(COOKIE_VALUE_KEY, cookie.value)
        val expiresAt = cookie.maxAge?.let {
            System.currentTimeMillis() + it.toLong() * 1_000L
        } ?: 0L
        preferences.putLong(COOKIE_EXPIRES_AT_KEY, expiresAt)
        preferences.flush()
    }

    override fun close() = Unit

    private fun clear() {
        preferences.remove(COOKIE_VALUE_KEY)
        preferences.remove(COOKIE_EXPIRES_AT_KEY)
        preferences.flush()
    }

    private companion object {
        const val COOKIE_VALUE_KEY = "session_cookie"
        const val COOKIE_EXPIRES_AT_KEY = "session_cookie_expires_at"
        const val COOKIE_NAME = "gamearena_session"
    }
}
