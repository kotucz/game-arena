package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cookies.HttpCookies

actual fun createAuthHttpClient(): HttpClient = HttpClient(CIO) {
    install(HttpCookies) {
        storage = PreferencesCookieStorage()
    }
}

actual fun authBaseUrl(): String = System.getenv("GAMEARENA_API_URL") ?: "http://localhost:8080"
