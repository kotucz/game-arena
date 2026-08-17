package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

actual fun createPlatformAuthHttpClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Js) {
    configure()
}
