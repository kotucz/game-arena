package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun createAuthHttpClient(onUnauthorized: () -> Unit): HttpClient = HttpClient(Js) {
    commonHttpClientConfig(onUnauthorized)
}
