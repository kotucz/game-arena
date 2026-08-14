package cz.kotu.gamearena

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cz.kotu.tools.DebugHttpClientFactory
import cz.kotu.tools.MultiPlayerScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Game Arena - Multiplayer Debug",
    ) {
        val debugHttpClientFactory: DebugHttpClientFactory = ::createDebugAuthHttpClient
        MultiPlayerScreen(debugHttpClientFactory)
    }
}
