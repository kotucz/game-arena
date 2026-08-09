package cz.kotu.gamearena

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createAuthHttpClient(): HttpClient = HttpClient(CIO)

actual fun authBaseUrl(): String = System.getenv("GAMEARENA_API_URL") ?: "http://localhost:8080"
