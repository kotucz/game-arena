package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cookies.HttpCookies

actual fun createPlatformAuthHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(OkHttp) {
    install(HttpCookies)
    configure()
}

actual fun authBaseUrl(): String = "http://10.0.2.2:8080"
