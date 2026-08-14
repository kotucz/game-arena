package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.sse.SSE

actual fun createAuthHttpClient(): HttpClient = HttpClient(Js) { install(SSE) }
