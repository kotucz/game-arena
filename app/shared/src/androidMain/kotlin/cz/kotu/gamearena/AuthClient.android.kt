package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.sse.SSE

actual fun createAuthHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpCookies)
    install(SSE)
}

actual fun authBaseUrl(): String = "http://10.0.2.2:8080"
